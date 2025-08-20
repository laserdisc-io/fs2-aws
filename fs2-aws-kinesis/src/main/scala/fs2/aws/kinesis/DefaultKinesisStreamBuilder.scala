package fs2.aws.kinesis
import cats.effect.kernel.Resource
import cats.effect.std.{Dispatcher, Queue}
import cats.effect.{Async, Concurrent, Sync}
import cats.syntax.either.*
import cats.syntax.flatMap.*
import fs2.aws.kinesis.models.KinesisModels.{AppName, BufferSize, SchedulerId, StreamName}
import fs2.concurrent.SignallingRef
import fs2.{Chunk, Stream}
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.common.{ConfigsBuilder, StreamIdentifier}
import software.amazon.kinesis.coordinator.CoordinatorConfig.ClientVersionConfig
import software.amazon.kinesis.coordinator.CoordinatorConfig.ClientVersionConfig.CLIENT_VERSION_CONFIG_COMPATIBLE_WITH_2X
import software.amazon.kinesis.coordinator.Scheduler
import software.amazon.kinesis.processor.*

import java.util.UUID
object DefaultKinesisStreamBuilder {

  def apply[F[_]: Async: Concurrent](): KinesisStreamBuilder[F]#InitialPhase = {
    val ksb = new DefaultKinesisStreamBuilder[F]()
    new ksb.DefaultInitialPhase()
  }
}

class DefaultKinesisStreamBuilder[F[_]: Async: Concurrent] extends KinesisStreamBuilder[F] {

  private class DefaultInitialPhase extends InitialPhase {
    override protected def next(appName: Resource[F, AppName]): KinesisClientPhase =
      new DefaultKinesisClientPhase(appName)
  }

  private class DefaultKinesisClientPhase(appName: Resource[F, AppName]) extends KinesisClientPhase {
    override protected def next(build: Resource[F, KinesisAsyncClient]): DynamoDBClientPhase =
      new DefaultDynamoDBClientPhase(appName, build)
  }

  private class DefaultDynamoDBClientPhase(
      appName: Resource[F, AppName],
      kinesisClient: Resource[F, KinesisAsyncClient]
  ) extends DynamoDBClientPhase {
    override protected def next(dynamoDBClient: Resource[F, DynamoDbAsyncClient]): CloudWatchClientPhase =
      new DefaultCloudWatchClientPhase(appName, kinesisClient, dynamoDBClient)
  }

  private class DefaultCloudWatchClientPhase(
      appName: Resource[F, AppName],
      kinesisClient: Resource[F, KinesisAsyncClient],
      dynamoDBClient: Resource[F, DynamoDbAsyncClient]
  ) extends CloudWatchClientPhase {
    override protected def next(cloudWatchClient: Resource[F, CloudWatchAsyncClient]): SchedulerIdPhase =
      new DefaultSchedulerIdPhase(appName, kinesisClient, dynamoDBClient, cloudWatchClient)
  }

  final private class DefaultSchedulerIdPhase(
      appName: Resource[F, AppName],
      kinesisClient: Resource[F, KinesisAsyncClient],
      dynamoDBClient: Resource[F, DynamoDbAsyncClient],
      cloudWatchClient: Resource[F, CloudWatchAsyncClient]
  ) extends SchedulerIdPhase {
    override protected def next(schedulerId: Resource[F, SchedulerId]): StreamTrackerJunctionPhase =
      new DefaultStreamTrackerJunctionPhase(
        appName,
        kinesisClient,
        dynamoDBClient,
        cloudWatchClient,
        schedulerId
      )
    override protected def defaultSchedulerId: Resource[F, SchedulerId] =
      Resource.eval(Sync[F].delay(SchedulerId(UUID.randomUUID().toString)))

  }

