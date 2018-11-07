package fs2.aws.sqs

import java.util.concurrent.CountDownLatch

import cats.effect.{ContextShift, IO}
import com.amazon.sqs.javamessaging.SQSConnection
import com.amazon.sqs.javamessaging.message.SQSTextMessage
import eu.timepit.refined.auto._
import fs2.aws
import javax.jms.{Message, MessageListener, TextMessage}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.concurrent.ExecutionContext

class SqsSpec extends AsyncFlatSpec with Matchers with MockitoSugar {
  implicit val ec: ExecutionContext             = ExecutionContext.global
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)
  var msgListener: MessageListener              = _
  implicit val messageDecoder: Message => Either[Throwable, Int] = { sqs_msg =>
    val text = sqs_msg.asInstanceOf[TextMessage].getText
    if ("fail" == text) Left(intercept[Exception](()))
    else Right(text.toInt)
  }
  "SQS endpoint" should "stream messages" in {

    val listenerReadiness = new CountDownLatch(1)
    val res = aws
      .sqsStream[IO, Int](
        SqsConfig("dummy"),
        (_, listener) => {
          msgListener = listener
          listenerReadiness.countDown()
          new ConsumerBuilder[IO] {
            override def start: IO[SQSConsumer] =
              IO.delay(new SQSConsumer {
                override def callback: MessageListener = listener

                override def startConsumer(): Unit = ()

                override def shutdown(): Unit = ()

                override def connection: SQSConnection = mock[SQSConnection]
              })
          }
        }
      )
      .take(4)
      .compile
      .toList

    val future = res.unsafeToFuture()

    listenerReadiness.await()
    msgListener.onMessage(new SQSTextMessage("1"))
    msgListener.onMessage(new SQSTextMessage("2"))
    msgListener.onMessage(new SQSTextMessage("fail"))
    msgListener.onMessage(new SQSTextMessage("4"))
    msgListener.onMessage(new SQSTextMessage("5"))

    future.map(_ should be(List(1, 2, 4, 5)))
  }
}
