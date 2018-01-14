import java.io.ByteArrayInputStream

import cats.effect.IO
import org.scalatest.{FlatSpec, Matchers}
import fs2.aws.s3._
import fs2.aws.utils._

class S3Spec extends FlatSpec with Matchers {

  ignore should "stdout the jsonfile" in {
    readS3FileMultipart[IO]("json", "file", 25, s3TestClient)
      .through(fs2.io.stdout)
      .compile
      .toVector
      .unsafeRunSync should be(Vector())
  }

  "Downloading the JSON test file" should "return the same content" in {
    readS3FileMultipart[IO]("json", "file", 25, s3TestClient)
      .through(fs2.text.utf8Decode)
      .through(fs2.text.lines)
      .compile
      .toVector
      .unsafeRunSync
      .reduce(_+_)
      .concat("")
      .trim should be(new String(testJson).lines.reduce(_+_))
  }

  "Decompressing a multipart downloaded JSON Gzipped file" should "work" in {
    readS3FileMultipart[IO]("jsongzip", "file", 25, s3TestClient)
      .through(fs2.compress.deflate(nowrap = true))
      .through(fs2.text.utf8Decode)
      .compile
      .toVector
      .unsafeRunSync
      .reduce(_+_) should be("Hey")
  }
  "Reading a JSON Gzipped file" should "work" in {
    fs2.io.readInputStream[IO](IO(new ByteArrayInputStream(testJsonGzip)), 25)
      .through(fs2.compress.deflate(nowrap = true))
      .through(fs2.text.utf8Decode)
      .compile
      .toVector
      .unsafeRunSync
      .reduce(_+_) should be("Hey")
  }
}