  private class DefaultStreamTrackerJunctionPhase(
      appName: Resource[F, AppName],
      kinesisClient: Resource[F, KinesisAsyncClient],
      dynamoDBClient: Resource[F, DynamoDbAsyncClient],
      cloudWatchClient: Resource[F, CloudWatchAsyncClient],
      schedulerId: Resource[F, SchedulerId]
  ) extends StreamTrackerJunctionPhase {
    override protected def next(streamName: Resource[F, StreamName]): SingleStreamTrackerPhase =
      new DefaultStreamTrackerPhase(appName, kinesisClient, dynamoDBClient, cloudWatchClient, schedulerId, streamName)

    override protected def nextMulti: MultiStreamTrackerPhase = new DefaultMultiStreamTrackerPhase(
      appName,
      kinesisClient,
      dynamoDBClient,
      cloudWatchClient,
      schedulerId
    )
  }
  final private class DefaultStreamTrackerPhase(
      appName: Resource[F, AppName],
      kinesisClient: Resource[F, KinesisAsyncClient],
      dynamoDBClient: Resource[F, DynamoDbAsyncClient],
      cloudWatchClient: Resource[F, CloudWatchAsyncClient],
      schedulerId: Resource[F, SchedulerId],
      streamName: Resource[F, StreamName]
  ) extends SingleStreamTrackerPhase {
    override protected def next(build: StreamName => Resource[F, SingleStreamTracker]): SchedulerConfigPhase =
      new DefaultSchedulerConfigPhase(
        appName,
        kinesisClient,
        dynamoDBClient,
        cloudWatchClient,
        schedulerId,
        streamName.flatMap(build)
      )

    override protected def defaultStreamTracker: StreamName => Resource[F, SingleStreamTracker] = streamName =>
      Resource.pure(
        new SingleStreamTracker(StreamIdentifier.singleStreamInstance(streamName))
      )
  }

  private class DefaultMultiStreamTrackerPhase(
      appName: Resource[F, AppName],
      kinesisClient: Resource[F, KinesisAsyncClient],
      dynamoDBClient: Resource[F, DynamoDbAsyncClient],
      cloudWatchClient: Resource[F, CloudWatchAsyncClient],
      schedulerId: Resource[F, SchedulerId]
  ) extends MultiStreamTrackerPhase {
    override protected def next(streamTracker: Resource[F, MultiStreamTracker]): SchedulerConfigPhase =
      new DefaultSchedulerConfigPhase(
        appName,
        kinesisClient,
        dynamoDBClient,
        cloudWatchClient,
        schedulerId,
        streamTracker
      )
  }

  final private class DefaultSchedulerConfigPhase(
      appName: Resource[F, AppName],
      kinesisClient: Resource[F, KinesisAsyncClient],
      dynamoDBClient: Resource[F, DynamoDbAsyncClient],
      cloudWatchClient: Resource[F, CloudWatchAsyncClient],
      schedulerId: Resource[F, SchedulerId],
      streamTracker: Resource[F, StreamTracker]
  ) extends SchedulerConfigPhase {
    override protected def next(
        schedulerConfigs: (
            KinesisAsyncClient,
            DynamoDbAsyncClient,
            CloudWatchAsyncClient,
            StreamTracker,
            SchedulerId,
            AppName,
            Dispatcher[F],
            Queue[F, Chunk[CommittableRecord]]
        ) => Resource[F, ConfigsBuilder]
    ): BufferSizePhase = new DefaultBufferSizePhase(
      appName,
      kinesisClient,
      dynamoDBClient,
      cloudWatchClient,
      schedulerId,
      streamTracker,
      schedulerConfigs
    )

    override protected def defaultSchedulerConfigs: (
        KinesisAsyncClient,
        DynamoDbAsyncClient,
        CloudWatchAsyncClient,
        StreamTracker,
        SchedulerId,
        AppName,
        Dispatcher[F],
        Queue[F, Chunk[CommittableRecord]]
    ) => Resource[F, ConfigsBuilder] = {
      case (kinesisClient, dynamoDBClient, cloudWatchClient, streamTracker, schedulerId, appName, dispatcher, queue) =>
        Resource.pure(
          new ConfigsBuilder(
            streamTracker,
            appName,
            kinesisClient,
            dynamoDBClient,
            cloudWatchClient,
            schedulerId,
            new ShardRecordProcessorFactory() {
              override def shardRecordProcessor(): ShardRecordProcessor =
                new ChunkedRecordProcessor(records => dispatcher.unsafeRunSync(queue.offer(records)))
            }
          )
        )
    }
  }

