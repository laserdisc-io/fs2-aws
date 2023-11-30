package fs2.aws.s3

import cats.effect.*
import cats.implicits.*
import cats.{Applicative, ApplicativeThrow, ~>}
import eu.timepit.refined.auto.*
import fs2.aws.s3.S3.MultipartETagValidation
import fs2.aws.s3.models.Models.*
import fs2.{Chunk, Pipe, Pull}
import io.laserdisc.pure.s3.tagless.S3AsyncClientOp
import software.amazon.awssdk.core.async.{AsyncRequestBody, AsyncResponseTransformer}
import software.amazon.awssdk.services.s3.model.*

import java.nio.ByteBuffer
import java.security.MessageDigest
import scala.collection.immutable.ArraySeq
import scala.collection.immutable.ArraySeq.unsafeWrapArray
import scala.jdk.CollectionConverters.*
import scala.util.control.NoStackTrace

/* A purely functional abstraction over the S3 API based on fs2.Stream */
trait S3[F[_]] {
  def delete(bucket: BucketName, key: FileKey): F[Unit]
  def uploadFile(
      bucket: BucketName,
      key: FileKey,
      awsRequestModifier: AwsRequestModifier.Upload1 = AwsRequestModifier.Upload1.identity
  ): Pipe[F, Byte, ETag]

  def uploadFileMultipart(
      bucket: BucketName,
      key: FileKey,
      partSize: PartSizeMB,
      uploadEmptyFiles: UploadEmptyFiles = false,
      multiPartConcurrency: MultiPartConcurrency = 10,
      requestModifier: AwsRequestModifier.MultipartUpload = AwsRequestModifier.MultipartUpload.identity,
      eTagValidation: Option[MultipartETagValidation[F]] = None
  ): Pipe[F, Byte, Option[ETag]]
  def readFile(bucket: BucketName, key: FileKey): fs2.Stream[F, Byte]

  def readFileMultipart(
      bucket: BucketName,
      key: FileKey,
      partSize: PartSizeMB
  ): fs2.Stream[F, Byte]
}

object S3 {
  private type PartETag   = String
  private type PartId     = Int
  private type UploadId   = String
  private type PartNumber = Long
  private type PartDigest = Array[Byte]
  private type Checksum   = String

  private case class PartProcessingOutcome(tag: PartETag, id: PartId, digest: PartDigest)

  case object NoEtagReturnedByS3 extends RuntimeException("No etag returned by s3")

  trait MultipartETagValidation[F[_]] {
    def validateETag(eTag: ETag, maxPartNumber: PartNumber, checksum: Checksum): F[Unit]
  }
  object MultipartETagValidation {
    final case class InvalidChecksum(s3ETag: ETag, expectedTag: ETag)
        extends RuntimeException(show"Invalid checksum. found $s3ETag, expected $expectedTag")
        with NoStackTrace

    def create[F[_]: ApplicativeThrow]: MultipartETagValidation[F] = new MultipartETagValidation[F] {
      def validateETag(eTag: ETag, maxPartNumber: PartNumber, checksum: Checksum): F[Unit] = {
        import cats.syntax.eq.*

        val eTagWithoutQuotes = eTag.replace("\"", "") // ETag from uploadFileMultipart has double quotes around it

        val expectedEtag = show"$checksum-$maxPartNumber"
        ApplicativeThrow[F].raiseWhen(expectedEtag =!= eTagWithoutQuotes)(
          InvalidChecksum(eTagWithoutQuotes, expectedEtag)
        )
      }
    }

    /** Useful for testing */
    def noOp[F[_]: Applicative]: MultipartETagValidation[F] = new MultipartETagValidation[F] {
      override def validateETag(
          eTag: ETag,
          maxPartNumber: PartNumber,
          checksum: Checksum
      ): F[Unit] = Applicative[F].unit
    }
  }

