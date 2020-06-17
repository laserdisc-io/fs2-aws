package fs2.aws.ciris;

import java.util.Date

import cats.effect.{ ContextShift, IO }
import ciris.{ ConfigException, ConfigValue }
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import software.amazon.kinesis.common.InitialPositionInStream

import scala.concurrent.ExecutionContext.Implicits.global;

class CirisDecoderSpec extends AnyWordSpec with Matchers {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  "InitialPositionDecoderSpec" should {

    "when decoding Either[InitialPositionInStream, Date]" can {

      // same package, so `import fs2.aws.ciris._` not necessary here
      def decode(testStr: String): Either[InitialPositionInStream, Date] =
        ConfigValue
          .default(testStr)
          .as[Either[InitialPositionInStream, Date]]
          .load[IO]
          .unsafeRunSync()

      def expectDecodeFailure(testString: String): Assertion =
        intercept[ConfigException] {
          decode(testString)
        }.getMessage should include(
          s"Unable to convert value $testString to InitialPositionInStream"
        )

      "decode supported strings as initial offsets" in {

        decode("LATEST")           should equal(Left(InitialPositionInStream.LATEST))
        decode("TRIM_HORIZON")     should equal(Left(InitialPositionInStream.TRIM_HORIZON))
        decode("TS_1592404273000") should equal(Right(new Date(1592404273000L)))

      }

      "fail to decode valid strings" in {

        expectDecodeFailure("FOOBAR")
        expectDecodeFailure("TS_FOO")
        expectDecodeFailure("TS_")
        expectDecodeFailure("_1592404273000")

      }
    }

  }

}
