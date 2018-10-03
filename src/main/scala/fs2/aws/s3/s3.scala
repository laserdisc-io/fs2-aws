package fs2
package aws

import java.io.{ByteArrayInputStream, InputStream}

import cats.effect.Effect
import cats.implicits._
import com.amazonaws.services.s3.model._
import fs2.aws.internal.Internal._

import scala.collection.JavaConverters._

package object s3 {
  def readS3FileMultipart[F[_]](
      bucket: String,
      key: String,
      chunkSize: Int,
      s3Client: S3Client[F] = new S3Client[F] {})(implicit F: Effect[F]): fs2.Stream[F, Byte] = {
    def go(offset: Int)(implicit F: Effect[F]): fs2.Pull[F, Byte, Unit] =
      fs2.Pull
        .acquire[F, Option[S3ObjectInputStream]]({
          try s3Client
            .getObjectContent(
              new GetObjectRequest(bucket, key).withRange(offset, offset + chunkSize))
            .map(Some.apply)
          catch {
            case _: AmazonS3Exception => F.pure(None)
          }
        })({
          case Some(s) => F.delay(s.abort())
          case None    => F.delay(() => ())
        })
        .flatMap({
          case Some(s) =>
            fs2.Pull.eval(F.delay {
              val is: InputStream = s
              val buf             = new Array[Byte](chunkSize)
              val len = is.read(buf)
              Some(Chunk.bytes(buf, 0, len))
            })
          case None => fs2.Pull.eval(F.delay(None))
        })
        .flatMap({
          case Some(o) => fs2.Pull.outputChunk(o) >> go(offset + o.size)
          case None    => fs2.Pull.done
        })

    go(0).stream

  }

  def uploadS3FileMultipart[F[_]](
      bucket: String,
      key: String,
      objectMetadata: Option[ObjectMetadata],
      s3Client: S3Client[F] = new S3Client[F] {})(implicit F: Effect[F]): fs2.Sink[F, Byte] = {
    def uploadPart(uploadId: String): fs2.Pipe[F, (Chunk[Byte], Long), PartETag] =
      _.flatMap({
        case (c, i) =>
          fs2.Stream.eval(
            s3Client
              .uploadPart(
                new UploadPartRequest()
                  .withBucketName(bucket)
                  .withKey(key)
                  .withUploadId(uploadId)
                  .withPartNumber(i.toInt)
                  .withInputStream(new ByteArrayInputStream(c.toArray)))
              .flatMap(r => F.delay(r.getPartETag)))
      })

    def completeUpload(uploadId: String): fs2.Sink[F, List[PartETag]] =
      _.flatMap(
        parts =>
          fs2.Stream.eval_(s3Client.completeMultipartUpload(
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
        fs2.Stream
          .eval(mui)
          .flatMap(
            m =>
              in.chunks.zipWithIndex
                .through(uploadPart(m.uploadId))
                .fold[List[PartETag]](List())(_ :+ _)
                .to(completeUpload(m.uploadId)))
      }
  }
}
