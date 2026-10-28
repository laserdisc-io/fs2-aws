package fs2.aws.sqs

import cats.effect.Async
import cats.syntax.applicative.*
import fs2.{Chunk, Pipe, Stream}
import io.laserdisc.pure.sqs.tagless.SqsAsyncClientOp
import software.amazon.awssdk.services.sqs.model.*

import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

trait SQS[F[_]] {
  def sqsStream: Stream[F, Message]

  /** One chunk per non-empty receive. Its messages' visibility timeouts all started at that receive, so extend or delete them
    * as a batch when handling can be slow. The default chunks `sqsStream`; `SQS.create` overrides it.
    */
  def sqsStreamBatched: Stream[F, Chunk[Message]] = sqsStream.chunks

  /** Sets each message's visibility timeout once, as it passes through, counting from that call. It isn't a heartbeat: to keep
    * messages hidden during a long handler, repeat it for every message still in flight. The timeout is rounded up to whole
    * seconds, and zero makes the message visible again immediately.
    */
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
    createWithReceiveRequest(sqsConfig, sqs, identity[ReceiveMessageRequest.Builder])

  /** Like `create`, with `receiveRequest` applied to every receive request: long polling (`_.waitTimeSeconds(20)`), a
    * per-receive visibility timeout, or attributes such as `ApproximateReceiveCount`. The queue URL and message count always
    * come from `sqsConfig`. It has its own name, not a `create` overload, so Scala 2.13 can infer the lambda's type.
    */
  def createWithReceiveRequest[F[_]: Async](
      sqsConfig: SqsConfig,
      sqs: SqsAsyncClientOp[F],
      receiveRequest: ReceiveMessageRequest.Builder => ReceiveMessageRequest.Builder
  ): F[SQS[F]] =
    new SQS[F] {
      override def sqsStream: Stream[F, Message] = sqsStreamBatched.unchunks

      override def sqsStreamBatched: Stream[F, Chunk[Message]] =
        Stream
          .awakeEvery[F](sqsConfig.pollRate)
          .evalMap(_ =>
            sqs
              .receiveMessage(
                receiveRequest(ReceiveMessageRequest.builder())
                  .queueUrl(sqsConfig.queueUrl)
                  .maxNumberOfMessages(sqsConfig.fetchMessageCount)
                  .build()
              )
          )
          .map(response => Chunk.from(response.messages().asScala))
          .filter(_.nonEmpty)

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
                    .visibilityTimeout(roundUpToSeconds(timeout))
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

  // SQS takes whole seconds. Round up, as truncating e.g. 500.millis to 0 would release the message at once.
  private def roundUpToSeconds(timeout: FiniteDuration): Int = {
    val whole = timeout.toSeconds
    (if (timeout > whole.seconds) whole + 1 else whole).toInt
  }
}
