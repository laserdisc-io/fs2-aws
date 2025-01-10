package fs2.aws.internal

import cats.effect.Sync
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProvider, DefaultCredentialsProvider}
import com.google.common.util.concurrent.ListenableFuture
import software.amazon.kinesis.producer.{KinesisProducer, KinesisProducerConfiguration, UserRecordResult}

import java.nio.ByteBuffer

trait KinesisProducerClient[F[_]] {
  def putData(streamName: String, partitionKey: String, data: ByteBuffer)(implicit
      F: Sync[F]
  ): F[ListenableFuture[UserRecordResult]]
}

class KinesisProducerClientImpl[F[_]](config: Option[KinesisProducerConfiguration] = None)
    extends KinesisProducerClient[F] {

  val credentials: AwsCredentialsProvider = DefaultCredentialsProvider.create()
  val region: Option[String]              = None

  private lazy val defaultConfig: KinesisProducerConfiguration = {
    val c = new KinesisProducerConfiguration()
      .setCredentialsProvider(credentials)

    region.map(r => c.setRegion(r))
    c
  }

  private lazy val client = new KinesisProducer(config.getOrElse(defaultConfig))

  override def putData(streamName: String, partitionKey: String, data: ByteBuffer)(implicit
      F: Sync[F]
  ): F[ListenableFuture[UserRecordResult]] =
    F.delay(client.addUserRecord(streamName, partitionKey, data))
}
