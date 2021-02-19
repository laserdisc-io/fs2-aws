import com.amazonaws.auth.{ AWSStaticCredentialsProvider, BasicAWSCredentials }
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration
import fs2.aws.kinesis.{ KinesisConsumerSettings, Polling }
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.{
  CloudWatchAsyncClient,
  CloudWatchAsyncClientBuilder
}
import software.amazon.awssdk.services.dynamodb.{ DynamoDbAsyncClient, DynamoDbAsyncClientBuilder }
import software.amazon.awssdk.services.kinesis.{ KinesisAsyncClient, KinesisAsyncClientBuilder }
import software.amazon.kinesis.common.InitialPositionInStream
import cats.implicits._

import java.net.URI
import java.util.Date

case class KinesisAppConfig(
  awsHost: String,
  awsPort: Int,
  awsRegion: Region,
  awsKeyId: String,
  awsKey: String,
  streamName: String,
  appName: String
)
object KinesisAppConfig {
  def localstackConfig: KinesisAppConfig = KinesisAppConfig(
    awsHost = "localhost",
    awsPort = 4566,
    awsRegion = Region.US_EAST_1,
    awsKeyId = "dummy",
    awsKey = "dummy",
    streamName = "example",
    appName = "test-app"
  )
}

object syntax {

  implicit class ConfigExtensions(kinesisAppConfig: KinesisAppConfig) {
    private val cp = StaticCredentialsProvider.create(
      AwsBasicCredentials.create(kinesisAppConfig.awsKeyId, kinesisAppConfig.awsKey)
    )
    private def overwriteStuff[B <: AwsClientBuilder[B, C], C](
      awsClientBuilder: AwsClientBuilder[B, C]
    ) =
      awsClientBuilder
        .credentialsProvider(cp)
        .region(kinesisAppConfig.awsRegion)
        .endpointOverride(
          URI.create(s"http://${kinesisAppConfig.awsHost}:${kinesisAppConfig.awsPort}")
        )

    def kinesisSdkBuilder: KinesisAsyncClientBuilder = overwriteStuff(KinesisAsyncClient.builder())
    def dynamoSdkBuilder: DynamoDbAsyncClientBuilder = overwriteStuff(DynamoDbAsyncClient.builder())
    def cloudwatchSdkBuilder: CloudWatchAsyncClientBuilder =
      overwriteStuff(CloudWatchAsyncClient.builder())

    def consumerConfig: KinesisConsumerSettings = KinesisConsumerSettings(
      kinesisAppConfig.streamName,
      kinesisAppConfig.appName,
      initialPositionInStream = InitialPositionInStream.TRIM_HORIZON.asLeft[Date],
      endpoint = Some(s"http://${kinesisAppConfig.awsHost}:${kinesisAppConfig.awsPort}"),
      retrievalMode = Polling
    )

    def producerConfig: KinesisProducerConfiguration = {
      val credentials = new BasicAWSCredentials(kinesisAppConfig.awsKeyId, kinesisAppConfig.awsKey)

      new KinesisProducerConfiguration()
        .setCredentialsProvider(new AWSStaticCredentialsProvider(credentials))
        .setKinesisEndpoint(kinesisAppConfig.awsHost)
        .setKinesisPort(kinesisAppConfig.awsPort)
        .setCloudwatchEndpoint(kinesisAppConfig.awsHost)
        .setCloudwatchPort(kinesisAppConfig.awsPort)
        .setVerifyCertificate(false)
        .setRegion(kinesisAppConfig.awsRegion.id())
    }

  }

}
