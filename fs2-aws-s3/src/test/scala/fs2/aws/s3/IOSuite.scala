package fs2.aws

import cats.effect._
import cats.effect.unsafe.IORuntime
import munit._

trait IOSuite extends FunSuite {

  implicit val runtime: IORuntime = IORuntime.global

  override def munitValueTransforms: List[ValueTransform] =
    super.munitValueTransforms :+ new ValueTransform("IO", {
      case ioa: IO[_] => IO.defer(ioa).unsafeToFuture
    })

}
