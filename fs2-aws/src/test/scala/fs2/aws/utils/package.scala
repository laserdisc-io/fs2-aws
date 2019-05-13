package fs2.aws

import java.io._

import cats.effect.{Effect, IO}
import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.model.{AmazonS3Exception, GetObjectRequest, S3ObjectInputStream}
import fs2.aws.internal._
import org.apache.http.client.methods.HttpRequestBase
import scala.io.Source

package object utils {
  val s3TestClient: S3Client[IO] = new S3Client[IO] {
    override def getObjectContentOrError(getObjectRequest: GetObjectRequest)(
        implicit e: Effect[IO]): IO[Either[Throwable, InputStream]] =
      getObjectRequest match {
        case goe: GetObjectRequest => {
          IO[Either[Throwable, ByteArrayInputStream]] {
            val fileContent: Array[Byte] =
              try {
                Source.fromInputStream(Thread.currentThread.getContextClassLoader.getResourceAsStream(goe.getKey)).mkString.getBytes
              } catch {
                case _: FileNotFoundException => throw new AmazonS3Exception("File not found")
                case e: Throwable             => throw e
              }
            goe.getRange match {
              case Array(x, y) =>
                if (x >= fileContent.length) Left(new AmazonS3Exception("Invalid range"))
                else if (y > fileContent.length)
                  Right(new ByteArrayInputStream(fileContent.slice(x.toInt, fileContent.length)))
                else Right(new ByteArrayInputStream(fileContent.slice(x.toInt, y.toInt)))
            }

          } map {
            case Left(e) => Left(e)
            case Right(is) =>
              Thread.sleep(500) // simulate a call to S3
              Right(new S3ObjectInputStream(is, new HttpRequestBase {
                def getMethod = ""
              }))
          }
        }
        case _ => throw new SdkClientException("Invalid GetObjectRequest")
      }

    override def getObjectContent(getObjectRequest: GetObjectRequest)(
        implicit e: Effect[IO]): IO[InputStream] =
      IO[ByteArrayInputStream] {
        val fileContent: Array[Byte] =
          try {
            Source.fromInputStream(Thread.currentThread.getContextClassLoader.getResourceAsStream(getObjectRequest.getKey)).mkString.getBytes
          } catch {
            case _: FileNotFoundException => throw new AmazonS3Exception("File not found")
            case e: Throwable             => throw e
          }
        new ByteArrayInputStream(fileContent)

      }.map { is =>
        Thread.sleep(500) // simulate a call to S3
        new S3ObjectInputStream(is, new HttpRequestBase {
          def getMethod = ""
        })
      }
  }

}
