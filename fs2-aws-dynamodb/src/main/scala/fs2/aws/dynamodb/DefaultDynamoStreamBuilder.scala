package fs2.aws.dynamodb

import cats.effect.kernel.Resource
import cats.effect.std.{Dispatcher, Queue}
import cats.effect.{Async, Concurrent, Sync}
import cats.syntax.either.*
import cats.syntax.flatMap.*
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.dynamodbv2.streamsadapter.{AmazonDynamoDBStreamsAdapterClient, StreamsWorkerFactory}
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBStreams}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{KinesisClientLibConfiguration, Worker}
import fs2.Stream
import fs2.aws.dynamodb.models.DynamoDBModels.BufferSize
import fs2.concurrent.SignallingRef

object DefaultDynamoStreamBuilder {
  def apply[F[_]: Async: Concurrent](): DynamoDBStreamBuilder[F]#InitialPhase = {
    val ksb = new DefaultDynamoStreamBuilder[F]()
    new ksb.DefaultInitialPhase()
  }
}

class DefaultDynamoStreamBuilder[F[_]: Async: Concurrent] extends DynamoDBStreamBuilder[F] {
  private class DefaultInitialPhase extends InitialPhase {
    override protected def next(
        kinesisClientConf: Resource[F, KinesisClientLibConfiguration]
    ): DynamoDBStreamsClientPhase =
      new DefaultKinesisClientPhase(kinesisClientConf)
  }

  private class DefaultKinesisClientPhase(kinesisClientConf: Resource[F, KinesisClientLibConfiguration])
      extends DynamoDBStreamsClientPhase {
    override protected def next(ddbStreams: Resource[F, AmazonDynamoDBStreams]): DynamoDBClientPhase =
      new DefaultDynamoDBClientPhase(kinesisClientConf, ddbStreams)
  }

  private class DefaultDynamoDBClientPhase(
      kinesisClientConf: Resource[F, KinesisClientLibConfiguration],
      ddbStreams: Resource[F, AmazonDynamoDBStreams]
  ) extends DynamoDBClientPhase {
    override protected def next(dynamoDBClient: Resource[F, AmazonDynamoDB]): CloudWatchClientPhase =
      new DefaultCloudWatchClientPhase(kinesisClientConf, ddbStreams, dynamoDBClient)
  }

  private class DefaultCloudWatchClientPhase(
      kinesisClientConf: Resource[F, KinesisClientLibConfiguration],
      ddbStreams: Resource[F, AmazonDynamoDBStreams],
      dynamoDBClient: Resource[F, AmazonDynamoDB]
  ) extends CloudWatchClientPhase {
    override protected def next(cloudWatchClient: Resource[F, AmazonCloudWatch]): ProcessorFactoryPhase =
      new DefaultProcessorFactoryPhase(kinesisClientConf, ddbStreams, dynamoDBClient, cloudWatchClient)
  }

  final private class DefaultProcessorFactoryPhase(
      kinesisClientConf: Resource[F, KinesisClientLibConfiguration],
      ddbStreams: Resource[F, AmazonDynamoDBStreams],
      dynamoDBClient: Resource[F, AmazonDynamoDB],
      cloudWatchClient: Resource[F, AmazonCloudWatch]
  ) extends ProcessorFactoryPhase {
    override protected def next(
        processorFactory: (
            Dispatcher[F],
            Queue[F, CommittableRecord]
        ) => Resource[F, IRecordProcessorFactory]
    ): BufferSizePhase = new DefaultBufferSizePhase(
      kinesisClientConf,
      ddbStreams,
      dynamoDBClient,
      cloudWatchClient,
      processorFactory
    )

    override protected def defaultRecordProcessor: (
        Dispatcher[F],
        Queue[F, CommittableRecord]
    ) => Resource[F, IRecordProcessorFactory] = { case (dispatcher, queue) =>
      Resource.pure(
        new IRecordProcessorFactory() {
          override def createProcessor(): IRecordProcessor =
            new RecordProcessor(records => dispatcher.unsafeRunSync(queue.offer(records)))
        }
      )
    }
  }

