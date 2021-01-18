package fs2.aws.sqs

import cats.effect.{ Async, Timer }
import fs2.Pipe
import fs2.aws.sqs.SQS.MsgBody
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model._
import sqs.SqsConfig

import java.util.concurrent.{ CompletableFuture, CompletionException }
import scala.jdk.CollectionConverters._
import cats.syntax.applicative._

trait SQS[F[_]] {
  def sqsStream: fs2.Stream[F, Message]

  def deleteMessagePipe: fs2.Pipe[F, Message, DeleteMessageResponse]

  def sendMessagePipe: fs2.Pipe[F, MsgBody, SendMessageResponse]
}

object SQS {
  type MsgBody = String
  def create[F[_]: Async: Timer](
    sqsConfig: SqsConfig,
    sqsClient: SqsAsyncClient
  ): F[SQS[F]] =
    new SQS[F] {
      override def sqsStream: fs2.Stream[F, Message] =
        fs2.Stream
          .awakeEvery[F](sqsConfig.pollRate)
          .evalMap(_ =>
            eff[F, ReceiveMessageResponse](
              sqsClient.receiveMessage(
                ReceiveMessageRequest
                  .builder()
                  .queueUrl(sqsConfig.queueUrl)
                  .maxNumberOfMessages(sqsConfig.fetchMessageCount)
                  .build()
              )
            )
          )
          .flatMap(response => fs2.Stream.emits(response.messages().asScala))

      override def deleteMessagePipe: Pipe[F, Message, DeleteMessageResponse] =
        _.flatMap(msg =>
          fs2.Stream.eval(
            eff(
              sqsClient.deleteMessage(
                DeleteMessageRequest
                  .builder()
                  .queueUrl(sqsConfig.queueUrl)
                  .receiptHandle(msg.receiptHandle())
                  .build()
              )
            )
          )
        )

      override def sendMessagePipe: Pipe[F, MsgBody, SendMessageResponse] =
        _.flatMap(msg =>
          fs2.Stream.eval(
            eff(
              sqsClient
                .sendMessage(
                  SendMessageRequest.builder().queueUrl(sqsConfig.queueUrl).messageBody(msg).build()
                )
            )
          )
        )
    }.pure[F]

  def eff[F[_]: Async, A <: AnyRef](fut: =>CompletableFuture[A]): F[A] =
    Async[F].async { cb =>
      fut.handle[Unit] { (a, x) =>
        if (a eq null)
          x match {
            case t: CompletionException => cb(Left(t.getCause))
            case t                      => cb(Left(t))
          }
        else
          cb(Right(a))
      }
      ()
    }
}
