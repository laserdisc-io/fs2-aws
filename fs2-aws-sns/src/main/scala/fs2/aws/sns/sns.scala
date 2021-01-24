package fs2.aws.sns

import cats.effect._
import cats.implicits._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.Positive
import fs2._
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.{ PublishRequest, PublishResponse }

object sns {
  type MsgBody     = String
  type PositiveInt = Int Refined Positive

  final case class SnsSettings(concurrency: PositiveInt = 10)

  trait SNS[F[_]] {
    def publish(topicArn: String): Pipe[F, MsgBody, PublishResponse]
  }

  object SNS {

    import fs2.aws.helper.CompletableFutureLift._

    def create[F[_]: Concurrent: Async: Timer](
      snsClient: SnsAsyncClient,
      settings: SnsSettings = SnsSettings()
    ): F[SNS[F]] =
      new SNS[F] {
        override def publish(topicArn: String): Pipe[F, MsgBody, PublishResponse] =
          _.mapAsyncUnordered(settings.concurrency)(msg =>
            eff(
              snsClient
                .publish(PublishRequest.builder().message(msg).topicArn(topicArn).build())
            )
          )
      }.pure[F]
  }
}
