package fs2.aws.examples

import cats.Parallel
import cats.effect.*
import cats.implicits.*
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration
import fs2.Stream
import fs2.aws.examples.KinesisMultistreamAppConfig.syntax.*
import fs2.aws.internal.KinesisProducerClientImpl
import fs2.aws.kinesis.models.KinesisModels.AppName
import fs2.aws.kinesis.publisher.writeToKinesis
import fs2.aws.kinesis.{CommittableRecord, DefaultKinesisStreamBuilder}
import io.laserdisc.pure.cloudwatch.tagless.Interpreter as CloudwatchInterpreter
import io.laserdisc.pure.dynamodb.tagless.Interpreter as DynamoDbInterpreter
import io.laserdisc.pure.kinesis.tagless.{Interpreter as KinesisInterpreter, KinesisAsyncClientOp}
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder
import software.amazon.awssdk.services.kinesis.model.{CreateStreamRequest, DeleteStreamRequest}
import software.amazon.kinesis.common.{StreamConfig, StreamIdentifier}
import software.amazon.kinesis.processor.{FormerStreamsLeasesDeletionStrategy, MultiStreamTracker}

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*

object KinesisMultistreamExample extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val appConfig = KinesisMultistreamAppConfig.localstackConfig
    kAlgebraResource[IO](
      appConfig.kinesisSdkBuilder,
      appConfig.dynamoSdkBuilder,
      appConfig.cloudwatchSdkBuilder,
      appConfig.streamNames
    ).use(stream =>
      program[IO](stream.flatMap(Stream.chunk), appConfig.producerConfig, appConfig.streamNames).as(ExitCode.Success)
    )
  }
  private def kAlgebraResource[F[_]: Async: Concurrent](
      kac: KinesisAsyncClientBuilder,
      dac: DynamoDbAsyncClientBuilder,
      cac: CloudWatchAsyncClientBuilder,
      streamNames: List[String]
  ) =
    for {
      k <- KinesisInterpreter[F].KinesisAsyncClientResource(kac)
      _ <- streamNames.map(streamName => disposableStream(KinesisInterpreter[F].create(k), streamName)).parSequence_
      appName <- Resource.eval(Sync[F].fromEither(AppName("kinesis-multistream-example").leftMap(new Throwable(_))))
      stream <- DefaultKinesisStreamBuilder[F]()
        .withAppName(appName)
        .withKinesisClient(k)
        .withDynamoDBClient(DynamoDbInterpreter[F].DynamoDbAsyncClientResource(dac))
        .withCloudWatchClient(CloudwatchInterpreter[F].CloudWatchAsyncClientResource(cac))
        .withDefaultSchedulerId
        .withMultiStreamTracker
        .withStreamTracker(
          Resource.pure(new MultiStreamTracker {
            override def streamConfigList(): util.List[StreamConfig] =
              streamNames.map { sn =>
                val streamIdentifier =
                  StreamIdentifier.multiStreamInstance(s"accountId:$sn:${Instant.now().getEpochSecond}")

                new StreamConfig(streamIdentifier, ???)
              }.asJava

            override def formerStreamsLeasesDeletionStrategy(): FormerStreamsLeasesDeletionStrategy =
              new FormerStreamsLeasesDeletionStrategy.NoLeaseDeletionStrategy()
          })
        )
        .withDefaultSchedulerConfigs
        .withDefaultBufferSize
        .withDefaultScheduler
        .build
    } yield stream

  def program[F[_]: Async: Concurrent: Temporal: Parallel](
      stream: Stream[F, CommittableRecord],
      producerConfiguration: KinesisProducerConfiguration,
      streamNames: List[String]
  ): F[Unit] =
    (
      streamNames.map { streamName =>
        Stream
          .awakeEvery[F](5.seconds)
          .take(5)
          .map(e => s"$e -> $streamName")
          .evalTap(d => Sync[F].delay(println(s"Producing $d")))
          .map(d => (d, ByteBuffer.wrap(d.getBytes)))
          .through(
            writeToKinesis[F](
              streamName,
              producer = new KinesisProducerClientImpl[F](Some(producerConfiguration))
            )
          )
          .compile
          .drain
      }.parSequence_,
      stream
        .take(streamNames.size * 5L)
        .map(cr => StandardCharsets.UTF_8.decode(cr.record.data()).toString)
        .evalTap(cr => Sync[F].delay(println(s"Consuming $cr")))
        .compile
        .drain
    ).parMapN { case (_, _) => () }

  private def disposableStream[F[_]: Sync](
      interpreter: KinesisAsyncClientOp[F],
      streamName: String
  ) =
    Resource.make(
      interpreter.createStream(
        CreateStreamRequest.builder().streamName(streamName).shardCount(1).build()
      )
    )(_ =>
      interpreter
        .deleteStream(DeleteStreamRequest.builder().streamName(streamName).build())
        .void
    )

}
