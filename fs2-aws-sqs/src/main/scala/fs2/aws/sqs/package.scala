package fs2.aws

import cats.effect.{ Async, Concurrent, Timer }
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model._

import java.util.concurrent.{ CompletableFuture, CompletionException }
import scala.jdk.CollectionConverters._
package object sqs {
  type MsgBody = String

  def sqsStream[F[_]: Concurrent: Timer](
    sqsConfig: SqsConfig,
    sqsClient: SqsAsyncClient
  ): fs2.Stream[F, Message] =
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

  def deleteFromSqs[F[_]: Async](
    queueUrl: String,
    sqsClient: SqsAsyncClient
  ): fs2.Pipe[F, Message, DeleteMessageResponse] =
    _.flatMap(msg =>
      fs2.Stream.eval(
        eff(
          sqsClient.deleteMessage(
            DeleteMessageRequest
              .builder()
              .queueUrl(queueUrl)
              .receiptHandle(msg.receiptHandle())
              .build()
          )
        )
      )
    )

  def sendToSqs[F[_]: Async](
    queueUrl: String,
    sqsClient: SqsAsyncClient
  ): fs2.Pipe[F, MsgBody, SendMessageResponse] =
    _.flatMap(msg =>
      fs2.Stream.eval(
        eff(
          sqsClient
            .sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(msg).build())
        )
      )
    )

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
