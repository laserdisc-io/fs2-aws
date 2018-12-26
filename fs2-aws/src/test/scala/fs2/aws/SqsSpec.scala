package fs2.aws

import fs2.aws.sqs.SqsConfig
import fs2.aws.sqs.consumer.readObjectFromSqs
import cats.effect.{ContextShift, IO, Timer}
import software.amazon.awssdk.services.sqs.model.Message

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.concurrent.ExecutionContext

class SqsSpec extends AsyncFlatSpec with Matchers with MockitoSugar {
  implicit val ec: ExecutionContext             = ExecutionContext.global
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)
  implicit val ioTimer: Timer[IO]               = IO.timer(ec)
  implicit val messageDecoder: Message => Either[Throwable, Int] = { sqs_msg =>
    val text = sqs_msg.body()
    if ("fail" == text) Left(new Exception("failure"))
    else Right(text.toInt)
  }

  "SQS endpoint" should "stream messages" in {
    val msgs = List("1", "2", "fail", "4", "5")

    val r =
      readObjectFromSqs[IO, Int](sqsConfig = SqsConfig(""), sqsClient = new TestSqsClient[IO](msgs))
        .take(5)
        .compile
        .toList

    val future = r.unsafeToFuture()
    future.map(
      _.filter(_.isRight) should be(
        List(
          Right(1),
          Right(2),
          Right(4),
          Right(5)
        )
      )
    )
  }
}
