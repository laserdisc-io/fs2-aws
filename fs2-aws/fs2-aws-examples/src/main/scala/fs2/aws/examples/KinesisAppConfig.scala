package fs2.aws.examples

import cats.implicits.*
import fs2.aws.kinesis.models.KinesisModels.{AppName, StreamName}
import fs2.aws.kinesis.{KinesisConsumerSettings, Polling}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.{CloudWatchAsyncClient, CloudWatchAsyncClientBuilder}
import software.amazon.awssdk.services.dynamodb.{DynamoDbAsyncClient, DynamoDbAsyncClientBuilder}
import software.amazon.awssdk.services.kinesis.{KinesisAsyncClient, KinesisAsyncClientBuilder}
import software.amazon.kinesis.common.InitialPositionInStream
import software.amazon.kinesis.producer.KinesisProducerConfiguration

import java.net.URI
import java.util.Date

case class KinesisAppConfig(
    awsHost: String,
    awsPort: Long,
    awsRegion: Region,
    awsKeyId: String,
    awsKey: String,
    streamName: StreamName,
    appName: AppName
)

object KinesisAppConfig {

  def localstackConfig: Either[String, KinesisAppConfig] = (AppName("test-app"), StreamName("example")).mapN {
    case (appName, streamName) =>
      KinesisAppConfig(
        awsHost = "localhost",
        awsPort = 4566L,
        awsRegion = Region.US_EAST_1,
        awsKeyId = "dummy",
        awsKey = "dummy",
        streamName = streamName,
        appName = appName
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
        retrievalMode = Polling
      )

      def producerConfig: KinesisProducerConfiguration = {
        val credentials = AwsBasicCredentials.create(kinesisAppConfig.awsKeyId, kinesisAppConfig.awsKey)

        new KinesisProducerConfiguration()
          .setCredentialsProvider(StaticCredentialsProvider.create(credentials))
          .setStsPort(kinesisAppConfig.awsPort)
          .setStsEndpoint(kinesisAppConfig.awsHost)
          .setKinesisEndpoint(kinesisAppConfig.awsHost)
          .setKinesisPort(kinesisAppConfig.awsPort)
          .setCloudwatchEndpoint(kinesisAppConfig.awsHost)
          .setCloudwatchPort(kinesisAppConfig.awsPort)
          .setVerifyCertificate(false)
          .setRegion(kinesisAppConfig.awsRegion.id())
      }

    }

  }
}
