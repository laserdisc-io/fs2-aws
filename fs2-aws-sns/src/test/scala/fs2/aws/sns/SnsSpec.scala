package fs2.aws.sns

import cats.effect.{ ContextShift, IO, Timer }
import fs2.aws.sns.sns.SNS
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model._
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model._

import java.net.URI
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.matching.Regex

class SnsSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  import fs2.aws.helper._

  implicit val ec: ExecutionContext             = ExecutionContext.global
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)
  implicit val ioTimer: Timer[IO]               = IO.timer(ec)

  val snsClient: SnsAsyncClient = mkSNSClient(4566)
  val sqsClient: SqsAsyncClient = mkSQSClient(4566)
  var topicArn: String          = _
  var queueUrl: String          = _
  val pattern: Regex            = new Regex("\"Message\": \"[0-9]\"")

  override def beforeAll(): Unit =
    (for {
      topic <- CompletableFutureLift
                .eff[IO, CreateTopicResponse](
                  snsClient.createTopic(CreateTopicRequest.builder().name("topic").build())
                )
                .map(_.topicArn())

      queueUrlV <- CompletableFutureLift
                    .eff[IO, CreateQueueResponse](
                      sqsClient.createQueue(CreateQueueRequest.builder().queueName("names").build())
                    )
                    .map(_.queueUrl())
    } yield {
      topicArn = topic
      queueUrl = queueUrlV
    }).unsafeRunSync()

  override def afterAll(): Unit =
    (for {
      _ <- CompletableFutureLift
            .eff[IO, DeleteTopicResponse](
              snsClient.deleteTopic(DeleteTopicRequest.builder().topicArn(topicArn).build())
            )
      _ <- CompletableFutureLift
            .eff[IO, DeleteQueueResponse](
              sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build())
            )
    } yield {}).unsafeRunSync()

  "SNS" should {
    "publish messages" in {
      val messages = (for {
        sns <- fs2.Stream.eval(SNS.create[IO](snsClient))
        sqsArn <- fs2.Stream.eval(
                   CompletableFutureLift.eff[IO, GetQueueAttributesResponse](
                     sqsClient
                       .getQueueAttributes(
                         GetQueueAttributesRequest
                           .builder()
                           .queueUrl(queueUrl)
                           .attributeNames(QueueAttributeName.QUEUE_ARN)
                           .build()
                       )
                   )
                 )

        _ <- fs2.Stream.eval(
              CompletableFutureLift.eff[IO, SubscribeResponse](
                snsClient.subscribe(
                  SubscribeRequest
                    .builder()
                    .protocol("sqs")
                    .endpoint(sqsArn.attributes().get(QueueAttributeName.QUEUE_ARN))
                    .topicArn(topicArn)
                    .build()
                )
              )
            )
        _ <- fs2
              .Stream("1", "2", "3", "4", "5")
              .covary[IO]
              .through(sns.publish(topicArn))

        sqsMessages <- fs2.Stream.eval(
                        CompletableFutureLift.eff[IO, ReceiveMessageResponse](
                          sqsClient
                            .receiveMessage(
                              ReceiveMessageRequest
                                .builder()
                                .queueUrl(queueUrl)
                                .build()
                            )
                        )
                      ).delayBy(3.seconds)
      } yield {
        sqsMessages
          .messages()
          .asScala
          .map(x => pattern.findAllIn(x.body()).mkString(","))
          .map(x => new Regex("\\d").findAllIn(x).mkString(","))
      }).take(5)
        .compile
        .toList
        .unsafeRunSync()

      messages.flatten should contain theSameElementsAs (List("1", "2", "3", "4", "5"))
    }
  }

  def mkSNSClient(snsPort: Int) = {
    val credentials =
      AwsBasicCredentials.create("accesskey", "secretkey")

    SnsAsyncClient
      .builder()
      .credentialsProvider(StaticCredentialsProvider.create(credentials))
      .endpointOverride(URI.create(s"http://localhost:$snsPort"))
      .region(Region.US_EAST_1)
      .build()
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
