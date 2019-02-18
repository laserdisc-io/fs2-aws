package fs2.aws.testkit

import cats.effect.Async
import fs2.aws.internal.SqsClient
import software.amazon.awssdk.services.sqs.model._

import scala.concurrent.ExecutionContext

class TestSqsClient[F[_]](msgs: List[String]) extends SqsClient[F] {
  override def fetchMessages(url: String, n: Int)(implicit ec: ExecutionContext,
                                                  F: Async[F]): F[List[Message]] =
    F.delay(msgs.map(data => Message.builder().body(data).build()))

  override def deleteMessage(url: String, message: Message)(
      implicit ec: ExecutionContext,
      F: Async[F]): F[DeleteMessageResponse] =
    F.delay(DeleteMessageResponse.builder().build())

  override def buildReceiveRequest(url: String, numMessages: Int): ReceiveMessageRequest =
    ReceiveMessageRequest.builder().build()

  override def buildDeleteRequest(url: String, message: Message): DeleteMessageRequest =
    DeleteMessageRequest.builder().build()

  override def sendMessage(url: String, message: Message)(implicit ec: ExecutionContext,
                                                          F: Async[F]): F[SendMessageResponse] =
    F.delay(SendMessageResponse.builder().build())

  override def buildSendMessageRequest(url: String, message: Message): SendMessageRequest =
    SendMessageRequest.builder().build()
}
