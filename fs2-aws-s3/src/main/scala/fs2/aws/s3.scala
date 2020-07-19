package fs2.aws

import java.net.URI
import java.nio.ByteBuffer

import scala.jdk.CollectionConverters._

import cats.effect._
import cats.implicits._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.boolean.Or
import eu.timepit.refined.generic.Equal
import eu.timepit.refined.numeric.Greater
import eu.timepit.refined.types.string.NonEmptyString
import fs2._
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.core.sync.{RequestBody, ResponseTransformer}
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model._

object s3 {
  // Each part must be at least 5 MB in size
  // https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/S3Client.html#uploadPart-software.amazon.awssdk.services.s3.model.UploadPartRequest-software.amazon.awssdk.core.sync.RequestBody-
  type PartSizeMB = Int Refined (Greater[5] Or Equal[5])
  type ETag       = String

  final case class BucketName(value: NonEmptyString)
  final case class FileKey(value: NonEmptyString)

  /* A purely functional abstraction over the S3 API based on fs2.Stream */
  trait S3[F[_]] {
    def uploadFile(bucket: BucketName, key: FileKey): Pipe[F, Byte, ETag]
    def uploadFileMultipart(bucket: BucketName, key: FileKey, partSize: PartSizeMB): Pipe[F, Byte, ETag]
    def readFile(bucket: BucketName, key: FileKey): Stream[F, Byte]
    def readFileMultipart(bucket: BucketName, key: FileKey, partSize: PartSizeMB): Stream[F, Byte]
  }

  object S3 {
    type PartETag = String
    type PartId   = Int
    type UploadId = String

    /**
     * It creates an instance of the purely functional S3 API.
     *
     * Example:
     *
     * {{{
     * S3.create[IO](client, blocker).flatMap { s3 =>
     *   streamOfBytes
     *     .through(s3.uploadFileMultipart(BucketName("foo"), FileKey("bar"), partSize = 5))
     *     .evalMap(t => IO(println(s"eTag: $t")))
     *     .compile
     *     .drain
     * }
     * }}}
     */
    def create[F[_]: Concurrent: ContextShift](client: S3Client, blocker: Blocker): F[S3[F]] =
      new S3[F] {
        /**
         * Uploads a file in one request. Suitable for small files.
         *
         * For big files, consider using [[uploadFileMultipart]] instead.
         */
        def uploadFile(bucket: BucketName, key: FileKey): Pipe[F, Byte, ETag] =
          in => {
            Stream.eval {
              in.compile.toVector.flatMap { vs =>
                val bs = ByteBuffer.wrap(vs.toArray)
                blocker
                  .delay(
                    client.putObject(
                      PutObjectRequest.builder().bucket(bucket.value).key(key.value).build(),
                      RequestBody.fromByteBuffer(bs)
                    )
                  )
                  .map(_.eTag())
              }
            }
          }

        /**
         * Uploads a file in multiple parts of the specifed @partSize per request. Suitable for big files.
         *
         * It does so in constant memory. So at a given time, only the number of bytes indicated by @partSize
         * will be loaded in memory.
         *
         * For small files, consider using [[uploadFile]] instead.
         *
         * @partSize the part size indicated in MBs. It must be at least 5, as required by AWS.
         */
        def uploadFileMultipart(bucket: BucketName, key: FileKey, partSize: PartSizeMB): Pipe[F, Byte, ETag] = {
          val chunkSizeBytes = partSize * 1000000

          def initiateMultipartUpload: F[UploadId] =
            blocker
              .delay {
                client.createMultipartUpload(
                  CreateMultipartUploadRequest.builder().bucket(bucket.value).key(key.value).build()
                )
              }
              .map(_.uploadId())

          def uploadPart(uploadId: UploadId): Pipe[F, (Chunk[Byte], Long), (PartETag, PartId)] =
            _.evalMap {
              case (c, i) =>
                blocker
                  .delay {
                    client.uploadPart(
                      UploadPartRequest
                        .builder()
                        .bucket(bucket.value)
                        .key(key.value)
                        .uploadId(uploadId)
                        .partNumber(i.toInt)
                        .contentLength(c.size)
                        .build(),
                      RequestBody.fromBytes(c.toArray)
                    )
                  }
                  .map(_.eTag() -> i.toInt)
            }

          def completeUpload(uploadId: UploadId): Pipe[F, List[(PartETag, PartId)], ETag] =
            _.evalMap { tags =>
              val parts = tags.map { case (t, i) => CompletedPart.builder().partNumber(i).eTag(t).build() }.asJava
              blocker.delay {
                client.completeMultipartUpload(
                  CompleteMultipartUploadRequest
                    .builder()
                    .bucket(bucket.value)
                    .key(key.value)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                    .build()
                )
              }.map(_.eTag())
            }

          in => {
            Stream
              .eval(initiateMultipartUpload)
              .flatMap { uploadId =>
                in.chunkN(chunkSizeBytes)
                  .zip(Stream.iterate(1L)(_ + 1))
                  .through(uploadPart(uploadId))
                  .fold[List[(PartETag, PartId)]](List.empty)(_ :+ _)
                  .through(completeUpload(uploadId))
              }
          }
        }

        /**
         * Reads a file in one request. Suitable for small files.
         *
         * For big files, consider using [[readFileMultipart]] instead.
         */
        def readFile(bucket: BucketName, key: FileKey): Stream[F, Byte] =
          fs2.io.readInputStream(
            blocker.delay(
              client.getObject(
                GetObjectRequest
                  .builder()
                  .bucket(bucket.value)
                  .key(key.value)
                  .build()
              )
            ),
            chunkSize = 4096,
            closeAfterUse = true,
            blocker = blocker
          )

        /**
         * Reads a file in multiple parts of the specifed @partSize per request. Suitable for big files.
         *
         * It does so in constant memory. So at a given time, only the number of bytes indicated by @partSize
         * will be loaded in memory.
         *
         * For small files, consider using [[readFile]] instead.
         */
        def readFileMultipart(bucket: BucketName, key: FileKey, partSize: PartSizeMB): Stream[F, Byte] = {
          val chunkSizeBytes = partSize.value * 1000000

          // Range must be in the form "bytes=0-500" -> https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.35
          def go(offset: Int): Pull[F, Byte, Unit] =
            Stream
              .eval {
                blocker.delay {
                  client.getObject(
                    GetObjectRequest
                      .builder()
                      .range(s"bytes=$offset-${offset + chunkSizeBytes}")
                      .bucket(bucket.value)
                      .key(key.value)
                      .build(),
                    ResponseTransformer.toBytes[GetObjectResponse]
                  )
                }
              }
              .pull
              .last
              .flatMap {
                case Some(resp) =>
                  Pull.eval {
                    blocker.delay {
                      val bs  = resp.asByteArray()
                      val len = bs.length
                      if (len < 0) None else Some(Chunk.bytes(bs, 0, len))
                    }
                  }
                case None =>
                  Pull.eval(none.pure[F])
              }
              .flatMap {
                case Some(o) =>
                  if (o.size < chunkSizeBytes) Pull.output(o)
                  else Pull.output(o) >> go(offset + o.size)
                case None    => Pull.done
              }

          go(0).stream
        }

      }.pure[F]
  }
}
