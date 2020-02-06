package fs2
package aws

import java.util.concurrent.Executors

import cats.effect.{ContextShift, IO}
import fs2.aws.s3._
import fs2.aws.utils._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class S3Spec extends AnyFlatSpec with Matchers {

  private val blockingEC                        = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(6))
  implicit val ec: ExecutionContext             = ExecutionContext.global
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)

  ignore should "stdout the jsonfile" in {
    readS3FileMultipart[IO]("resources", "jsontest.json", 25, s3TestClient).compile.toVector.unsafeRunSync should be(
      Vector())
  }

  "Downloading the JSON test file by chunks" should "return the same content" in {
    readS3FileMultipart[IO]("resources", "jsontest.json", 25, s3TestClient)
      .through(fs2.text.utf8Decode)
      .through(fs2.text.lines)
      .compile
      .toVector
      .unsafeRunSync
      .reduce(_ + _)
      .concat("") should be(
      """{"test": 1}{"test": 2}{"test": 3}{"test": 4}{"test": 5}{"test": 6}{"test": 7}{"test": 8}""")
  }

  "Downloading the JSON test file" should "return the same content" in {
    readS3File[IO]("resources", "jsontest.json", blockingEC, s3TestClient)
      .through(fs2.text.utf8Decode)
      .through(fs2.text.lines)
      .compile
      .toVector
      .unsafeRunSync
      .reduce(_ + _)
      .concat("") should be(
      """{"test": 1}{"test": 2}{"test": 3}{"test": 4}{"test": 5}{"test": 6}{"test": 7}{"test": 8}""")
  }

  "big chunk size but small entire text" should "be trimmed to content" in {
    readS3FileMultipart[IO]("resources", "jsontest1.json", 25, s3TestClient)
      .through(fs2.text.utf8Decode)
      .through(fs2.text.lines)
      .compile
      .toVector
      .unsafeRunSync
      .reduce(_ + _)
      .concat("") should be("""{"test": 1}""")
  }
}
