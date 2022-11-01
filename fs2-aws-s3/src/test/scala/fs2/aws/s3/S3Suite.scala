package fs2.aws.s3

import cats.effect.*
import cats.implicits.*
import eu.timepit.refined.types.string.NonEmptyString
import fs2.aws.s3.models.Models.{BucketName, FileKey, PartSizeMB}
import fs2.io.file.{Files, Flags, Path}
import fs2.text
import io.laserdisc.pure.s3.tagless.{Interpreter, S3AsyncClientOp}
import munit.CatsEffectSuite
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.endpoints.Endpoint
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

import java.net.{URI, URL}
import scala.concurrent.Future
import scala.jdk.FutureConverters.FutureOps

class S3Suite extends CatsEffectSuite {

  val credentials: AwsBasicCredentials =
    AwsBasicCredentials
      .create("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")

  val s3R: Resource[IO, S3AsyncClientOp[IO]] =
    Interpreter[IO].S3AsyncClientOpResource(
      S3AsyncClient
        .builder()
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .endpointProvider(_ => Future {
          Endpoint.builder().url(URI.create("http://localhost:9000")).build()
        }.asJava.toCompletableFuture)
        .region(Region.US_EAST_1)
    )

  val testFile: URL = getClass.getResource("/jsontest.json")

  val bucket: BucketName   = BucketName(NonEmptyString.unsafeFrom("resources"))
  val fileKey: FileKey     = FileKey(NonEmptyString.unsafeFrom("jsontest.json"))
  val partSize: PartSizeMB = PartSizeMB.unsafeFrom(5)

  test("Upload JSON test file & read it back") {
    s3R.use { s3Op =>
      S3.create[IO](s3Op).flatMap { s3 =>
        val upload =
          Files[IO]
            .readAll(Path(testFile.getPath), 4096, Flags.Read)
            .through(s3.uploadFile(bucket, fileKey))
            .compile
            .drain

        val read =
          s3.readFile(bucket, fileKey)
            .through(text.utf8.decode)
            .through(fs2.text.lines)
            .compile
            .toVector
            .map { res =>
              val expected =
                """{"test": 1}{"test": 2}{"test": 3}{"test": 4}{"test": 5}{"test": 6}{"test": 7}{"test": 8}"""
              assert(res.reduce(_ + _).concat("") === expected)
            }

        upload >> read
      }
    }
  }

  test(
    "Upload JSON test file in multipart fashion & read it back in a one go, then delete it and try to read it again"
  ) {
    s3R.use { s3 =>
      S3.create[IO](s3).flatMap { s3 =>
        val fileKeyMix = FileKey(NonEmptyString.unsafeFrom("jsontest-mix.json"))
        val upload =
          Files[IO]
            .readAll(Path(testFile.getPath), 4096, Flags.Read)
            .through(
              s3.uploadFileMultipart(bucket, fileKeyMix, partSize)
            )
            .compile
            .drain
        val read =
          s3.readFile(bucket, fileKeyMix)
            .through(text.utf8.decode)
            .through(fs2.text.lines)
            .compile
            .toVector
            .map { res =>
              val expected =
                """{"test": 1}{"test": 2}{"test": 3}{"test": 4}{"test": 5}{"test": 6}{"test": 7}{"test": 8}"""
              assert(res.reduce(_ + _).concat("") === expected)
            }
        val delete =
          s3.delete(bucket, fileKeyMix)
        val readNonExisting =
          read.handleError {
            case _: NoSuchKeyException => assert(true)
            case _                     => assert(false)
          }
        upload >> read >> delete >> readNonExisting
      }
    }
  }

  test("Upload JSON test file & read it back in multipart fashion all the way") {

    s3R.use { s3 =>
      S3.create[IO](s3).flatMap { s3 =>
        val fileKeyMultipart = FileKey(NonEmptyString.unsafeFrom("jsontest-multipart.json"))

        val upload =
          Files[IO]
            .readAll(Path(testFile.getPath), 4096, Flags.Read)
            .through(s3.uploadFileMultipart(bucket, fileKeyMultipart, partSize))
            .compile
            .drain

        val read =
          s3.readFileMultipart(bucket, fileKeyMultipart, partSize)
            .through(text.utf8.decode)
            .through(fs2.text.lines)
            .compile
            .toVector
            .map { res =>
              val expected =
                """{"test": 1}{"test": 2}{"test": 3}{"test": 4}{"test": 5}{"test": 6}{"test": 7}{"test": 8}"""
              assert(res.reduce(_ + _).concat("") === expected)
            }

        upload >> read
      }
    }
  }

//  test("MapK can change the effect of the initial S3 instance") {
//    sealed trait Span[F[_]] //a span typeclass from your preferred tracing library
//    case object NoOpSpan extends Span[IO]
//    s3R.use { s3 =>
//      S3.create[IO](s3).map { s3 =>
//        assert(
//          S3.mapK(s3)(Kleisli.liftK[IO, Span[IO]], Kleisli.applyK[IO, Span[IO]](NoOpSpan))
//            .isInstanceOf[S3[Kleisli[IO, Span[IO], *]]]
//        )
//      }
//    }
//  }
}
