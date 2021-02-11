package fs2.aws.sqs

import cats.effect.{ Async, ContextShift, Timer }
import cats.syntax.applicative._
import fs2.Pipe
import fs2.aws.sqs.SQS.MsgBody
import io.laserdisc.pure.sqs.tagless.SqsAsyncClientOp
import software.amazon.awssdk.services.sqs.model._
import sqs.SqsConfig

import scala.jdk.CollectionConverters._

trait SQS[F[_]] {
  def sqsStream: fs2.Stream[F, Message]

  def deleteMessagePipe: fs2.Pipe[F, Message, DeleteMessageResponse]

  def sendMessagePipe: fs2.Pipe[F, MsgBody, SendMessageResponse]
}

object SQS {
  type MsgBody = String
  def create[F[_]: Async: Timer: ContextShift](
    sqsConfig: SqsConfig,
    sqs: SqsAsyncClientOp[F]
  ): F[SQS[F]] =
    new SQS[F] {
      override def sqsStream: fs2.Stream[F, Message] =
        fs2.Stream
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
          .flatMap(response => fs2.Stream.emits(response.messages().asScala))

      override def deleteMessagePipe: Pipe[F, Message, DeleteMessageResponse] =
        _.flatMap(msg =>
          fs2.Stream.eval(
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
          fs2.Stream.eval(
            sqs
              .sendMessage(
                SendMessageRequest.builder().queueUrl(sqsConfig.queueUrl).messageBody(msg).build()
              )
          )
        )
    }.pure[F]

}
