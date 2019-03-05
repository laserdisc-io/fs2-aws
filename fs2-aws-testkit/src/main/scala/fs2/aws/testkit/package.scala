package fs2.aws

import cats.effect.ConcurrentEffect
import cats.effect.concurrent.Deferred
import eu.timepit.refined.auto._
import fs2.aws.sqs.SqsConfig
import javax.jms.{Message, MessageListener}
import scala.concurrent.duration._

package object testkit {
  def sqsStream[F[_]: ConcurrentEffect, O](
      d: Deferred[F, MessageListener]
  )(implicit decoder: Message => Either[Throwable, O]): fs2.Stream[F, O] = {
    fs2.aws.sqsStream(SqsConfig("dummy"),
                      (_: SqsConfig, _: MessageListener) => new TestSqsConsumerBuilder[F],
                      Some(d))
  }

  val TestRecordProcessor = new kinesis.SingleRecordProcessor(_ => (), 10.seconds)
}
