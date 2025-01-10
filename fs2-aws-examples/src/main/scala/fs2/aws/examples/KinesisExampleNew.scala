package fs2.aws.examples

import cats.NonEmptyParallel
import cats.effect.*
import cats.implicits.*
import software.amazon.kinesis.producer.KinesisProducerConfiguration
import fs2.Stream
import fs2.aws.examples.KinesisAppConfig.syntax.*
import fs2.aws.internal.KinesisProducerClientImpl
import fs2.aws.kinesis.models.KinesisModels.{AppName, StreamName}
import fs2.aws.kinesis.publisher.writeToKinesis
import fs2.aws.kinesis.{CommittableRecord, DefaultKinesisStreamBuilder}
import io.laserdisc.pure.cloudwatch.tagless.Interpreter as CloudwatchInterpreter
import io.laserdisc.pure.dynamodb.tagless.Interpreter as DynamoDbInterpreter
import io.laserdisc.pure.kinesis.tagless.{Interpreter as KinesisInterpreter, KinesisAsyncClientOp}
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder
import software.amazon.awssdk.services.kinesis.model.{CreateStreamRequest, DeleteStreamRequest}
import software.amazon.kinesis.common.{InitialPositionInStream, InitialPositionInStreamExtended, StreamIdentifier}
import software.amazon.kinesis.processor.SingleStreamTracker

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

object KinesisExampleNew extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    Resource
      .eval(IO.fromEither(KinesisAppConfig.localstackConfig.leftMap(new IllegalArgumentException(_))))
      .flatMap(appConfig =>
        kAlgebraResource[IO](
          appConfig.kinesisSdkBuilder,
          appConfig.dynamoSdkBuilder,
          appConfig.cloudwatchSdkBuilder,
          appConfig.streamName
        ).map(k => (appConfig, k))
      )
      .use { case (appConfig, stream) =>
        program[IO](stream.flatMap(Stream.chunk), appConfig.producerConfig, appConfig.streamName).as(ExitCode.Success)
      }

  private def kAlgebraResource[F[_]: Async: Concurrent](
      kac: KinesisAsyncClientBuilder,
      dac: DynamoDbAsyncClientBuilder,
      cac: CloudWatchAsyncClientBuilder,
      streamName: StreamName
  ) =
    for {
      k       <- KinesisInterpreter[F].KinesisAsyncClientResource(kac)
      _       <- disposableStream(KinesisInterpreter[F].create(k), streamName)
      appName <- Resource.eval(Sync[F].fromEither(AppName("test").leftMap(new Throwable(_))))
      stream  <- DefaultKinesisStreamBuilder[F]()
        .withAppName(appName)
        .withKinesisClient(k)
        .withDynamoDBClient(DynamoDbInterpreter[F].DynamoDbAsyncClientResource(dac))
        .withCloudWatchClient(CloudwatchInterpreter[F].CloudWatchAsyncClientResource(cac))
        .withDefaultSchedulerId
        .withSingleStreamTracker(streamName)
        .withStreamTracker { streamName =>
          Resource.pure(
            new SingleStreamTracker(
              StreamIdentifier.singleStreamInstance(streamName),
              InitialPositionInStreamExtended.newInitialPosition(InitialPositionInStream.TRIM_HORIZON)
            )
          )
        }
        .withDefaultSchedulerConfigs
        .withDefaultBufferSize
        .withDefaultScheduler
        .build
    } yield stream

  def program[F[_]: Async: Concurrent: Temporal: NonEmptyParallel](
      stream: Stream[F, CommittableRecord],
      producerConfiguration: KinesisProducerConfiguration,
      streamName: String
  ): F[Unit] =
    (
      Stream
        .awakeEvery[F](5.seconds)
        .take(15)
        .map(_.toString())
        .evalTap(d => Sync[F].delay(println(s"Producing $d")))
        .map(d => (d, ByteBuffer.wrap(d.getBytes)))
        .through(
          writeToKinesis[F](
            streamName,
            producer = new KinesisProducerClientImpl[F](Some(producerConfiguration))
          )
        )
        .compile
        .drain,
      stream
        .take(15)
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
