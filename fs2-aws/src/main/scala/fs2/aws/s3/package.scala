package fs2.aws

import java.io.{ByteArrayInputStream, InputStream}

import cats.effect.{ContextShift, Effect}
import cats.implicits._
import com.amazonaws.services.s3.model._
import fs2.{Chunk, Pull, Stream}
import fs2.io.readInputStream
import fs2.aws.internal._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

package object s3 {
  def readS3FileMultipart[F[_]](
      bucket: String,
      key: String,
      chunkSize: Int,
      s3Client: S3Client[F] = new S3Client[F] {})(implicit F: Effect[F]): Stream[F, Byte] = {
    def go(offset: Int)(implicit F: Effect[F]): Pull[F, Byte, Unit] =
      Pull
        .acquire[F, Either[Throwable, InputStream]](s3Client.getObjectContentOrError(
          new GetObjectRequest(bucket, key).withRange(offset, offset + chunkSize))) {
          //todo: properly log the error
          case Left(e)  => F.delay(() => e.printStackTrace())
          case Right(s) => F.delay(s.close())
        }
        .flatMap {
          case Right(s3_is) =>
            Pull.eval(F.delay {
              val is: InputStream = s3_is
              val buf             = new Array[Byte](chunkSize)
              val len             = is.read(buf)
              if (len < 0) None else Some(Chunk.bytes(buf, 0, len))
            })
          case Left(_) => Pull.eval(F.delay(None))
        }
        .flatMap {
          case Some(o) => Pull.output(o) >> go(offset + o.size)
          case None    => Pull.done
        }

    go(0).stream

  }

  def readS3File[F[_]](bucket: String,
                       key: String,
                       blockingEC: ExecutionContext,
                       s3Client: S3Client[F] = new S3Client[F] {})(
      implicit F: Effect[F],
      cs: ContextShift[F]): Stream[F, Byte] = {
    readInputStream[F](s3Client.getObjectContent(new GetObjectRequest(bucket, key)),
                       chunkSize = 8192,
                       blockingExecutionContext = blockingEC,
                       closeAfterUse = true)
  }

  def uploadS3FileMultipart[F[_]](
      bucket: String,
      key: String,
      objectMetadata: Option[ObjectMetadata] = None,
      s3Client: S3Client[F] = new S3Client[F] {})(implicit F: Effect[F]): fs2.Sink[F, Byte] = {
    def uploadPart(uploadId: String): fs2.Pipe[F, (Chunk[Byte], Long), PartETag] =
      _.flatMap({
        case (c, i) =>
          Stream.eval(
            s3Client
              .uploadPart(
                new UploadPartRequest()
                  .withBucketName(bucket)
                  .withKey(key)
                  .withUploadId(uploadId)
                  .withPartNumber(i.toInt)
                  .withPartSize(c.size)
                  .withInputStream(new ByteArrayInputStream(c.toArray)))
              .flatMap(r => F.delay(r.getPartETag)))
      })

    def completeUpload(uploadId: String): fs2.Sink[F, List[PartETag]] =
      _.flatMap(
        parts =>
          Stream.eval_(s3Client.completeMultipartUpload(
            new CompleteMultipartUploadRequest(bucket, key, uploadId, parts.asJava))))

    in =>
      {
        val imuF: F[InitiateMultipartUploadResult] = objectMetadata match {
          case Some(o) =>
            s3Client.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucket, key, o))
          case None =>
            s3Client.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucket, key))
        }
        val mui: F[MultiPartUploadInfo] =
          imuF.flatMap(imu => F.pure(MultiPartUploadInfo(imu.getUploadId, List())))
        Stream
          .eval(mui)
          .flatMap(
            m =>
              in.chunks
                .zip(Stream.iterate(1L)(_ + 1))
                .through(uploadPart(m.uploadId))
                .fold[List[PartETag]](List())(_ :+ _)
                .to(completeUpload(m.uploadId)))
      }
  }

  def listFiles[F[_]](bucketName: String, s3Client: S3Client[F] = new S3Client[F] {})(
      implicit F: Effect[F]): Stream[F, S3ObjectSummary] = {
    val req = new ListObjectsV2Request().withBucketName(bucketName)
    Stream.eval(s3Client.s3ObjectSummaries(req)).flatMap(list => Stream.emits(list))
  }
}
