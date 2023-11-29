package fs2.aws.s3

import cats.effect.*
import cats.implicits.*
import eu.timepit.refined.types.string.NonEmptyString
import fs2.aws.s3.S3.MultipartETagValidation
import fs2.aws.s3.S3.MultipartETagValidation.InvalidChecksum
import fs2.aws.s3.models.Models.{BucketName, FileKey, PartSizeMB}
import fs2.io.file.{Files, Flags, Path}
import fs2.text
import io.laserdisc.pure.s3.tagless.{Interpreter, S3AsyncClientOp}
import munit.CatsEffectSuite
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.{ListMultipartUploadsRequest, NoSuchKeyException}
import software.amazon.awssdk.services.s3.{S3AsyncClient, S3Configuration}

import java.net.{URI, URL}
import java.util.UUID

class S3Suite extends CatsEffectSuite {

  val credentials: AwsBasicCredentials =
    AwsBasicCredentials
      .create("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")

  val multipartETagValidation = MultipartETagValidation.create[IO]

  def s3R: Resource[IO, S3AsyncClientOp[IO]] =
    Interpreter[IO].S3AsyncClientOpResource(
      S3AsyncClient
        .builder()
        .endpointOverride(URI.create("http://localhost:9000"))
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .serviceConfiguration(
          // see https://stackoverflow.com/a/61602647
          S3Configuration
            .builder()
            .pathStyleAccessEnabled(true)
            .build()
        )
        .region(Region.US_EAST_1)
    )

  val testFile: URL = getClass.getResource("/jsontest.json")

  val bucket: BucketName   = BucketName(NonEmptyString.unsafeFrom("resources"))
  val fileKey: FileKey     = FileKey(NonEmptyString.unsafeFrom("jsontest.json"))
  val partSize: PartSizeMB = PartSizeMB.unsafeFrom(5)

  test("Upload JSON test file & read it back") {
    s3R.map(S3.create[IO](_)).use { s3 =>
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

  test(
    "Upload JSON test file in multipart fashion & read it back in a one go, then delete it and try to read it again"
  ) {
    s3R.use { s3 =>
      s3R.map(S3.create[IO](_)).use { s3 =>
        val fileKeyMix = FileKey(NonEmptyString.unsafeFrom("jsontest-mix.json"))
        val upload =
          Files[IO]
            .readAll(Path(testFile.getPath), 4096, Flags.Read)
            .through(
              s3.uploadFileMultipart(bucket, fileKeyMix, partSize, eTagValidation = multipartETagValidation.some)
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
      s3R.map(S3.create[IO](_)).use { s3 =>
        val fileKeyMultipart = FileKey(NonEmptyString.unsafeFrom("jsontest-multipart.json"))

        val upload =
          Files[IO]
            .readAll(Path(testFile.getPath), 4096, Flags.Read)
            .through(
              s3.uploadFileMultipart(bucket, fileKeyMultipart, partSize, eTagValidation = multipartETagValidation.some)
            )
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

  test(
    "Gracefully abort multipart upload when no data has been passed through the stream and `uploadEmptyFile` is disabled."
  ) {

    s3R.map(s3 => s3 -> S3.create[IO](s3)).use { case (s3Ops, s3) =>
      for {
        fileKey <- IO.delay(UUID.randomUUID().toString).map(k => FileKey(NonEmptyString.unsafeFrom(k)))
        uploadedTags <- fs2.Stream.empty
          .through(s3.uploadFileMultipart(bucket, fileKey, partSize, eTagValidation = multipartETagValidation.some))
          .compile
          .last
          .map(_.get)
        activeUploads <- s3Ops
          .listMultipartUploads(
            ListMultipartUploadsRequest
              .builder()
              .bucket(bucket.value.value)
              .prefix(fileKey.value.value)
              .build()
          )
          .map(_.uploads())
      } yield {
        assert(uploadedTags.isEmpty)
        assert(activeUploads.isEmpty)
      }
    }
  }

  test(
    "upload an empty file when using multipart upload when no data has been passed through the stream and `uploademptyfile` is enabled."
  ) {

    s3R.map(s3 => s3 -> S3.create[IO](s3)).use { case (s3Ops, s3) =>
      for {
        fileKey <- IO.delay(UUID.randomUUID().toString).map(k => FileKey(NonEmptyString.unsafeFrom(k)))
        uploadedTags <- fs2.Stream.empty
          .through(
            s3.uploadFileMultipart(
              bucket,
              fileKey,
              partSize,
              uploadEmptyFiles = true,
              eTagValidation = multipartETagValidation.some
            )
          )
          .compile
          .last
          .map(_.get)
        read <- s3
          .readFile(bucket, fileKey)
          .compile
          .toVector
          .map(_.toArray)
        activeUploads <- s3Ops
          .listMultipartUploads(
            ListMultipartUploadsRequest
              .builder()
              .bucket(bucket.value.value)
              .prefix(fileKey.value.value)
              .build()
          )
          .map(_.uploads())
      } yield {
        assert(uploadedTags.nonEmpty)
        assert(read.isEmpty)
        assert(activeUploads.isEmpty)
      }
    }
  }

  test("it should validate a valid eTag should validate successfully") {

    val s3ETag     = "\"" + "77458ecf309cd60b283376e14bb03d55-4" + "\""
    val maxPartNum = 4L
    val checksum   = "77458ecf309cd60b283376e14bb03d55"
    val validator  = multipartETagValidation
    val result     = validator.validateETag(s3ETag, maxPartNum, checksum)
    assertIO_(result)
  }

  test("it should validate invalid eTag's by raising an error") {

    val s3ETag          = "\"" + "77458ecf309cd60b283376e14bb03d55-4" + "\""
    val validPartNumber = 4L
    val validChecksum   = "77458ecf309cd60b283376e14bb03d55"

    val wrongPart     = 5L
    val wrongChecksum = "88458ecf309cd60b283376e14bb03d77"

    val validator = multipartETagValidation

    val result: IO[List[Either[Throwable, Unit]]] =
      List(
        validator.validateETag(s3ETag, wrongPart, validChecksum).attempt,
        validator.validateETag(s3ETag, validPartNumber, wrongChecksum).attempt
      ).sequence

    val invalidChecksumCount = result.map(_.count {
      case Left(_: InvalidChecksum) => true
      case Left(_) | Right(_)       => false
    })

    assertIO(invalidChecksumCount, 2)

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
