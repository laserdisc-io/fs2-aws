package fs2.aws.sns

import cats.effect._
import cats.implicits._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.Positive
import software.amazon.awssdk.services.sns.model.{ PublishRequest, PublishResponse }
import fs2.Pipe
import io.laserdisc.pure.sns.tagless.SnsAsyncClientOp
object sns {
  type MsgBody     = String
  type PositiveInt = Int Refined Positive

  final case class SnsSettings(concurrency: PositiveInt = 10)

  trait SNS[F[_]] {
    def publish(topicArn: String): Pipe[F, MsgBody, PublishResponse]
  }

  object SNS {

    def create[F[_]: Concurrent: Async: Timer](
      sns: SnsAsyncClientOp[F],
      settings: SnsSettings = SnsSettings()
    ): F[SNS[F]] =
      new SNS[F] {
        override def publish(topicArn: String): Pipe[F, MsgBody, PublishResponse] =
          _.mapAsyncUnordered(settings.concurrency)(msg =>
            sns.publish(PublishRequest.builder().message(msg).topicArn(topicArn).build())
          )
      }.pure[F]
  }
}
