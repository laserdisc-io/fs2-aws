package fs2.aws

import fs2.aws.sqs.SqsConfig
import fs2.Stream
import cats.effect.{Concurrent, Timer}
import software.amazon.awssdk.services.sqs.model.Message

import scala.concurrent.ExecutionContext

package object testkit {
  def sqsStream[F[_]: Concurrent, A](msgs: List[String])(
      implicit ec: ExecutionContext,
      Timer: Timer[F],
      decoder: Message => Either[Throwable, A]): Stream[F, Either[Throwable, A]] = {
    val config = SqsConfig("")
    fs2.aws.sqs.consumer.readObjectFromSqs(config, new TestSqsClient[F](msgs))
  }
}
