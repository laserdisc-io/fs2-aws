package fs2.aws.examples

import cats.data.Kleisli
import cats.effect.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import io.laserdisc.pure.sns.tagless.{Interpreter as SNSInterpreter, SnsAsyncClientOp}
import io.laserdisc.pure.sqs.tagless.{Interpreter as SQSInterpreter, SqsAsyncClientOp}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.{
  CreateTopicRequest,
  DeleteTopicRequest,
  PublishRequest,
  SubscribeRequest
}
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.*

import java.net.URI
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*

case class Environment(sqs: SqsAsyncClient, sns: SnsAsyncClient)
object PureAWSKleisli extends IOApp {
  private val creds = AwsBasicCredentials.create("accesskey", "secretkey")
  private val port  = 4566
  override def run(args: List[String]): IO[ExitCode] =
    // Kleisli example
    resourcesK.use { e =>
      program[Kleisli[IO, Environment, *]](
        SQSInterpreter[IO].SqsAsyncClientInterpreter.lens[Environment](_.sqs),
        SNSInterpreter[IO].SnsAsyncClientInterpreter.lens[Environment](_.sns)
      ).run(e)
    } >> // TF example
      resourcesF.use { case (sqs, sns) => program[IO](sqs, sns) }

  def resourcesK: Resource[IO, Environment] =
    for {
      sns <- Resource.fromAutoCloseable(
        IO.delay(
          SnsAsyncClient
            .builder()
            .credentialsProvider(
              StaticCredentialsProvider.create(creds)
            )
            .endpointOverride(URI.create(s"http://localhost:$port"))
            .region(Region.US_EAST_1)
            .build()
        )
      )
      sqs <- Resource.fromAutoCloseable(
        IO.delay(
          SqsAsyncClient
            .builder()
            .credentialsProvider(StaticCredentialsProvider.create(creds))
            .endpointOverride(URI.create(s"http://localhost:$port"))
            .region(Region.US_EAST_1)
            .build()
        )
      )
    } yield Environment(sqs, sns)

  def resourcesF: Resource[IO, (SqsAsyncClientOp[IO], SnsAsyncClientOp[IO])] =
    for {
      sns <- SNSInterpreter[IO].SnsAsyncClientOpResource(
        SnsAsyncClient
          .builder()
          .credentialsProvider(StaticCredentialsProvider.create(creds))
          .endpointOverride(URI.create(s"http://localhost:$port"))
          .region(Region.US_EAST_1)
      )
      sqs <- SQSInterpreter[IO].SqsAsyncClientOpResource(
        SqsAsyncClient
          .builder()
          .credentialsProvider(StaticCredentialsProvider.create(creds))
          .endpointOverride(URI.create(s"http://localhost:$port"))
          .region(Region.US_EAST_1)
      )
    } yield sqs -> sns

  // Program with SQS and SNS algebras
  def program[F[_]: Async: Temporal](
      sqsOp: SqsAsyncClientOp[F],
      snsOp: SnsAsyncClientOp[F]
  ): F[ExitCode] =
    for {
      topicArn <- snsOp
        .createTopic(CreateTopicRequest.builder().name("topic").build())
        .map(_.topicArn())

      queueUrl <- sqsOp
        .createQueue(CreateQueueRequest.builder().queueName("names").build())
        .map(_.queueUrl())
      sqsArn <- sqsOp
        .getQueueAttributes(
          GetQueueAttributesRequest
            .builder()
            .queueUrl(queueUrl)
            .attributeNames(QueueAttributeName.QUEUE_ARN)
            .build()
        )
      _ <- snsOp.subscribe(
        SubscribeRequest
          .builder()
          .protocol("sqs")
          .endpoint(sqsArn.attributes().get(QueueAttributeName.QUEUE_ARN))
          .topicArn(topicArn)
          .build()
      )
      _ <- snsOp.publish(PublishRequest.builder().message("Barry").topicArn(topicArn).build())
      _ <- Temporal[F].sleep(5 seconds)
      msg <- sqsOp
        .receiveMessage(
          ReceiveMessageRequest
            .builder()
            .queueUrl(queueUrl)
            .build()
        )
      _ <- Sync[F].delay(
        println(
          s"Received message from SQS ${msg.messages().asScala.map(_.body())}"
        )
      )
      _ <- snsOp.deleteTopic(DeleteTopicRequest.builder().topicArn(topicArn).build())
      _ <- sqsOp.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build())
    } yield ExitCode.Success
}
