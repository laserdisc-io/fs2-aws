package fs2.aws.dynamodb

import cats.effect.kernel.Resource
import cats.effect.std.{Dispatcher, Queue}
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBStreams}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{KinesisClientLibConfiguration, Worker}
import fs2.Stream
import fs2.aws.dynamodb.models.DynamoDBModels.BufferSize

abstract class DynamoDBStreamBuilder[F[_]] {

  trait InitialPhase {

    protected def next(
        kinesisClientConf: Resource[F, KinesisClientLibConfiguration]
    ): DynamoDBStreamsClientPhase

    def withKinesisClientConf(kinesisClientConf: KinesisClientLibConfiguration): DynamoDBStreamsClientPhase = next(
      Resource.pure(kinesisClientConf)
    )
    def withKinesisClientConf(
        kinesisClientConf: Resource[F, KinesisClientLibConfiguration]
    ): DynamoDBStreamsClientPhase = next(
      kinesisClientConf
    )

  }

  trait DynamoDBStreamsClientPhase {

    protected def next(build: Resource[F, AmazonDynamoDBStreams]): DynamoDBClientPhase

    def withDynamoDBStreams(kinesisClient: Resource[F, AmazonDynamoDBStreams]): DynamoDBClientPhase = next(
      kinesisClient
    )
    def withDynamoDBStreams(kinesisClient: AmazonDynamoDBStreams): DynamoDBClientPhase = next(
      Resource.pure(kinesisClient)
    )
  }

  trait DynamoDBClientPhase {

    protected def next(build: Resource[F, AmazonDynamoDB]): CloudWatchClientPhase

    def withDynamoDBClient(dynamoDBClient: Resource[F, AmazonDynamoDB]): CloudWatchClientPhase = next(
      dynamoDBClient
    )

    def withDynamoDBClient(dynamoDBClient: AmazonDynamoDB): CloudWatchClientPhase = next(
      Resource.pure(dynamoDBClient)
    )
  }

  trait CloudWatchClientPhase {
    protected def next(build: Resource[F, AmazonCloudWatch]): ProcessorFactoryPhase
    def withCloudWatchClient(cloudWatchClient: Resource[F, AmazonCloudWatch]): ProcessorFactoryPhase = next(
      cloudWatchClient
    )
    def withCloudWatchClient(cloudWatchClient: AmazonCloudWatch): ProcessorFactoryPhase = next(
      Resource.pure(cloudWatchClient)
    )
  }

  trait ProcessorFactoryPhase {
    protected def next(
        processorFactory: (
            Dispatcher[F],
            Queue[F, CommittableRecord]
        ) => Resource[F, IRecordProcessorFactory]
    ): BufferSizePhase

    def withProcessorFactory(
        processorFactory: (
            Dispatcher[F],
            Queue[F, CommittableRecord]
        ) => Resource[F, IRecordProcessorFactory]
    ): BufferSizePhase = next(processorFactory)

    protected def defaultRecordProcessor: (
        Dispatcher[F],
        Queue[F, CommittableRecord]
    ) => Resource[F, IRecordProcessorFactory]
    def withDefaultRecordProcessor: BufferSizePhase = next(defaultRecordProcessor)
  }

  trait BufferSizePhase {
    protected def next(build: Resource[F, BufferSize]): SchedulerPhase
    protected def defaultBufferSize: Resource[F, BufferSize]
    def withBufferSize(bufferSize: Resource[F, BufferSize]): SchedulerPhase = next(bufferSize)
    def withDefaultBufferSize: SchedulerPhase                               = next(defaultBufferSize)
  }

  trait SchedulerPhase {

    protected def next(
        scheduler: (
            KinesisClientLibConfiguration,
            AmazonDynamoDBStreams,
            AmazonDynamoDB,
            AmazonCloudWatch,
            IRecordProcessorFactory
        ) => Resource[F, Worker]
    ): FinalPhase
    protected def defaultScheduler: (
        KinesisClientLibConfiguration,
        AmazonDynamoDBStreams,
        AmazonDynamoDB,
        AmazonCloudWatch,
        IRecordProcessorFactory
    ) => Resource[F, Worker]
    def withScheduler(
        scheduler: (
            KinesisClientLibConfiguration,
            AmazonDynamoDBStreams,
            AmazonDynamoDB,
            AmazonCloudWatch,
            IRecordProcessorFactory
        ) => Resource[F, Worker]
    ): FinalPhase = next(scheduler)
    def withDefaultScheduler: FinalPhase = next(defaultScheduler)
  }

  trait FinalPhase {
    def build: Resource[F, Stream[F, CommittableRecord]]
  }
}
