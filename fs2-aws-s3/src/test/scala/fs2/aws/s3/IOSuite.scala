package fs2.aws

import java.util.concurrent.Executors

import cats.effect._
import munit._
import scala.concurrent.ExecutionContext

trait IOSuite extends FunSuite {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO]     = IO.timer(ExecutionContext.global)

  override def munitValueTransforms: List[ValueTransform] =
    super.munitValueTransforms :+ new ValueTransform("IO", {
      case ioa: IO[_] => IO.suspend(ioa).unsafeToFuture
    })

  val blocker = Blocker.liftExecutionContext(
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(6))
  )
}
