package fs2.aws.internal

import cats.effect.Async
import software.amazon.awssdk.services.sqs.{SqsAsyncClient => SDKClient}
import software.amazon.awssdk.services.sqs.model._

import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext
import scala.compat.java8.FutureConverters._
import scala.collection.JavaConverters._

trait SqsClient[F[_]] {
  def fetchMessages(url: String, n: Int)(implicit ec: ExecutionContext,
                                         F: Async[F]): F[List[Message]]
  def sendMessage(url: String, message: Message)(implicit ec: ExecutionContext,
                                                 F: Async[F]): F[SendMessageResponse]
  def deleteMessage(url: String, message: Message)(implicit ec: ExecutionContext,
                                                   F: Async[F]): F[DeleteMessageResponse]
  def buildReceiveRequest(url: String, numMessages: Int): ReceiveMessageRequest
  def buildSendMessageRequest(url: String, message: Message): SendMessageRequest
  def buildDeleteRequest(url: String, message: Message): DeleteMessageRequest
}

class SqsClientImpl[F[_]] extends SqsClient[F] {
  private val client: SDKClient = SDKClient.create()

  override def fetchMessages(url: String, n: Int)(implicit ec: ExecutionContext,
                                                  F: Async[F]): F[List[Message]] =
    F.async[List[Message]] { cb =>
      client.receiveMessage(buildReceiveRequest(url, n)).toScala.onComplete {
        case Success(response) => cb(Right(response.messages().asScala.toList))
        case Failure(err)      => cb(Left(err))
      }
    }

  override def sendMessage(url: String, message: Message)(implicit ec: ExecutionContext,
                                                          F: Async[F]): F[SendMessageResponse] =
    F.async[SendMessageResponse] { cb =>
      client.sendMessage(buildSendMessageRequest(url, message)).toScala.onComplete {
        case Success(response) => cb(Right(response))
        case Failure(err)      => cb(Left(err))
      }
    }

  override def deleteMessage(url: String, message: Message)(
      implicit ec: ExecutionContext,
      F: Async[F]): F[DeleteMessageResponse] =
    F.async[DeleteMessageResponse] { cb =>
      client.deleteMessage(buildDeleteRequest(url, message)).toScala.onComplete {
        case Success(response) => cb(Right(response))
        case Failure(err)      => cb(Left(err))
      }
    }

  override def buildReceiveRequest(url: String, numMessages: Int): ReceiveMessageRequest =
    ReceiveMessageRequest.builder().queueUrl(url).maxNumberOfMessages(numMessages).build()

  override def buildSendMessageRequest(url: String, message: Message): SendMessageRequest =
    SendMessageRequest.builder().queueUrl(url).messageBody(message.body()).build()

  override def buildDeleteRequest(url: String, message: Message): DeleteMessageRequest =
    DeleteMessageRequest.builder().queueUrl(url).receiptHandle(message.receiptHandle()).build()
}
