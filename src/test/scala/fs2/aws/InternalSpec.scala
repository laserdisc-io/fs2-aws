package fs2
package aws

import cats.effect.{ContextShift, IO}
import fs2.aws.internal.Internal._
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext

class InternalSpec extends FlatSpec with Matchers {
  implicit val ec: ExecutionContext             = ExecutionContext.global
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)

  "groupBy" should "create K substreams based on K selector outputs" in {
    val k = 30
    val streams = Stream
      .emits((1 to 100000))
      .through(groupBy(i => IO(i % k)))
      .compile
      .toVector
      .unsafeRunSync

    streams.size shouldBe (k)
  }

  it should "split stream elements into respective substreams" in {
    val streams = Stream
      .emits((1 to 10))
      .through(groupBy(i => IO(i % 2)))
      .compile
      .toVector
      .unsafeRunSync

    streams.filter(_._1 == 0).head._2.compile.toVector.unsafeRunSync shouldBe (List(2, 4, 6, 8, 10))
    streams.filter(_._1 == 1).head._2.compile.toVector.unsafeRunSync shouldBe (List(1, 3, 5, 7, 9))
  }

  it should "fail on exception" in {
    val streams = Stream
      .emits((1 to 10))
      .through(groupBy(i => IO(throw new Exception())))
      .attempt
      .compile
      .toVector
      .unsafeRunSync

    streams.size shouldBe (1)
    streams.head.isLeft shouldBe (true)
  }
}