  /** It creates an instance of the purely functional S3 API. */
  def create[F[_]: Async](s3: S3AsyncClientOp[F]): S3[F] =
    new S3[F] {

      /** Deletes a file in a single request.
        */
      def delete(bucket: BucketName, key: FileKey): F[Unit] =
        s3.deleteObject(
          DeleteObjectRequest
            .builder()
            .bucket(bucket.value)
            .key(key.value)
            .build()
        ).void

      /** Uploads a file in a single request. Suitable for small files.
        *
        * For big files, consider using [[uploadFileMultipart]] instead.
        */
      def uploadFile(bucket: BucketName, key: FileKey, modifier: AwsRequestModifier.Upload1): Pipe[F, Byte, ETag] =
        in =>
          fs2.Stream.eval {
            in.compile.toVector.flatMap { vs =>
              val bs = ByteBuffer.wrap(vs.toArray)
              s3.putObject(
                modifier.putObject(PutObjectRequest.builder().bucket(bucket.value).key(key.value)).build(),
                AsyncRequestBody.fromByteBuffer(bs)
              ).map(_.eTag())
            }
          }

      /** Uploads a file in multiple parts of the specified @partSize per request. Suitable for big files.
        *
        * It does so in constant memory. So at a given time, only the number of bytes indicated by @partSize
        * will be loaded in memory.
        *
        * Note: AWS S3 API does not support uploading empty files via multipart upload. It does not gracefully
        * respond on attempting to do this and returns a `400` response with a generic error message. This function
        * accepts a boolean `uploadEmptyFile` (set to `false` by default) to determine how to handle this scenario. If
        * set to false (default) and no data has passed through the stream, it will gracefully abort the multi-part
        * upload request. If set to true, and no data has passed through the stream, an empty file will be uploaded on
        * completion. An `Option[ETag]` of `None` will be emitted on the stream if no file was uploaded, else a
        * `Some(ETag)` will be emitted. Alternatively, If you need to create empty files, consider using consider using
        * [[uploadFile]] instead.
        *
        * For small files, consider using [[uploadFile]] instead.
        *
        * @param bucket the bucket name
        * @param key the target file key
        * @param partSize the part size indicated in MBs. It must be at least 5, as required by AWS.
        * @param uploadEmptyFiles whether to upload empty files or not, if no data has passed through the stream create an empty file default is false
        * @param multiPartConcurrency the number of concurrent parts to upload
        */
      def uploadFileMultipart(
          bucket: BucketName,
          key: FileKey,
          partSize: PartSizeMB,
          uploadEmptyFiles: UploadEmptyFiles,
          multiPartConcurrency: MultiPartConcurrency
      ): Pipe[F, Byte, Option[ETag]] = {
        val chunkSizeBytes = partSize * 1048576

        def initiateMultipartUpload: F[UploadId] =
          s3.createMultipartUpload(
            requestModifier
              .createMultipartUpload(CreateMultipartUploadRequest.builder().bucket(bucket.value).key(key.value))
              .build()
          ).map(_.uploadId())

        def uploadPart(uploadId: UploadId): Pipe[F, (Chunk[Byte], PartNumber), PartProcessingOutcome] =
          _.parEvalMap(multiPartConcurrency) { case (c, i) =>
            s3.uploadPart(
              requestModifier
                .uploadPart(
                  UploadPartRequest
                    .builder()
                    .bucket(bucket.value)
                    .key(key.value)
                    .uploadId(uploadId)
                    .partNumber(i.toInt)
                    .contentLength(c.size.toLong)
                )
                .build(),
              AsyncRequestBody.fromBytes(c.toArray)
            ).map(resp => PartProcessingOutcome(resp.eTag(), i.toInt, checksumPart(c)))
          }

        def uploadEmptyFile = s3.putObject(
          PutObjectRequest.builder().bucket(bucket.value).key(key.value).build(),
          AsyncRequestBody.fromBytes(new Array[Byte](0))
        )
        def completeUpload(
            uploadId: UploadId
        ): Pipe[F, List[PartProcessingOutcome], (Option[ETag], Option[Checksum], Option[PartId])] =
          _.evalMap[F, (Option[ETag], Option[Checksum], Option[PartId])] {
            case Nil =>
              cancelUpload(uploadId) *>
                Async[F]
                  .ifM(Async[F].pure(uploadEmptyFiles))(
                    uploadEmptyFile.map(eTag => (Option(eTag.eTag()), Option.empty[Checksum], Option.empty[PartId])),
                    Async[F].pure((Option.empty[ETag], Option.empty[Checksum], Option.empty[PartId]))
                  )
            case tags =>
              val parts = tags.map { case PartProcessingOutcome(t, i, _) =>
                CompletedPart.builder().partNumber(i).eTag(t).build()
              }.asJava
              val maxPartId = getMaxPartId(tags)
              for {
                resp <- s3.completeMultipartUpload(
                  requestModifier
                    .completeMultipartUpload(
                      requestModifier
                  .completeMultipartUpload(CompleteMultipartUploadRequest
                        .builder()
                        .bucket(bucket.value)
                        .key(key.value)
                        .uploadId(uploadId)
                        .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                    )
                    .build()
                )
                overallCheckSum <- getOverallChecksum(tags)
              } yield (Option(resp.eTag()), Some(overallCheckSum), maxPartId)

          }

        def cancelUpload(uploadId: UploadId): F[Unit] =
          s3.abortMultipartUpload(
            requestModifier
              .abortMultipartUpload(
                AbortMultipartUploadRequest
                  .builder()
                  .bucket(bucket.value)
                  .key(key.value)
                  .uploadId(uploadId)
              )
              .build()
          ).void

        def createMD5: MessageDigest = MessageDigest.getInstance("MD5")

        def checksumPart(chunk: Chunk[Byte]): PartDigest = {
          val md = createMD5
          md.update(chunk.toArray)
          md.digest()
        }

        def formatChecksum(checksum: Array[Byte]): F[Checksum] = ApplicativeThrow[F].catchNonFatal {
          import scala.collection.mutable
          val sb = new mutable.StringBuilder
          for (b <- checksum)
            sb.append(String.format("%02x", b))
          sb.toString()
        }
        def getOverallChecksum(tags: List[PartProcessingOutcome]): F[Checksum] = {
          val md = createMD5
          tags.sortBy(_.id).foreach { case PartProcessingOutcome(_, _, partDigest) => md.update(partDigest) }
          formatChecksum(md.digest())
        }
        def getMaxPartId(tags: List[PartProcessingOutcome]): Option[PartId] =
          tags.maxByOption(_.id).map(_.id)

        def validateETag(et: Option[ETag], maxPartId: Option[PartId], cs: Option[Checksum]): F[Unit] =
          (multipartETagValidation, maxPartId, cs) match {
            case (Some(validator), Some(partId), Some(checksum)) =>
              for {
                eTag   <- ApplicativeThrow[F].fromOption(et, NoEtagReturnedByS3)
                result <- validator.validateETag(eTag, partId.toLong, checksum)
              } yield result
            case _ => Applicative[F].unit
          }

        in =>
          for {
            uploadId <- fs2.Stream.eval(initiateMultipartUpload)
            (eTag, checksum, maxPartNum) <- in
              .chunkN(chunkSizeBytes)
              .zip(fs2.Stream.iterate(1L)(_ + 1))
              .through(uploadPart(uploadId))
              .fold[List[PartProcessingOutcome]](List.empty)(_ :+ _)
              .through(completeUpload(uploadId))
              .handleErrorWith(ex => fs2.Stream.eval(cancelUpload(uploadId) >> Sync[F].raiseError(ex)))
            _ <- fs2.Stream.eval(validateETag(eTag, maxPartNum, checksum))
          } yield eTag

      }

      /** Reads a file in a single request. Suitable for small files.
        *
        * For big files, consider using [[readFileMultipart]] instead.
        */
      def readFile(bucket: BucketName, key: FileKey): fs2.Stream[F, Byte] =
        fs2.Stream
          .eval(
            s3.getObject(
              GetObjectRequest
                .builder()
                .bucket(bucket.value)
                .key(key.value)
                .build(),
              AsyncResponseTransformer.toBytes[GetObjectResponse]
            )
          )
          .flatMap(r => fs2.Stream.chunk(Chunk(ArraySeq.unsafeWrapArray(r.asByteArray)*)))

      /** Reads a file in multiple parts of the specified @partSize per request. Suitable for big files.
        *
        * It does so in constant memory. So at a given time, only the number of bytes indicated by @partSize
        * will be loaded in memory.
        *
        * For small files, consider using [[readFile]] instead.
        */
      def readFileMultipart(
          bucket: BucketName,
          key: FileKey,
          partSize: PartSizeMB
      ): fs2.Stream[F, Byte] = {
        val chunkSizeBytes = partSize * 1000000

        // Range must be in the form "bytes=0-500" -> https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.35
        def go(offset: Long): Pull[F, Byte, Unit] =
          fs2.Stream
            .eval {
              s3.getObject(
                GetObjectRequest
                  .builder()
                  .range(s"bytes=$offset-${offset + chunkSizeBytes}")
                  .bucket(bucket.value)
                  .key(key.value)
                  .build(),
                AsyncResponseTransformer.toBytes[GetObjectResponse]
              )
            }
            .pull
            .last
            .flatMap {
              case Some(resp) =>
                Pull.eval {
                  Async[F].blocking {
                    val bs  = resp.asByteArray()
                    val len = bs.length
                    if (len < 0) None else Some(Chunk(unsafeWrapArray(bs)*))
                  }
                }
              case None =>
                Pull.eval(none.pure[F])
            }
            .flatMap {
              case Some(o) =>
                if (o.size < chunkSizeBytes) Pull.output(o)
                else Pull.output(o) >> go(offset + o.size)
              case None => Pull.done
            }

        go(0).stream
      }

    }

  /** Translates effect type from F to G using the supplied FunctionKs.
    */
  def mapK[F[_], G[_]](s3: S3[F])(fToG: F ~> G, gToF: G ~> F): S3[G] =
    new S3[G] {
      override def delete(bucket: BucketName, key: FileKey): G[Unit] =
        fToG(s3.delete(bucket, key))

      override def uploadFile(
          bucket: BucketName,
          key: FileKey,
          modifier: AwsRequestModifier.Upload1
      ): Pipe[G, Byte, ETag] =
        _.translate(gToF)
          .through(s3.uploadFile(bucket, key, modifier))
          .translate(fToG)

      override def uploadFileMultipart(
          bucket: BucketName,
          key: FileKey,
          partSize: PartSizeMB,
          uploadEmptyFiles: UploadEmptyFiles,
          multiPartConcurrency: MultiPartConcurrency = 10
      ): Pipe[G, Byte, Option[ETag]] =
        _.translate(gToF)
          .through(
            s3.uploadFileMultipart(
              bucket,
              key,
              partSize,
              uploadEmptyFiles,
              multiPartConcurrency,
              requestModifier,
              multipartETagValidation.map { validator =>
                new MultipartETagValidation[F] {
                  override def validateETag(eTag: ETag, maxPartNumber: PartNumber, checksum: Checksum): F[Unit] =
                    gToF(validator.validateETag(eTag, maxPartNumber, checksum))
                }
              }
            )
          )
          .translate(fToG)

      override def readFile(bucket: BucketName, key: FileKey): fs2.Stream[G, Byte] =
        s3.readFile(bucket, key).translate(fToG)

      override def readFileMultipart(
          bucket: BucketName,
          key: FileKey,
          partSize: PartSizeMB
      ): fs2.Stream[G, Byte] = s3.readFileMultipart(bucket, key, partSize).translate(fToG)
    }
}
