package fs2.aws.s3

import cats.effect.*
import cats.implicits.*
import cats.~>
import eu.timepit.refined.auto.*
import fs2.aws.s3.models.Models.{BucketName, ETag, FileKey, PartSizeMB, UploadEmptyFiles}
import fs2.{Chunk, Pipe, Pull}
import io.laserdisc.pure.s3.tagless.S3AsyncClientOp
import software.amazon.awssdk.core.async.{AsyncRequestBody, AsyncResponseTransformer}
import software.amazon.awssdk.services.s3.model.*

import java.nio.ByteBuffer
import scala.collection.immutable.ArraySeq
import scala.collection.immutable.ArraySeq.unsafeWrapArray
import scala.jdk.CollectionConverters.*

/* A purely functional abstraction over the S3 API based on fs2.Stream */
trait S3[F[_]] {
  def delete(bucket: BucketName, key: FileKey): F[Unit]
  def uploadFile(bucket: BucketName, key: FileKey): Pipe[F, Byte, ETag]

  def uploadFileMultipart(
      bucket: BucketName,
      key: FileKey,
      partSize: PartSizeMB,
      uploadEmptyFiles: UploadEmptyFiles = false
  ): Pipe[F, Byte, Option[ETag]]
  def readFile(bucket: BucketName, key: FileKey): fs2.Stream[F, Byte]

  def readFileMultipart(
      bucket: BucketName,
      key: FileKey,
      partSize: PartSizeMB
  ): fs2.Stream[F, Byte]
}

object S3 {
  type PartETag = String
  type PartId   = Int
  type UploadId = String

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
      def uploadFile(bucket: BucketName, key: FileKey): Pipe[F, Byte, ETag] =
        in =>
          fs2.Stream.eval {
            in.compile.toVector.flatMap { vs =>
              val bs = ByteBuffer.wrap(vs.toArray)
              s3.putObject(
                PutObjectRequest.builder().bucket(bucket.value).key(key.value).build(),
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
        * @param partSize the part size indicated in MBs. It must be at least 5, as required by AWS.
        */
      def uploadFileMultipart(
          bucket: BucketName,
          key: FileKey,
          partSize: PartSizeMB,
          uploadEmptyFiles: UploadEmptyFiles
      ): Pipe[F, Byte, Option[ETag]] = {
        val chunkSizeBytes = partSize * 1048576

        def initiateMultipartUpload: F[UploadId] =
          s3.createMultipartUpload(
            CreateMultipartUploadRequest.builder().bucket(bucket.value).key(key.value).build()
          ).map(_.uploadId())

        def uploadPart(uploadId: UploadId): Pipe[F, (Chunk[Byte], Long), (PartETag, PartId)] =
          _.evalMap { case (c, i) =>
            s3.uploadPart(
              UploadPartRequest
                .builder()
                .bucket(bucket.value)
                .key(key.value)
                .uploadId(uploadId)
                .partNumber(i.toInt)
                .contentLength(c.size.toLong)
                .build(),
              AsyncRequestBody.fromBytes(c.toArray)
            ).map(_.eTag() -> i.toInt)
          }

        def completeUpload(uploadId: UploadId): Pipe[F, List[(PartETag, PartId)], Option[ETag]] =
          _.evalMap { tags =>
            if (tags.nonEmpty) {
              val parts = tags.map { case (t, i) =>
                CompletedPart.builder().partNumber(i).eTag(t).build()
              }.asJava
              s3.completeMultipartUpload(
                CompleteMultipartUploadRequest
                  .builder()
                  .bucket(bucket.value)
                  .key(key.value)
                  .uploadId(uploadId)
                  .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                  .build()
              ).map(eTag => Option.apply[ETag](eTag.eTag()))
            } else {
              if (uploadEmptyFiles) {
                s3.putObject(
                  PutObjectRequest.builder().bucket(bucket.value).key(key.value).build(),
                  AsyncRequestBody.fromBytes(new Array[Byte](0))
                ).map(eTag => Option.apply[ETag](eTag.eTag()))
              } else {
                cancelUpload(uploadId).map(_ => Option.empty[ETag])
              }
            }
          }

        def cancelUpload(uploadId: UploadId): F[Unit] =
          s3.abortMultipartUpload(
            AbortMultipartUploadRequest
              .builder()
              .bucket(bucket.value)
              .key(key.value)
              .uploadId(uploadId)
              .build()
          ).void

        in =>
          fs2.Stream
            .eval(initiateMultipartUpload)
            .flatMap { uploadId =>
              in.chunkN(chunkSizeBytes)
                .zip(fs2.Stream.iterate(1L)(_ + 1))
                .through(uploadPart(uploadId))
                .fold[List[(PartETag, PartId)]](List.empty)(_ :+ _)
                .through(completeUpload(uploadId))
                .handleErrorWith(ex => fs2.Stream.eval(cancelUpload(uploadId) >> Sync[F].raiseError(ex)))
            }
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

      override def uploadFile(bucket: BucketName, key: FileKey): Pipe[G, Byte, ETag] =
        _.translate(gToF)
          .through(s3.uploadFile(bucket, key))
          .translate(fToG)

      override def uploadFileMultipart(
          bucket: BucketName,
          key: FileKey,
          partSize: PartSizeMB,
          uploadEmptyFiles: UploadEmptyFiles
      ): Pipe[G, Byte, Option[ETag]] =
        _.translate(gToF)
          .through(s3.uploadFileMultipart(bucket, key, partSize, uploadEmptyFiles))
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
