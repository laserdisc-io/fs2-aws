package fs2.aws.sqs

import cats.effect.Async
import cats.syntax.applicative.*
import fs2.{Pipe, Stream}
import io.laserdisc.pure.sqs.tagless.SqsAsyncClientOp
import software.amazon.awssdk.services.sqs.model.*

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.*

trait SQS[F[_]] {
  def sqsStream: Stream[F, Message]

  def changeMessageVisibilityPipe(timeout: FiniteDuration): Pipe[F, Message, Message]

  def deleteMessagePipe: Pipe[F, Message, DeleteMessageResponse]

  def sendMessagePipe: Pipe[F, SQS.MsgBody, SendMessageResponse]
}

object SQS {
  type MsgBody = String

  def create[F[_]: Async](
      sqsConfig: SqsConfig,
      sqs: SqsAsyncClientOp[F]
  ): F[SQS[F]] =
    new SQS[F] {
      override def sqsStream: fs2.Stream[F, Message] =
        Stream
          .awakeEvery[F](sqsConfig.pollRate)
          .evalMap(_ =>
            sqs
              .receiveMessage(
                ReceiveMessageRequest
                  .builder()
                  .queueUrl(sqsConfig.queueUrl)
                  .maxNumberOfMessages(sqsConfig.fetchMessageCount)
                  .build()
              )
          )
          .flatMap(response => Stream.emits(response.messages().asScala))

      def changeMessageVisibilityPipe(timeout: FiniteDuration): Pipe[F, Message, Message] =
        _.flatMap(msg =>
          Stream
            .eval(
              sqs
                .changeMessageVisibility(
                  ChangeMessageVisibilityRequest
                    .builder()
                    .queueUrl(sqsConfig.queueUrl)
                    .receiptHandle(msg.receiptHandle())
                    .visibilityTimeout(timeout.toSeconds.toInt)
                    .build()
                )
            )
            .as(msg)
        )

      override def deleteMessagePipe: Pipe[F, Message, DeleteMessageResponse] =
        _.flatMap(msg =>
          Stream.eval(
            sqs
              .deleteMessage(
                DeleteMessageRequest
                  .builder()
                  .queueUrl(sqsConfig.queueUrl)
                  .receiptHandle(msg.receiptHandle())
                  .build()
              )
          )
        )

      override def sendMessagePipe: Pipe[F, MsgBody, SendMessageResponse] =
        _.flatMap(msg =>
          Stream.eval(
            sqs
              .sendMessage(
                SendMessageRequest.builder().queueUrl(sqsConfig.queueUrl).messageBody(msg).build()
              )
          )
        )
    }.pure[F]

}
