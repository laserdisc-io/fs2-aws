package fs2.aws.examples

import cats.effect.{ExitCode, IO, IOApp, Resource}
import eu.timepit.refined.types.string.NonEmptyString
import fs2.aws.s3.S3
import fs2.aws.s3.models.Models.{BucketName, FileKey}
import io.laserdisc.pure.s3.tagless.{Interpreter as S3Interpreter, S3AsyncClientOp}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient

import java.net.URI

object S3Example extends IOApp {
  val credentials = AwsBasicCredentials.create("accesskey", "secretkey")
  val port        = 4566
  override def run(args: List[String]): IO[ExitCode] =
    s3StreamResource.use(s3 => S3.create(s3).flatMap(program).as(ExitCode.Success))

  def s3StreamResource: Resource[IO, (S3AsyncClientOp[IO])] =
    S3Interpreter[IO].S3AsyncClientOpResource(
      S3AsyncClient
        .builder()
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .endpointOverride(URI.create(s"http://localhost:$port"))
        .region(Region.US_EAST_1)
    )

  def program(s3: S3[IO]): IO[Unit] =
    s3.readFile(
      BucketName(NonEmptyString.unsafeFrom("test")),
      FileKey(NonEmptyString.unsafeFrom("foo"))
    ).through(fs2.text.utf8.decode)
      .through(fs2.text.lines)
      .evalMap(line => IO(println(line)))
      .compile
      .drain
}
