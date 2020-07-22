package fs2.aws.sqs

import cats.effect.ConcurrentEffect
import cats.effect.concurrent.Deferred
import eu.timepit.refined.auto._
import javax.jms.{ Message, MessageListener }

package object testkit {
  def sqsStream[F[_]: ConcurrentEffect, O](
    d: Deferred[F, MessageListener]
  )(implicit decoder: Message => Either[Throwable, O]): fs2.Stream[F, O] =
    fs2.aws.sqsStream(
      SqsConfig("dummy"),
      (_: SqsConfig, _: MessageListener) => new TestSqsConsumerBuilder[F],
      Some(d)
    )

}
