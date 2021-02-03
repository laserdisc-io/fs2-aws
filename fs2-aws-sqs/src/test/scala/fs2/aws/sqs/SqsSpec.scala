package sqs

import cats.effect.{ Blocker, ContextShift, IO, Timer }
import fs2.aws.sqs.SQS
import io.laserdisc.pure.sqs.tagless.Interpreter
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model._

import java.net.URI
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class SqsSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {
  implicit val ec: ExecutionContext             = ExecutionContext.global
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)
  implicit val ioTimer: Timer[IO]               = IO.timer(ec)
  implicit val messageDecoder: Message => Either[Throwable, Int] = { sqs_msg =>
    val text = sqs_msg.body()
    if ("fail" == text) Left(new Exception("failure"))
    else Right(text.toInt)
  }
  val blocker                   = Blocker.liftExecutionContext(ec)
  val sqsClient: SqsAsyncClient = mkSQSClient(4566)
  val ip                        = Interpreter[IO](blocker).SqsAsyncClientInterpreter
  var queueUrl: String          = _

  override def beforeAll(): Unit =
    queueUrl = ip
      .createQueue(CreateQueueRequest.builder().queueName("names").build())
      .map(_.queueUrl())
      .run(sqsClient)
      .unsafeRunSync()

  // Delete the temp file
  override def afterAll(): Unit =
    ip.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build())
      .run(sqsClient)
      .unsafeRunSync()

  "SQS" should {
    "publish messages" in {
      (for {

        sqs <- fs2.Stream.eval(
                SQS
                  .create[IO](
                    SqsConfig(queueUrl = queueUrl, pollRate = 10 milliseconds),
                    Interpreter[IO](blocker).create(sqsClient)
                  )
              )
        sqsS <- fs2
                 .Stream("Barry", "Dmytro", "Ryan", "John", "Vlad")
                 .covary[IO]
                 .through(sqs.sendMessagePipe)
      } yield sqsS).compile.drain.unsafeRunSync()

    }

    "stream messages" in {
      val r = (for {
        sqs <- fs2.Stream.eval(
                SQS
                  .create[IO](
                    SqsConfig(
                      queueUrl = queueUrl,
                      pollRate = 10 milliseconds,
                      fetchMessageCount = 1
                    ),
                    Interpreter[IO](blocker).create(sqsClient)
                  )
              )
        sqsS <- sqs.sqsStream.map(_.body())
      } yield sqsS)
        .take(5)
        .compile
        .toList
        .unsafeRunSync()

      r should be(
        List(
          "Barry",
          "Dmytro",
          "Ryan",
          "John",
          "Vlad"
        )
      )
    }

  }

  def mkSQSClient(sqsPort: Int) = {
    val credentials =
      AwsBasicCredentials.create("accesskey", "secretkey")

    SqsAsyncClient
      .builder()
      .credentialsProvider(StaticCredentialsProvider.create(credentials))
      .endpointOverride(URI.create(s"http://localhost:$sqsPort"))
      .region(Region.US_EAST_1)
      .build()
  }
}
