package fs2.aws

import java.util.Date

import cats.implicits.*
import _root_.ciris.ConfigDecoder
import software.amazon.kinesis.common.InitialPositionInStream

import scala.util.matching.Regex

package object ciris {

  /** Ciris decoder support for Either[InitialPositionInStream, Date], useful when building [[fs2.aws.kinesis.KinesisConsumerSettings]]
    *
    * Usage:
    * `env("FOOBAR").as[Either[InitialPositionInStream, Date]]`
    *
    * <ul>
    * <li>`"TRIM_HORIZON"` becomes `Left(InitialPositionInStream.TRIM_HORIZON)` (consume from beginning)</li>
    * <li>`"LATEST"` becomes `(Left[InitialPositionInStream.LATEST)` (consume from latest)</li>
    * <li>`"TS_123456"` becomes `Right(Date(123456))` (consume from timestamp)</li>
    * </ul>
    */
  implicit def kinesisInitialPositionDecoder[T](implicit
      decoder: ConfigDecoder[T, String]
  ): ConfigDecoder[T, Either[InitialPositionInStream, Date]] = {

    val DateRegex: Regex = """^TS_([0-9]+)$""".r

    decoder.mapOption(typeName = "InitialPositionInStream") {
      case "TRIM_HORIZON"    => Some(Left(InitialPositionInStream.TRIM_HORIZON))
      case "LATEST"          => Some(Left(InitialPositionInStream.LATEST))
      case DateRegex(millis) => Some(Right(new Date(millis.toLong)))
      case _                 => None
    }
  }

}
