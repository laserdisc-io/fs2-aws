package fs2
package aws

import cats.effect.IO
import org.scalatest.{FlatSpec, Matchers}
import fs2.aws.s3._
import fs2.aws.utils._

class S3Spec extends FlatSpec with Matchers {

  ignore should "stdout the jsonfile" in {
    readS3FileMultipart[IO]("resources", "jsontest.json", 25, s3TestClient).compile.toVector.unsafeRunSync should be(
      Vector())
  }

  "Downloading the JSON test file" should "return the same content" in {
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
