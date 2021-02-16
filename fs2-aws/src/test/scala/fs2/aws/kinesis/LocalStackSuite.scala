package fs2.aws.kinesis

import cats.effect.{ Blocker, ContextShift, IO, Timer }
import com.amazonaws.auth.{ AWSStaticCredentialsProvider, BasicAWSCredentials }
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration
import fs2.Stream
import fs2.aws.internal.KinesisProducerClientImpl
import fs2.aws.kinesis.publisher.writeToKinesis
import io.laserdisc.pure.cloudwatch
import io.laserdisc.pure.dynamodb
import io.laserdisc.pure.kinesis
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Minutes, Second, Span }
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.common.InitialPositionInStream

import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext

class LocalStackSuite extends AnyFlatSpec with Matchers with ScalaFutures {

  implicit val ec: ExecutionContext             = ExecutionContext.global
  implicit val timer: Timer[IO]                 = IO.timer(ec)
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(2, Minutes)), interval = scaled(Span(1, Second)))

  "The Kinesis producer and consumer" should "be able to produce to and consume from LocalStack" in {

    // this is required to make the KCL work with LocalStack
    System.setProperty("aws.cborEnabled", "false")

    val streamName = "test"

    val consumerConfig = KinesisConsumerSettings(
      streamName,
      "test-app",
      initialPositionInStream = Left(InitialPositionInStream.TRIM_HORIZON),
      endpoint = Some("http://localhost:4566"),
      retrievalMode = Polling
    )

    val credentials =
      new BasicAWSCredentials("dummy", "dummy")

    val producerConfig = new KinesisProducerConfiguration()
      .setCredentialsProvider(new AWSStaticCredentialsProvider(credentials))
      .setKinesisEndpoint("localhost")
      .setKinesisPort(4566)
      .setCloudwatchEndpoint("localhost")
      .setCloudwatchPort(4566)
      .setVerifyCertificate(false)
      .setRegion("us-east-1")

    val partitionKey = "test"
    val data         = List("foo", "bar", "baz")

    val cp = StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy", "dummy"))

    val kac = KinesisAsyncClient
      .builder()
      .credentialsProvider(cp)
      .region(Region.US_EAST_1)
      .endpointOverride(URI.create("http://localhost:4566"))
    val dac = DynamoDbAsyncClient
      .builder()
      .credentialsProvider(cp)
      .region(Region.US_EAST_1)
      .endpointOverride(URI.create("http://localhost:4566"))
    val cac =
      CloudWatchAsyncClient
        .builder()
        .credentialsProvider(cp)
        .region(Region.US_EAST_1)
        .endpointOverride(URI.create("http://localhost:4566"))
    val test = (for {
      b <- Blocker[IO]
      k <- kinesis.tagless.Interpreter[IO](b).KinesisAsyncClientResource(kac)
      d <- dynamodb.tagless.Interpreter[IO](b).DynamoDbAsyncClientResource(dac)
      c <- cloudwatch.tagless.Interpreter[IO](b).CloudWatchAsyncClientResource(cac)
    } yield (k, d, c)).use {
      case (k, d, c) =>
        val kAlgebra = Kinesis.create[IO](k, d, c)

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
                     .readFromKinesisStream(consumerConfig)
                     .take(data.length)
                     .compile
                     .toList
        } yield record

    }
    val records = test.unsafeToFuture().futureValue

    val actualPartitionKeys = records.map(_.record.partitionKey())
    val actualData          = records.map(deserialiseData)
    actualPartitionKeys shouldBe (1 to data.length).map(_ => partitionKey)
    actualData          shouldBe data
  }

  private def deserialiseData(committable: CommittableRecord) =
    StandardCharsets.UTF_8.decode(committable.record.data()).toString

}
