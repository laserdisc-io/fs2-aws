package fs2.aws.examples

import cats.data.Kleisli
import cats.effect.*
import eu.timepit.refined.types.string.NonEmptyString
import fs2.aws.s3.S3
import fs2.aws.s3.models.Models.{BucketName, FileKey}
import fs2.text
import trace4cats.Span
import trace4cats.Trace
import io.laserdisc.pure.s3.tagless.{Interpreter as S3Interpreter, S3AsyncClientOp}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient

import java.net.URI

object TracedS3Example extends IOApp {
  val credentials: AwsBasicCredentials = AwsBasicCredentials
    .create("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
  val port = 9000
  override def run(args: List[String]): IO[ExitCode] =
    s3StreamResource.map(S3.create[IO]).use(s3 => program(s3).as(ExitCode.Success))

  def s3StreamResource: Resource[IO, S3AsyncClientOp[IO]] =
    S3Interpreter[IO].S3AsyncClientOpResource(
      S3AsyncClient
        .builder()
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .endpointOverride(URI.create(s"http://localhost:$port"))
        .region(Region.US_EAST_1)
    )

  def tracedS3Read[F[_]: Sync: Trace](s3: S3[F]): F[Unit] =
    Trace[F].span("read_s3_file") {
      s3.readFile(
        BucketName(NonEmptyString.unsafeFrom("resources")),
        FileKey(NonEmptyString.unsafeFrom("jsontest.json"))
      ).through(text.utf8.decode)
        .through(fs2.text.lines)
        .evalTap(l => Sync[F].delay(println(l)))
        .compile
        .drain
    }

  def program(s3: S3[IO]): IO[Unit] = {
    val spanResource: Resource[IO, Span[IO]] = Span.noop[IO] // Replace with your real span
    spanResource.use { span =>
      val tracedS3: S3[Kleisli[IO, Span[IO], *]] =
        S3.mapK(s3)(Kleisli.liftK, Kleisli.applyK(span))
      tracedS3Read(tracedS3).run(span)
    }
  }
}
