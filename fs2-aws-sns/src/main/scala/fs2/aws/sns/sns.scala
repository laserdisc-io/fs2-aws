package fs2.aws.sns

import cats.effect.*
import cats.implicits.*
import eu.timepit.refined.auto.*
import eu.timepit.refined.types.numeric.PosInt
import software.amazon.awssdk.services.sns.model.{PublishRequest, PublishResponse}
import fs2.Pipe
import io.laserdisc.pure.sns.tagless.SnsAsyncClientOp

object sns {
  type MsgBody = String

  final case class SnsSettings(concurrency: PosInt = PosInt.unsafeFrom(10))

  trait SNS[F[_]] {
    def publish(topicArn: String): Pipe[F, MsgBody, PublishResponse]
  }

  object SNS {

    def create[F[_]: Concurrent: Async](
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
