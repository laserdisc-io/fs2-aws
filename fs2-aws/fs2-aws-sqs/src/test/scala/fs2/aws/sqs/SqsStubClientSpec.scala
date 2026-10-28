package fs2.aws.sqs

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import fs2.{Pipe, Stream}
import io.laserdisc.pure.sqs.tagless.SqsAsyncClientOp
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import software.amazon.awssdk.services.sqs.model.*

import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

class SqsStubClientSpec extends AnyWordSpec with Matchers {

  implicit val runtime: IORuntime = IORuntime.global

  // Duration.Zero receives without pausing and the stubs repeat their last receive, so the streams below
  // are interrupted after a second: a regression that emits too little fails instead of hanging.
  val config: SqsConfig = SqsConfig(queueUrl = "queue-url", pollRate = Duration.Zero, fetchMessageCount = 7)

  "SQS" should {

    "emit one chunk per non-empty receive" in {
      val sqsOp   = stubReceives(List("a", "b", "c"), Nil, List("d", "e"))
      val batches =
        SQS
          .create[IO](config, sqsOp)
          .flatMap(_.sqsStreamBatched.take(2).interruptAfter(1.second).compile.toList)
          .unsafeRunSync()
      batches.map(_.toList.map(_.body())) should be(List(List("a", "b", "c"), List("d", "e")))
    }

    "keep the receive chunks in sqsStream" in {
      val sqsOp  = stubReceives(List("a", "b", "c"), Nil, List("d", "e"))
      val chunks = SQS
        .create[IO](config, sqsOp)
        .flatMap(_.sqsStream.take(5).interruptAfter(1.second).chunks.compile.toList)
        .unsafeRunSync()
      chunks.map(_.toList.map(_.body())) should be(List(List("a", "b", "c"), List("d", "e")))
    }

    "apply the receive request customizer, keeping the config's queue URL and message count" in {
      val sqsOp = stubReceives(List("a"))
      SQS
        .createWithReceiveRequest[IO](
          config,
          sqsOp,
          _.queueUrl("other-queue-url").maxNumberOfMessages(3).waitTimeSeconds(20)
        )
        .flatMap(_.sqsStream.take(1).interruptAfter(1.second).compile.drain)
        .unsafeRunSync()

      val captor: ArgumentCaptor[ReceiveMessageRequest] = ArgumentCaptor.forClass(classOf[ReceiveMessageRequest])
      verify(sqsOp).receiveMessage(captor.capture())
      captor.getValue.queueUrl() should be(config.queueUrl)
      captor.getValue.maxNumberOfMessages() should be(config.fetchMessageCount)
      captor.getValue.waitTimeSeconds() should be(20)
    }

    // implements only the members SQS had before sqsStreamBatched, so this stops compiling if it loses its default
    "default sqsStreamBatched to the chunks of sqsStream in other implementations" in {
      val custom = new SQS[IO] {
        def sqsStream: Stream[IO, Message] =
          Stream.emits(List(sqsMessage("a"), sqsMessage("b"))) ++ Stream.emit(sqsMessage("c"))
        def changeMessageVisibilityPipe(timeout: FiniteDuration): Pipe[IO, Message, Message] = ???
        def deleteMessagePipe: Pipe[IO, Message, DeleteMessageResponse]                      = ???
        def sendMessagePipe: Pipe[IO, SQS.MsgBody, SendMessageResponse]                      = ???
      }
      val batches = custom.sqsStreamBatched.compile.toList.unsafeRunSync()
      batches.map(_.toList.map(_.body())) should be(List(List("a", "b"), List("c")))
    }

    "round the visibility timeout up to whole seconds" in {
      val sqsOp = mock(classOf[SqsAsyncClientOp[IO]])
      when(sqsOp.changeMessageVisibility(any[ChangeMessageVisibilityRequest]()))
        .thenReturn(IO.pure(ChangeMessageVisibilityResponse.builder().build()))
      val timeouts = List(Duration.Zero, 1.nanosecond, 500.milliseconds, 1.second, 1500.milliseconds, 30.seconds)
      val msg      = Message.builder().receiptHandle("handle").build()

      SQS
        .create[IO](config, sqsOp)
        .flatMap(sqs =>
          Stream.emits(timeouts).flatMap(t => Stream(msg).through(sqs.changeMessageVisibilityPipe(t))).compile.drain
        )
        .unsafeRunSync()

      val captor: ArgumentCaptor[ChangeMessageVisibilityRequest] =
        ArgumentCaptor.forClass(classOf[ChangeMessageVisibilityRequest])
      verify(sqsOp, times(timeouts.size)).changeMessageVisibility(captor.capture())
      captor.getAllValues.asScala.toList.map(_.visibilityTimeout().intValue) should be(List(0, 1, 1, 1, 2, 30))
    }
  }

  def sqsMessage(body: String): Message = Message.builder().body(body).build()

  // each receive returns the next batch of bodies, repeating the last one
  def stubReceives(first: List[String], rest: List[String]*): SqsAsyncClientOp[IO] = {
    def response(bodies: List[String]): IO[ReceiveMessageResponse] =
      IO.pure(ReceiveMessageResponse.builder().messages(bodies.map(sqsMessage).asJava).build())

    val sqsOp = mock(classOf[SqsAsyncClientOp[IO]])
    when(sqsOp.receiveMessage(any[ReceiveMessageRequest]())).thenReturn(response(first), rest.map(response)*)
    sqsOp
  }
}
