package fs2.aws

import cats.effect.concurrent.Deferred
import cats.effect.{ ContextShift, IO }
import com.amazon.sqs.javamessaging.SQSConnection
import com.amazon.sqs.javamessaging.message.SQSTextMessage
import eu.timepit.refined.auto._
import fs2.aws
import fs2.aws.sqs.{ ConsumerBuilder, SQSConsumer, SqsConfig }
import javax.jms.{ Message, MessageListener, TextMessage }
import org.mockito.scalatest.MockitoSugar
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class SqsSpec extends AsyncFlatSpec with Matchers with MockitoSugar {
  implicit val ec: ExecutionContext             = ExecutionContext.global
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)
  implicit val messageDecoder: Message => Either[Throwable, Int] = { sqs_msg =>
    val text = sqs_msg.asInstanceOf[TextMessage].getText
    if ("fail" == text) Left(intercept[Exception](()))
    else Right(text.toInt)
  }
  "SQS endpoint" should "stream messages" in {

    def stream(d: Deferred[IO, MessageListener]) =
      aws
        .sqsStream[IO, Int](
          SqsConfig("dummy"),
          (_, listener) =>
            new ConsumerBuilder[IO] {
              override def start: IO[SQSConsumer] =
                IO.delay(new SQSConsumer {
                  override def callback: MessageListener = listener

                  override def startConsumer(): Unit = ()

                  override def shutdown(): Unit = ()

                  override def connection: SQSConnection = mock[SQSConnection]
                })
            },
          Some(d)
        )
        .take(4)
        .compile
        .toList

    val r = for {
      d <- Deferred[IO, MessageListener]
      res <- IO.racePair(stream(d), d.get).flatMap {
              case Right((streamFiber, listener)) =>
                listener.onMessage(new SQSTextMessage("1"))
                listener.onMessage(new SQSTextMessage("2"))
                listener.onMessage(new SQSTextMessage("fail"))
                listener.onMessage(new SQSTextMessage("4"))
                listener.onMessage(new SQSTextMessage("5"))
                streamFiber.join
              case _ => IO(Nil)
            }
    } yield res

    val future = r.unsafeToFuture()

    future.map(_ should be(List(1, 2, 4, 5)))
  }
}
