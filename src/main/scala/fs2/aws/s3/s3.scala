package fs2
package aws

import java.io.InputStream

import cats.effect.Effect
import cats.implicits._
import com.amazonaws.services.s3.model.{AmazonS3Exception, GetObjectRequest, S3ObjectInputStream}
import fs2.aws.internal.Internal._

package object s3 {
  def readS3FileMultipart[F[_]](bucket: String, key: String, chunkSize: Int, s3Client: S3Client[F] = new S3Client[F] {})(implicit F: Effect[F]): fs2.Stream[F, Byte] = {
    def go(offset: Int)(implicit F: Effect[F]): fs2.Pull[F, Byte, Unit] =
      fs2.Pull.acquire[F, Option[S3ObjectInputStream]]({
          try s3Client.getObjectContent(new GetObjectRequest(bucket, key).withRange(offset, offset + chunkSize)).map(Some.apply)
          catch {
            case _: AmazonS3Exception => F.pure(None)
          }
      })({
        case Some(s) => F.delay(s.abort())
        case None => F.delay(() => ())
      }).flatMap({
        case Some(s) =>
          fs2.Pull.eval(F.delay {
            val is: InputStream = s
            val buf = new Array[Byte](chunkSize)
            is.read(buf)
            Some(Chunk.bytes(buf))
          })
        case None => fs2.Pull.eval(F.delay(None))
        }).flatMap({
          case Some(o) => fs2.Pull.outputChunk(o) >> go(offset + o.size)
          case None => fs2.Pull.done
        })


    go(0).stream

  }
}
