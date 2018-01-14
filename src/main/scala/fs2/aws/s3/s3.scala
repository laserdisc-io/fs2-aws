package fs2
package aws

import java.io.InputStream

import cats.effect.{Effect, IO}
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.{AmazonS3Exception, GetObjectRequest, S3ObjectInputStream}
import fs2.aws.internal.Internal.S3Client

package object s3 {
  val javaJdkClient: S3Client[IO] = {
    val c = AmazonS3ClientBuilder.defaultClient
    new S3Client[IO] {
       def getObjectContent(getObjectRequest: GetObjectRequest) = IO(c.getObject(getObjectRequest).getObjectContent)
    }
  }
  def readFile[F[_]](bucket: String, key: String, chunkSize: Int, s3Client: S3Client[IO] = javaJdkClient)(implicit F: Effect[F]): fs2.Stream[F, Byte] = {
    val s3Client = AmazonS3ClientBuilder.defaultClient
    def readFilePart(offset: Int)(implicit F: Effect[F]): fs2.Pull[F, Byte, Unit] =
      fs2.Pull.acquire[F, Option[S3ObjectInputStream]]({
        F.delay(
          try Some(s3Client.getObject(new GetObjectRequest(bucket, key).withRange(offset, offset + chunkSize)).getObjectContent)
          catch {
            case _: AmazonS3Exception => None
          }
        )
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
          case Some(o) => fs2.Pull.outputChunk(o) >> readFilePart(offset + o.size)
          case None => fs2.Pull.done
        })


    readFilePart(0).stream

  }
}
