package fs2.aws.internal

import java.nio.ByteBuffer

import cats.effect.Effect
import com.amazonaws.auth.{AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain}
import com.amazonaws.services.kinesis.producer.{
  KinesisProducer,
  KinesisProducerConfiguration,
  UserRecordResult
}
import com.google.common.util.concurrent.ListenableFuture

trait KinesisProducerClient[F[_]] {
  def putData(streamName: String, partitionKey: String, data: ByteBuffer)(
      implicit F: Effect[F]): F[ListenableFuture[UserRecordResult]]
}

class KinesisProducerClientImpl[F[_]] extends KinesisProducerClient[F] {

  val credentials: AWSCredentialsProviderChain = new DefaultAWSCredentialsProviderChain()
  val region: Option[String]                   = None

  private lazy val config: KinesisProducerConfiguration = {
    val c = new KinesisProducerConfiguration()
      .setCredentialsProvider(credentials)

    region.map(r => c.setRegion(r))
    c
  }

  private lazy val client = new KinesisProducer(config)

  override def putData(streamName: String, partitionKey: String, data: ByteBuffer)(
      implicit F: Effect[F]): F[ListenableFuture[UserRecordResult]] =
    F.delay(client.addUserRecord(streamName, partitionKey, data))
}
