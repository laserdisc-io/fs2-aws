package fs2.aws

import fs2.Stream
import cats.effect.{Concurrent, Timer}
import software.amazon.awssdk.services.sqs.model.Message

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

package object testkit {
  def sqsStream[F[_]: Concurrent, A](msgs: List[String])(
      implicit ec: ExecutionContext,
      Timer: Timer[F],
      decoder: Message => Either[Throwable, A]): Stream[F, Either[Throwable, A]] = {
    fs2.aws.sqs.consumer.readObjectFromSqs("", 5.seconds, new TestSqsClient[F](msgs))
  }
}
