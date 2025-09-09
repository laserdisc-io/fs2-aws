package fs2.aws.kinesis

import cats.effect.unsafe.IORuntime
import cats.effect.{IO, Resource}
import fs2.Stream
import fs2.aws.internal.KinesisProducerClientImpl
import fs2.aws.kinesis.publisher.writeToKinesis
import io.laserdisc.pure.cloudwatch.tagless.Interpreter as CloudwatchInterpreter
import io.laserdisc.pure.dynamodb.tagless.Interpreter as DynamoDbInterpreter
import io.laserdisc.pure.kinesis.tagless.Interpreter as KinesisInterpreter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Minutes, Second, Span}
import retry.*
import retry.RetryPolicies.constantDelay
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.{CloudWatchAsyncClient, CloudWatchAsyncClientBuilder}
import software.amazon.awssdk.services.dynamodb.{DynamoDbAsyncClient, DynamoDbAsyncClientBuilder}
import software.amazon.awssdk.services.kinesis.model.*
import software.amazon.awssdk.services.kinesis.{KinesisAsyncClient, KinesisAsyncClientBuilder}
import software.amazon.kinesis.common.InitialPositionInStream
import software.amazon.kinesis.producer.KinesisProducerConfiguration

import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class NewLocalStackSuite extends AnyFlatSpec with Matchers with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val runtime: IORuntime   = IORuntime.global

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(2, Minutes)),
    interval = scaled(Span(1, Second))
  )

  val partitionKey = "test"

  def mkConsumerConfig(streamName: String): KinesisConsumerSettings = KinesisConsumerSettings(
    streamName,
    "test-app",
    initialPositionInStream = Left(InitialPositionInStream.TRIM_HORIZON),
    retrievalMode = Polling
  )

  val credsProvider: StaticCredentialsProvider = StaticCredentialsProvider.create(
    AwsBasicCredentials.builder().accessKeyId("dummy").secretAccessKey("dummy").build()
  )

  val producerConfig: KinesisProducerConfiguration = new KinesisProducerConfiguration()
    .setCredentialsProvider(credsProvider)
    .setStsEndpoint("localhost")
    .setStsPort(4566)
    .setKinesisEndpoint("localhost")
    .setKinesisPort(4566)
    .setCloudwatchEndpoint("localhost")
    .setCloudwatchPort(4566)
    .setVerifyCertificate(false)
    .setRegion("us-east-1")

  val kac: KinesisAsyncClientBuilder    = configure(KinesisAsyncClient.builder())
  val dac: DynamoDbAsyncClientBuilder   = configure(DynamoDbAsyncClient.builder())
  val cac: CloudWatchAsyncClientBuilder = configure(CloudWatchAsyncClient.builder())

  def configure[BuilderT <: AwsClientBuilder[BuilderT, ClientT], ClientT]: BuilderT => BuilderT =
    _.credentialsProvider(credsProvider)
      .region(Region.US_EAST_1)
      .endpointOverride(URI.create("http://localhost:4566"))

  "The Kinesis producer and consumer" should "be able to produce to and consume from LocalStack" in {

    val streamName = "prod-consume-test-" + System.currentTimeMillis()
    println("Using stream name: " + streamName)

    val data = List("foo", "bar", "baz")

    val test = kAlgebraResource(kac, dac, cac, streamName).use { case (ki, kAlgebra) =>
      for {
        _ <- Stream
          .emits(data)
          .map(d => (partitionKey, ByteBuffer.wrap(d.getBytes)))
          .through(
            writeToKinesis[IO](
              streamName,
              producer = new KinesisProducerClientImpl[IO](Some(producerConfig))
            )
          )
          .compile
          .drain
        record <- kAlgebra
          .readFromKinesisStream(mkConsumerConfig(streamName))
          .take(data.length.toLong)
          .compile
          .toList
      } yield record

    }
    val records = test.unsafeToFuture().futureValue

    val actualPartitionKeys = records.map(_.record.partitionKey())
    val actualData          = records.map(deserialiseData)
    actualPartitionKeys shouldBe (1 to data.length).map(_ => partitionKey)
    actualData shouldBe data
  }

  ("The Kinesis fs2 Stream" should "seamlessly consume from re-sharded stream").ignore {

    val streamName = "reshard-consume-" + System.currentTimeMillis()
    println("Using stream name: " + streamName)

    val data           = List("foo", "bar", "baz")
    val consumerConfig = mkConsumerConfig(streamName)
    val resource       = kAlgebraResource(kac, dac, cac, streamName)
    val producer       = new KinesisProducerClientImpl[IO](Some(producerConfig))
    val test = resource.use { case (kAsyncInterpreter, kStreamInterpreter) =>
      for {

        _ <- Stream
          .emits(data)
          .map(d => (d, ByteBuffer.wrap(d.getBytes)))
          .through(writeToKinesis[IO](streamName, producer = producer))
          .compile
          .drain
        _ <- kAsyncInterpreter.updateShardCount(
          UpdateShardCountRequest.builder().streamName(streamName).targetShardCount(2).build()
        )
        _ <- fs2.Stream
          .retry(
            kAsyncInterpreter
              .describeStream(
                DescribeStreamRequest.builder().streamName(streamName).build()
              )
              .flatMap { r =>
                if (r.streamDescription().shards().size() != 2)
                  IO.raiseError(new RuntimeException("Expected 2 shards"))
                else IO.unit
              },
            2.seconds,
            _.*(2),
            5,
            _.getMessage == "Expected 2 shards"
          )
          .compile
          .drain
        _ <- Stream
          .emits(List("foo1", "bar1", "baz1"))
          .map(d => (d, ByteBuffer.wrap(d.getBytes)))
          .through(writeToKinesis[IO](streamName, producer = producer))
          .compile
          .drain
        record <- kStreamInterpreter
          .readFromKinesisStream(consumerConfig)
          .take(data.length.toLong * 2L)
          .compile
          .toList

      } yield record

    }
    val records = test.unsafeToFuture().futureValue

    val actualPartitionKeys = records.map(_.record.partitionKey())
    val actualData          = records.map(deserialiseData)
    actualPartitionKeys shouldBe (1 to data.length).map(_ => partitionKey)
    actualData shouldBe data
  }

  private def deserialiseData(committable: CommittableRecord) =
    StandardCharsets.UTF_8.decode(committable.record.data()).toString

  private def kAlgebraResource(
      kac: KinesisAsyncClientBuilder,
      dac: DynamoDbAsyncClientBuilder,
      cac: CloudWatchAsyncClientBuilder,
      streamName: String,
      shardCount: Int = 1
  ) = {
    val policy: RetryPolicy[IO, Any] = constantDelay[IO](250.millis)

    val createReq = CreateStreamRequest.builder().streamName(streamName).shardCount(shardCount).build()
    val descReq   = DescribeStreamRequest.builder().streamName(streamName).build()
    val deleteReq = DeleteStreamRequest.builder().streamName(streamName).build()

    for {
      d <- DynamoDbInterpreter[IO].DynamoDbAsyncClientResource(dac)
      c <- CloudwatchInterpreter[IO].CloudWatchAsyncClientResource(cac)
      i = KinesisInterpreter[IO]
      k <- i.KinesisAsyncClientResource(kac)
      kinesisInterpreter = i.create(k)
      kAlgebra           = Kinesis.create[IO](k, d, c)
      _ <- Resource.make(
        for {
          _ <- kinesisInterpreter.createStream(createReq)
          _ <- retryingOnFailures(kinesisInterpreter.describeStream(descReq))(
            policy = constantDelay[IO](500.millis),
            valueHandler = (resp: DescribeStreamResponse, retryStats: RetryDetails) =>
              resp.streamDescription().streamStatus() match {
                case StreamStatus.ACTIVE =>
                  IO.println(s"Stream '$streamName' is active.").as(HandlerDecision.Stop)
                case status if retryStats.cumulativeDelay > 30.seconds =>
                  IO.delay(fail(s"Gave up waiting on stream '$streamName' to become active. Status=$resp'"))
                case status =>
                  IO.println(
                    s"Waiting on '$streamName' to be ready (status=$status, waited ${retryStats.cumulativeDelay.toMillis}ms)"
                  ).as(HandlerDecision.Continue)
              }
          )

        } yield ()
      )(_ => IO.unit /* kinesisInterpreter.deleteStream(deleteReq).void */ )
    } yield kinesisInterpreter -> kAlgebra
  }

}
