package fs2.aws.sqs

import cats.effect.{ ContextShift, IO, Timer }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{ mock, when }
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{
  Message,
  ReceiveMessageRequest,
  ReceiveMessageResponse,
  SendMessageRequest,
  SendMessageResponse
}

import java.util.concurrent.CompletableFuture
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class SqsSpec extends AsyncFlatSpec with Matchers {
  implicit val ec: ExecutionContext             = ExecutionContext.global
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)
  implicit val ioTimer: Timer[IO]               = IO.timer(ec)
  implicit val messageDecoder: Message => Either[Throwable, Int] = { sqs_msg =>
    val text = sqs_msg.body()
    if ("fail" == text) Left(new Exception("failure"))
    else Right(text.toInt)
  }
  it should "stream messages" in {
    val sqs = mock(classOf[SqsAsyncClient])
    when(sqs.receiveMessage(any[ReceiveMessageRequest]))
      .thenReturn(
        CompletableFuture
          .completedFuture(
            ReceiveMessageResponse.builder().messages(Message.builder().body("1").build()).build()
          )
      )
      .thenReturn(
        CompletableFuture
          .completedFuture(
            ReceiveMessageResponse.builder().messages(Message.builder().body("2").build()).build()
          )
      )
      .thenReturn(
        CompletableFuture
          .completedFuture(
            ReceiveMessageResponse
              .builder()
              .messages(Message.builder().body("fail").build())
              .build()
          )
      )
      .thenReturn(
        CompletableFuture
          .completedFuture(
            ReceiveMessageResponse.builder().messages(Message.builder().body("4").build()).build()
          )
      )
      .thenReturn(
        CompletableFuture
          .completedFuture(
            ReceiveMessageResponse
              .builder()
              .messages(Message.builder().body("5").build())
              .build()
          )
      )
    val r = sqsStream[IO](
      SqsConfig(queueUrl = "dummy", pollRate = 10 milliseconds),
      sqs
    ).take(5)
      .compile
      .toList
      .unsafeRunSync()

    r should be(
      List(
        Message.builder().body("1").build(),
        Message.builder().body("2").build(),
        Message.builder().body("fail").build(),
        Message.builder().body("4").build(),
        Message.builder().body("5").build()
      )
    )
  }
  "messages" should "be sent to SQS endpoint" in {
    val msgs = List("1", "2", "3", "4", "5")

    val sqs = mock(classOf[SqsAsyncClient])
    when(sqs.sendMessage(any[SendMessageRequest])).thenReturn(
      CompletableFuture.completedFuture(SendMessageResponse.builder().messageId("123").build())
    )
    val r = sendToSqs[IO]("", sqsClient = sqs)

    val res = r(fs2.Stream.emits(msgs)).compile.toList.unsafeRunSync()

    res.length should be(5)
  }
}
