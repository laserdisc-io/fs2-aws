package fs2
package aws

import java.util.concurrent.Executors

import cats.effect.{ ContextShift, IO }
import com.amazonaws.services.s3.AmazonS3
import fs2.aws.internal.S3Client
import fs2.aws.s3._
import org.mockito.MockitoSugar._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class S3Spec extends AnyFlatSpec with Matchers {

  private val blockingEC                        = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(6))
  implicit val ec: ExecutionContext             = ExecutionContext.global
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)

  implicit val s3Client: S3Client[IO] = fs2.aws.utils.s3TestClient
  val mockS3                          = mock[AmazonS3]

  ignore should "stdout the jsonfile" in {
    readS3FileMultipart[IO]("resources", "jsontest.json", 25, mockS3).compile.toVector.unsafeRunSync should be(
      Vector()
    )
  }

  "Downloading the JSON test file by chunks" should "return the same content" in {
    readS3FileMultipart[IO]("resources", "jsontest.json", 25, mockS3)
      .through(fs2.text.utf8Decode)
      .through(fs2.text.lines)
      .compile
      .toVector
      .unsafeRunSync
      .reduce(_ + _)
      .concat("") should be(
      """{"test": 1}{"test": 2}{"test": 3}{"test": 4}{"test": 5}{"test": 6}{"test": 7}{"test": 8}"""
    )
  }

  "Downloading the JSON test file" should "return the same content" in {
    readS3File[IO]("resources", "jsontest.json", blockingEC, mockS3)
      .through(fs2.text.utf8Decode)
      .through(fs2.text.lines)
      .compile
      .toVector
      .unsafeRunSync
      .reduce(_ + _)
      .concat("") should be(
      """{"test": 1}{"test": 2}{"test": 3}{"test": 4}{"test": 5}{"test": 6}{"test": 7}{"test": 8}"""
    )
  }

  "Downloading the versioned JSON test file" should "return the same content" in {
    readS3VersionedFile[IO]("resources", "jsontest.json", version = "ABC", blockingEC, mockS3)
      .through(fs2.text.utf8Decode)
      .through(fs2.text.lines)
      .compile
      .toVector
      .unsafeRunSync
      .reduce(_ + _)
      .concat("") should be(
      """{"this": 1}{"is": 2}{"versioned": 3}{"content": 4}"""
    )
  }

  "big chunk size but small entire text" should "be trimmed to content" in {
    readS3FileMultipart[IO]("resources", "jsontest1.json", 25, mockS3)
      .through(fs2.text.utf8Decode)
      .through(fs2.text.lines)
      .compile
      .toVector
      .unsafeRunSync
      .reduce(_ + _)
      .concat("") should be("""{"test": 1}""")
  }
}