  private class DefaultBufferSizePhase(
      kinesisClientConf: Resource[F, KinesisClientLibConfiguration],
      ddbStreams: Resource[F, AmazonDynamoDBStreams],
      dynamoDBClient: Resource[F, AmazonDynamoDB],
      cloudWatchClient: Resource[F, AmazonCloudWatch],
      processorFactory: (
          Dispatcher[F],
          Queue[F, CommittableRecord]
      ) => Resource[F, IRecordProcessorFactory]
  ) extends BufferSizePhase {
    override protected def next(bufferSize: Resource[F, BufferSize]): SchedulerPhase =
      new DefaultSchedulerPhase(
        kinesisClientConf,
        ddbStreams,
        dynamoDBClient,
        cloudWatchClient,
        bufferSize,
        processorFactory
      )

    override protected def defaultBufferSize: Resource[F, BufferSize] =
      Resource.eval(Sync[F].fromEither(BufferSize(10).leftMap(new IllegalArgumentException(_))))
  }

  private class DefaultSchedulerPhase(
      kinesisClientConf: Resource[F, KinesisClientLibConfiguration],
      ddbStreams: Resource[F, AmazonDynamoDBStreams],
      dynamoDBClient: Resource[F, AmazonDynamoDB],
      cloudWatchClient: Resource[F, AmazonCloudWatch],
      bufferSizeRes: Resource[F, BufferSize],
      processorFactory: (
          Dispatcher[F],
          Queue[F, CommittableRecord]
      ) => Resource[F, IRecordProcessorFactory]
  ) extends SchedulerPhase {
    override protected def next(
        s: (
            KinesisClientLibConfiguration,
            AmazonDynamoDBStreams,
            AmazonDynamoDB,
            AmazonCloudWatch,
            IRecordProcessorFactory
        ) => Resource[F, Worker]
    ): FinalPhase =
      new DefaultFinalPhase(for {
        kinesisConf <- kinesisClientConf
        ddbStreams  <- ddbStreams
        ddb         <- dynamoDBClient
        cw          <- cloudWatchClient
        dispatcher  <- Dispatcher.parallel[F]
        bufferSize  <- bufferSizeRes
        queue       <- Resource.eval(Queue.bounded[F, CommittableRecord](bufferSize))
        factory     <- processorFactory(dispatcher, queue)
        scheduler   <- s(kinesisConf, ddbStreams, ddb, cw, factory)
        signal      <- Resource.eval(SignallingRef[F, Boolean](false))
        _ <- Resource.make(Concurrent[F].start(Async[F].blocking(scheduler.run()).flatTap(_ => signal.set(true))))(_ =>
          Async[F].blocking(scheduler.shutdown())
        )

      } yield (scheduler, queue, signal))

    override protected def defaultScheduler: (
        KinesisClientLibConfiguration,
        AmazonDynamoDBStreams,
        AmazonDynamoDB,
        AmazonCloudWatch,
        IRecordProcessorFactory
    ) => Resource[F, Worker] = { case (config, streams, ddb, cw, factory) =>
      Resource.pure(
        StreamsWorkerFactory
          .createDynamoDbStreamsWorker(
            factory,
            config,
            new AmazonDynamoDBStreamsAdapterClient(streams),
            ddb,
            cw
          )
      )
    }
  }

  private class DefaultFinalPhase(
      scheduler: Resource[F, (Worker, Queue[F, CommittableRecord], SignallingRef[F, Boolean])]
  ) extends FinalPhase {
    override def build: Resource[F, Stream[F, CommittableRecord]] =
      scheduler.map { case (_, buffer, signal) =>
        Stream.fromQueueUnterminated(buffer).interruptWhen(signal)
      }
  }
}
