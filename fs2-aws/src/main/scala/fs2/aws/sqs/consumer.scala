package fs2.aws.sqs

import fs2.{Stream, Pipe, Sink}
import fs2.concurrent.Queue
import cats.effect.Concurrent
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model._

import scala.util.{Success, Failure}
import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

object consumer {
  def readObjectFromSqs[F[_], A](queueUrl: String)(
      implicit ec: ExecutionContext,
      F: Concurrent[F],
      decoder: Message => Either[Throwable, A]): Stream[F, Either[Throwable, A]] = {
    val client = SqsAsyncClient.create()

    def receiveMessageStream =
      Stream
        .eval(
          F.async[List[Message]] { cb =>
            client.receiveMessage(buildReceiveRequest(queueUrl, 5)).toScala.onComplete {
              case Success(response) => cb(Right(response.messages().asScala.toList))
              case Failure(err)      => cb(Left(err))
            }
          }
        )
        .flatMap(list => Stream.emits(list.map(decoder)))

    for {
      buffer <- Stream.eval(Queue.bounded[F, Either[Throwable, A]](10))
      stream <- buffer.dequeue concurrently receiveMessageStream
    } yield stream
  }

  def readFromSqs[F[_]](queueUrl: String)(implicit ec: ExecutionContext,
                                          F: Concurrent[F]): Stream[F, Message] =
    readObjectFromSqs[F, Message](queueUrl)(ec, F, msg => Right(msg)).map(_.right.get)

  def deleteFromSqs[F[_]](queueUrl: String)(
      implicit ec: ExecutionContext,
      F: Concurrent[F]): Pipe[F, Message, DeleteMessageResponse] = {
    val client = SqsAsyncClient.create()

    _.flatMap { msg =>
      Stream.eval(
        F.async[DeleteMessageResponse] { cb =>
          client.deleteMessage(buildDeleteRequest(queueUrl, msg)).toScala.onComplete {
            case Success(response) => cb(Right(response))
            case Failure(err)      => cb(Left(err))
          }
        }
      )
    }

  }

  def deleteFromSqs_[F[_]](queueUrl: String)(implicit ec: ExecutionContext,
                                             F: Concurrent[F]): Sink[F, Message] =
    _.through(deleteFromSqs(queueUrl)).drain

  private[this] def buildReceiveRequest(url: String, numMessages: Int): ReceiveMessageRequest = {
    ReceiveMessageRequest.builder().queueUrl(url).maxNumberOfMessages(numMessages).build()
  }

  private[this] def buildDeleteRequest(url: String, message: Message): DeleteMessageRequest = {
    DeleteMessageRequest.builder().queueUrl(url).receiptHandle(message.receiptHandle()).build()
  }
}
