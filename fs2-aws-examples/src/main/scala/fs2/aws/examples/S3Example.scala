package fs2.aws.examples

import cats.effect.{IO, IOApp}
import fs2.aws.s3.S3
import fs2.aws.s3.models.Models.{BucketName, FileKey}
import io.laserdisc.pure.s3.tagless.S3Interpreter

object S3Example extends IOApp.Simple {

  override def run: IO[Unit] =
    S3Interpreter[IO].resource
      .map(S3.create)
      .use { s3 =>
        s3.readFile(
          BucketName.unsafeFrom("test-bucket"),
          FileKey.unsafeFrom("file-key")
        ).through(fs2.text.utf8.decode)
          .through(fs2.text.lines)
          .evalMap(IO.println)
          .compile
          .drain
      }

}
