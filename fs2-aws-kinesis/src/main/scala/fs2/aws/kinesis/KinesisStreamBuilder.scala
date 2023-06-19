package fs2.aws.kinesis

import cats.effect.kernel.Resource
import cats.effect.std.{Dispatcher, Queue}
import fs2.aws.kinesis.models.KinesisModels.{AppName, BufferSize, SchedulerId, StreamName}
import fs2.{Chunk, Stream}
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.common.ConfigsBuilder
import software.amazon.kinesis.coordinator.Scheduler
import software.amazon.kinesis.processor.{MultiStreamTracker, SingleStreamTracker, StreamTracker}

abstract class KinesisStreamBuilder[F[_]] {

  trait InitialPhase {

    protected def next(build: Resource[F, AppName]): KinesisClientPhase

    def withAppName(appName: AppName): KinesisClientPhase              = next(Resource.pure(appName))
    def withAppName(appName: Resource[F, AppName]): KinesisClientPhase = next(appName)

  }

  trait KinesisClientPhase {

    protected def next(build: Resource[F, KinesisAsyncClient]): DynamoDBClientPhase

    def withKinesisClient(kinesisClient: Resource[F, KinesisAsyncClient]): DynamoDBClientPhase = next(kinesisClient)
    def withKinesisClient(kinesisClient: KinesisAsyncClient): DynamoDBClientPhase = next(Resource.pure(kinesisClient))
  }

  trait DynamoDBClientPhase {

    protected def next(build: Resource[F, DynamoDbAsyncClient]): CloudWatchClientPhase

    def withDynamoDBClient(dynamoDBClient: Resource[F, DynamoDbAsyncClient]): CloudWatchClientPhase = next(
      dynamoDBClient
    )

    def withDynamoDBClient(dynamoDBClient: DynamoDbAsyncClient): CloudWatchClientPhase = next(
      Resource.pure(dynamoDBClient)
    )
  }

  trait CloudWatchClientPhase {
    protected def next(build: Resource[F, CloudWatchAsyncClient]): SchedulerIdPhase
    def withCloudWatchClient(cloudWatchClient: Resource[F, CloudWatchAsyncClient]): SchedulerIdPhase = next(
      cloudWatchClient
    )
    def withCloudWatchClient(cloudWatchClient: CloudWatchAsyncClient): SchedulerIdPhase = next(
      Resource.pure(cloudWatchClient)
    )
  }

  trait SchedulerConfigPhase {
    protected def next(
        build: (
            KinesisAsyncClient,
            DynamoDbAsyncClient,
            CloudWatchAsyncClient,
            StreamTracker,
            SchedulerId,
            AppName,
            Dispatcher[F],
            Queue[F, Chunk[CommittableRecord]]
        ) => Resource[F, ConfigsBuilder]
    ): BufferSizePhase

    def withSchedulerConfigs(
        configs: (
            KinesisAsyncClient,
            DynamoDbAsyncClient,
            CloudWatchAsyncClient,
            StreamTracker,
            SchedulerId,
            AppName,
            Dispatcher[F],
            Queue[F, Chunk[CommittableRecord]]
        ) => Resource[F, ConfigsBuilder]
    ): BufferSizePhase = next(configs)

    protected def defaultSchedulerConfigs: (
        KinesisAsyncClient,
        DynamoDbAsyncClient,
        CloudWatchAsyncClient,
        StreamTracker,
        SchedulerId,
        AppName,
        Dispatcher[F],
        Queue[F, Chunk[CommittableRecord]]
    ) => Resource[F, ConfigsBuilder]
    def withDefaultSchedulerConfigs: BufferSizePhase = next(defaultSchedulerConfigs)
  }

  trait BufferSizePhase {
    protected def next(build: Resource[F, BufferSize]): SchedulerPhase
    protected def defaultBufferSize: Resource[F, BufferSize]
    def withBufferSize(bufferSize: Resource[F, BufferSize]): SchedulerPhase = next(bufferSize)
    def withDefaultBufferSize: SchedulerPhase                               = next(defaultBufferSize)
  }

  trait SchedulerPhase {

    protected def next(scheduler: ConfigsBuilder => Resource[F, Scheduler]): FinalPhase
    protected def defaultScheduler: ConfigsBuilder => Resource[F, Scheduler]
    def withScheduler(scheduler: ConfigsBuilder => Resource[F, Scheduler]): FinalPhase = next(scheduler)
    def withDefaultScheduler: FinalPhase                                               = next(defaultScheduler)
  }

  trait ConfigsBuilderPhase {
    protected def next(
        build: (
            KinesisAsyncClient,
            DynamoDbAsyncClient,
            CloudWatchAsyncClient,
            KinesisConsumerSettings
        ) => Resource[F, ConfigsBuilder]
    ): SingleStreamTrackerPhase

    def withConfigsBuilder(
        configsBuilder: (
            KinesisAsyncClient,
            DynamoDbAsyncClient,
            CloudWatchAsyncClient,
            KinesisConsumerSettings
        ) => Resource[F, ConfigsBuilder]
    ): SingleStreamTrackerPhase = next(configsBuilder)
  }

  trait SchedulerIdPhase {
    protected def next(build: Resource[F, SchedulerId]): StreamTrackerJunctionPhase
    protected def defaultSchedulerId: Resource[F, SchedulerId]
    def withSchedulerId(schedulerId: Resource[F, SchedulerId]): StreamTrackerJunctionPhase = next(schedulerId)
    def withDefaultSchedulerId: StreamTrackerJunctionPhase                                 = next(defaultSchedulerId)
  }

  trait StreamTrackerJunctionPhase {
    protected def next(streamName: Resource[F, StreamName]): SingleStreamTrackerPhase
    protected def nextMulti: MultiStreamTrackerPhase

    def withMultiStreamTracker: MultiStreamTrackerPhase                           = nextMulti
    def withSingleStreamTracker(streamName: StreamName): SingleStreamTrackerPhase = next(Resource.pure(streamName))
    def withSingleStreamTracker(streamName: Resource[F, StreamName]): SingleStreamTrackerPhase = next(streamName)

  }

  trait SingleStreamTrackerPhase {

    protected def next(build: StreamName => Resource[F, SingleStreamTracker]): SchedulerConfigPhase
    protected def defaultStreamTracker: StreamName => Resource[F, SingleStreamTracker]
    def withStreamTracker(streamTracker: StreamName => Resource[F, SingleStreamTracker]): SchedulerConfigPhase = next(
      streamTracker
    )
    def withDefaultStreamTracker: SchedulerConfigPhase = next(defaultStreamTracker)
  }

  trait MultiStreamTrackerPhase {

    protected def next(streamTracker: Resource[F, MultiStreamTracker]): SchedulerConfigPhase

    def withStreamTracker(streamTracker: Resource[F, MultiStreamTracker]): SchedulerConfigPhase = next(
      streamTracker
    )
  }

  trait FinalPhase {
    def build: Resource[F, Stream[F, Chunk[CommittableRecord]]]
  }
}
