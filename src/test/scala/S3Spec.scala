import cats.effect.IO
import org.scalatest.{FlatSpec, Matchers}
import fs2.aws.s3._

class S3Spec extends FlatSpec with Matchers {
  "Downloading 100 bytes to String" should "be hello" in {
     readFile[IO]("data-pond", "avro/com.gilt.event.test.ClickEvent-2-2017-12-14-14-21-29-10a0b450-c1bd-4d45-99ff-739dba5d2840.avro", 100)
       .through(fs2.io.stdout)
       .compile
       .toVector
       .unsafeRunSync
    readFile[IO]("data-pond", "avro/com.gilt.event.test.ClickEvent-2-2017-12-14-14-21-29-10a0b450-c1bd-4d45-99ff-739dba5d2840.avro", 100)
      .through(fs2.text.utf8Decode)
      .compile
      .toVector
      .unsafeRunSync
      .reduce(_+_) should be("Hello")

  }

  "Downloading 100 bytes to String" should "be hey" in {
    readFile[IO]("data-pond", "json/com.gilt.event.test.ClickEvent-5-2017-12-18-12-28-04-6f978c3a-3d89-48b4-8525-5685b9c0e2f7.gz", 100)
      .through(fs2.compress.inflate(nowrap = false))
      .through(fs2.io.stdout)
      .compile
      .toVector
      .unsafeRunSync
    readFile[IO]("data-pond", "json/com.gilt.event.test.ClickEvent-5-2017-12-18-12-28-04-6f978c3a-3d89-48b4-8525-5685b9c0e2f7.gz", 100)
      .through(fs2.text.utf8Decode)
      .compile
      .toVector
      .unsafeRunSync
      .reduce(_+_) should be("Hey")

  }
}
