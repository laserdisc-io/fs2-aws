package fs2.aws.s3

import cats.effect.*
import cats.effect.unsafe.IORuntime
import munit.*

trait IOSuite extends FunSuite {

  implicit val runtime: IORuntime = IORuntime.global

  override def munitValueTransforms: List[ValueTransform] =
    super.munitValueTransforms :+ new ValueTransform("IO", {
      case ioa: IO[_] => IO.defer(ioa).unsafeToFuture()
    })

}