  private class DefaultBufferSizePhase(
      appName: Resource[F, AppName],
      kinesisClient: Resource[F, KinesisAsyncClient],
      dynamoDBClient: Resource[F, DynamoDbAsyncClient],
      cloudWatchClient: Resource[F, CloudWatchAsyncClient],
      schedulerId: Resource[F, SchedulerId],
      streamTracker: Resource[F, StreamTracker],
      schedulerConfigs: (
          KinesisAsyncClient,
          DynamoDbAsyncClient,
          CloudWatchAsyncClient,
          StreamTracker,
          SchedulerId,
          AppName,
          Dispatcher[F],
          Queue[F, Chunk[CommittableRecord]]
      ) => Resource[F, ConfigsBuilder]
  ) extends BufferSizePhase {
    override protected def next(bufferSize: Resource[F, BufferSize]): SchedulerPhase =
      new DefaultSchedulerPhase(
        appName,
        kinesisClient,
        dynamoDBClient,
        cloudWatchClient,
        schedulerId,
        streamTracker,
        bufferSize,
        schedulerConfigs
      )

    override protected def defaultBufferSize: Resource[F, BufferSize] =
      Resource.eval(Sync[F].fromEither(BufferSize(10).leftMap(new IllegalArgumentException(_))))
  }

  private class DefaultSchedulerPhase(
      appName: Resource[F, AppName],
      kinesisClient: Resource[F, KinesisAsyncClient],
      dynamoDBClient: Resource[F, DynamoDbAsyncClient],
      cloudWatchClient: Resource[F, CloudWatchAsyncClient],
      schedulerId: Resource[F, SchedulerId],
      streamTracker: Resource[F, StreamTracker],
      bufferSizeRes: Resource[F, BufferSize],
      schedulerConfigs: (
          KinesisAsyncClient,
          DynamoDbAsyncClient,
          CloudWatchAsyncClient,
          StreamTracker,
          SchedulerId,
          AppName,
          Dispatcher[F],
          Queue[F, Chunk[CommittableRecord]]
      ) => Resource[F, ConfigsBuilder]
  ) extends SchedulerPhase {
    override protected def next(s: ConfigsBuilder => Resource[F, Scheduler]): FinalPhase =
      new DefaultFinalPhase(for {
        app              <- appName
        kinesisClient    <- kinesisClient
        dynamoDBClient   <- dynamoDBClient
        cloudWatchClient <- cloudWatchClient
        id               <- schedulerId
        dispatcher       <- Dispatcher.parallel[F]
        bufferSize       <- bufferSizeRes
        queue            <- Resource.eval(Queue.bounded[F, Chunk[CommittableRecord]](bufferSize))
        streamTracker    <- streamTracker
        cb <- schedulerConfigs(
          kinesisClient,
          dynamoDBClient,
          cloudWatchClient,
          streamTracker,
          id,
          app,
          dispatcher,
          queue
        )
        scheduler <- s(cb)
        signal    <- Resource.eval(SignallingRef[F, Boolean](false))

        _ <- Resource.make(Concurrent[F].start(Async[F].blocking(scheduler.run()).flatTap(_ => signal.set(true))))(_ =>
          Async[F].blocking(scheduler.shutdown())
        )

      } yield (scheduler, queue, signal))

    override protected def defaultScheduler: ConfigsBuilder => Resource[F, Scheduler] = cb =>
      Resource.pure(
        new Scheduler(
          cb.checkpointConfig(),
          cb.coordinatorConfig().clientVersionConfig(CLIENT_VERSION_CONFIG_COMPATIBLE_WITH_2X),
          cb.leaseManagementConfig(),
          cb.lifecycleConfig(),
          cb.metricsConfig(),
          cb.processorConfig(),
          cb.retrievalConfig()
        )
      )
  }

  private class DefaultFinalPhase(
      scheduler: Resource[F, (Scheduler, Queue[F, Chunk[CommittableRecord]], SignallingRef[F, Boolean])]
  ) extends FinalPhase {
    override def build: Resource[F, Stream[F, Chunk[CommittableRecord]]] =
      scheduler.map { case (_, buffer, signal) =>
        Stream.fromQueueUnterminated(buffer).interruptWhen(signal)
      }
  }
}
