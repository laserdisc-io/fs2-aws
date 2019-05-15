package fs2.aws.sqs

import fs2.aws.internal.{SqsClient, SqsClientImpl}
import fs2.{Pipe, Sink, Stream}
import fs2.concurrent.Queue
import cats.effect.{Async, Concurrent, Timer}
import software.amazon.awssdk.services.sqs.model._

import scala.concurrent.ExecutionContext

object consumer {
  def readObjectFromSqs[F[_]: Concurrent, A](sqsConfig: SqsConfig,
                                             sqsClient: SqsClient[F] = new SqsClientImpl[F])(
      implicit ec: ExecutionContext,
      Timer: Timer[F],
      decoder: Message => Either[Throwable, A]): Stream[F, Either[Throwable, A]] = {

    def receiveMessageStream(buffer: Queue[F, Either[Throwable, A]]) =
      Stream
        .eval(Timer.sleep(sqsConfig.pollRate))
        .repeat
        .zipRight(
          Stream.eval(sqsClient.fetchMessages(sqsConfig.queueUrl, sqsConfig.fetchMessageCount)))
        .flatMap(list => Stream.emits(list.map(decoder)))
        .evalMap(msg => buffer.enqueue1(msg))

    for {
      buffer <- Stream.eval(Queue.bounded[F, Either[Throwable, A]](sqsConfig.bufferSize))
      stream <- buffer.dequeue concurrently receiveMessageStream(buffer)
    } yield stream
  }

  def readFromSqs[F[_]: Concurrent](sqsConfig: SqsConfig,
                                    sqsClient: SqsClient[F] = new SqsClientImpl[F])(
      implicit ec: ExecutionContext,
      Timer: Timer[F]): Stream[F, Message] =
    readObjectFromSqs[F, Message](sqsConfig, sqsClient)(Concurrent[F],
                                                        ec,
                                                        Timer,
                                                        msg => Right(msg))
      .map(_.right.get)

  def deleteFromSqs[F[_]: Async](queueUrl: String, sqsClient: SqsClient[F] = new SqsClientImpl[F])(
      implicit ec: ExecutionContext): Pipe[F, Message, DeleteMessageResponse] = {

    _.flatMap { msg =>
      Stream.eval(sqsClient.deleteMessage(queueUrl, msg))
    }
  }

  def deleteFromSqs_[F[_]: Async](queueUrl: String)(
      implicit ec: ExecutionContext): Sink[F, Message] =
    _.through(deleteFromSqs(queueUrl)).drain

  def sendToSqs[F[_]: Async](queueUrl: String, sqsClient: SqsClient[F] = new SqsClientImpl[F])(
      implicit ec: ExecutionContext): Pipe[F, Message, SendMessageResponse] =
    _.flatMap(msg => Stream.eval(sqsClient.sendMessage(queueUrl, msg)))

}
