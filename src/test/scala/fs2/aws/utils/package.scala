package fs2
package aws

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import cats.effect.{Effect, IO}
import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.model.{AmazonS3Exception, GetObjectRequest, S3ObjectInputStream}
import fs2.aws.internal.Internal.S3Client
import org.apache.http.client.methods.HttpRequestBase
import java.util.zip.GZIPOutputStream

package object utils {
  val testJson =
    """{"test": 1}
      |{"test": 2}
      |{"test": 3}
      |{"test": 4}
      |{"test": 5}
      |{"test": 6}
      |{"test": 7}
      |{"test": 8}""".stripMargin.getBytes
  val testAvro =
    """
      | hey
    """.stripMargin.getBytes
  val testJsonGzip = {
    val bos = new ByteArrayOutputStream(testJson.length)
    val gzip = new GZIPOutputStream(bos)
    gzip.write(testJson)
    gzip.close()
    val compressed = bos.toByteArray
    bos.close()
    compressed
  }

  val s3TestClient: S3Client[IO] = new S3Client[IO] {
    override def getObjectContent(getObjectRequest: GetObjectRequest)(implicit e: Effect[IO]) : IO[S3ObjectInputStream] = getObjectRequest match {
      case goe: GetObjectRequest => {
        val is : InputStream = {
          val fileContent =
            if(goe.getBucketName == "json" && goe.getKey == "file")
              testJson
            else if (goe.getBucketName == "avro" && goe.getKey == "file")
              testAvro
            else if (goe.getBucketName == "jsongzip" && goe.getKey == "file")
              testJsonGzip
            else
              throw new AmazonS3Exception("File not found")
          goe.getRange match {
            case Array(x, y) =>
              if (x >= fileContent.length) throw new AmazonS3Exception("Invalid range")
              else if (y > fileContent.length) new ByteArrayInputStream(fileContent.slice(x.toInt, fileContent.length))
              else new ByteArrayInputStream(fileContent.slice(x.toInt, y.toInt))
          }
        }

        IO {
          Thread.sleep(500)  // simulate a call to S3
          new S3ObjectInputStream(is, new HttpRequestBase { def getMethod = "" })
        }
      }
      case _ => throw new SdkClientException("Invalid GetObjectRequest")
    }
  }
}
