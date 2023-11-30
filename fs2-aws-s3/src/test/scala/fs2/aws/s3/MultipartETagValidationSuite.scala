package fs2.aws.s3

import cats.effect.*
import cats.implicits.*
import fs2.aws.s3.S3.MultipartETagValidation
import fs2.aws.s3.S3.MultipartETagValidation.{ETagValidated, InvalidChecksum}
import munit.CatsEffectSuite

class MultipartETagValidationSuite extends CatsEffectSuite {

  val multipartETagValidation = MultipartETagValidation.create[IO]

  test("it should validate a valid eTag should validate successfully") {

    val s3ETag     = "\"" + "77458ecf309cd60b283376e14bb03d55-4" + "\""
    val maxPartNum = 4L
    val checksum   = "77458ecf309cd60b283376e14bb03d55"
    val validator  = multipartETagValidation
    val result     = validator.validateETag(s3ETag, maxPartNum, checksum)
    assertIO(result, ETagValidated(s3ETag))
  }

  test("it should validate invalid eTag's by raising an error") {

    val s3ETag          = "\"" + "77458ecf309cd60b283376e14bb03d55-4" + "\""
    val validPartNumber = 4L
    val validChecksum   = "77458ecf309cd60b283376e14bb03d55"

    val wrongPart     = 5L
    val wrongChecksum = "88458ecf309cd60b283376e14bb03d77"

    val validator = multipartETagValidation

    val result: IO[List[Either[Throwable, ETagValidated]]] =
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
}
