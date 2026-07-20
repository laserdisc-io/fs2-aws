package fs2.aws.examples

import cats.effect.{IO, IOApp, Resource}
import fs2.aws.s3.S3
import fs2.aws.s3.models.Models.{BucketName, FileKey}
import io.laserdisc.pure.s3.tagless.{Interpreter as S3Interpreter, S3AsyncClientOp}
import software.amazon.awssdk.services.s3.S3AsyncClient

object S3Example extends IOApp.Simple {

  def s3Resource: Resource[IO, S3[IO]] =
    S3Interpreter[IO]
      .S3AsyncClientOpResource(S3AsyncClient.builder())
      .map(S3.create[IO])

  override def run: IO[Unit] = s3Resource.use { s3 =>
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
