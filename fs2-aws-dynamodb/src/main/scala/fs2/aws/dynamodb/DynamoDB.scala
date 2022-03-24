package fs2.aws.dynamodb

import cats.effect.std.{ Dispatcher, Queue }
import cats.effect.{ Async, Concurrent, Sync }
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.dynamodbv2.streamsadapter.{
  AmazonDynamoDBStreamsAdapterClient,
  StreamsWorkerFactory
}
import com.amazonaws.services.dynamodbv2.{ AmazonDynamoDB, AmazonDynamoDBStreams }
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{
  InitialPositionInStream,
  KinesisClientLibConfiguration,
  Worker
}
import com.amazonaws.services.kinesis.model.Record
import fs2.aws.core
import fs2.concurrent.SignallingRef
import fs2.{ Pipe, Stream }

trait DynamoDB[F[_]] {

  /** Initialize a worker and start streaming records from a DynamoDB stream
    * On stream finish (due to error or other), worker will be shutdown
    *
    * @tparam F effect type of the fs2 stream
    * @param appName    name of the DynamoDB application. Used by KCL when resharding
    * @param streamName name of the DynamoDB stream to consume from
    * @return an infinite fs2 Stream that emits Kinesis Records
    */
  def readFromDynamoDBStream(appName: String, streamName: String): Stream[F, CommittableRecord]

  /** Initialize a worker and start streaming records from a DynamoDB stream
    * On stream finish (due to error or other), worker will be shutdown
    *
    * @tparam F effect type of the fs2 stream
    * @param consumerConfig configuration parameters for the KCL
    * @return an infinite fs2 Stream that emits DynamoDB Records
    */
  def readFromDynamoDBStream(
    workerConfiguration: KinesisClientLibConfiguration
  ): Stream[F, CommittableRecord]

  /** Pipe to checkpoint records in Kinesis, marking them as processed
    * Groups records by shard id, so that each shard is subject to its own clustering of records
    * After accumulating maxBatchSize or reaching maxBatchWait for a respective shard, the latest record is checkpointed
    * By design, all records prior to the checkpointed record are also checkpointed in Kinesis
    *
    * @tparam F effect type of the fs2 stream
    * @param checkpointSettings configure maxBatchSize and maxBatchWait time before triggering a checkpoint
    * @return a stream of Record types representing checkpointed messages
    */
  def checkpointRecords(
    checkpointSettings: KinesisCheckpointSettings
  ): Pipe[F, CommittableRecord, Record]

}

object DynamoDB {

  abstract class GenericKinesis[F[_]: Async: Concurrent] extends DynamoDB[F] {

    private[dynamodb] def readChunksFromDynamoDBStream(
      schedulerFactory: IRecordProcessorFactory => F[Worker]
    ): Stream[F, CommittableRecord] = {
      // Initialize a KCL scheduler which appends to the internal stream queue on message receipt
      def instantiateScheduler(
        dispatcher: Dispatcher[F],
        queue: Queue[F, CommittableRecord],
        signal: SignallingRef[F, Boolean]
      ): fs2.Stream[F, Worker] =
        Stream.bracket {
          schedulerFactory(() =>
            new RecordProcessor(records => dispatcher.unsafeRunSync(queue.offer(records)))
          ).flatTap(s =>
              Concurrent[F].start(Async[F].blocking(s.run()).flatTap(_ => signal.set(true)))
            )
        }(s => Async[F].blocking(s.shutdown()))

      // Instantiate a new bounded queue and concurrently run the queue populator
      // Expose the elements by dequeuing the internal buffer
      for {
        dispatcher      <- Stream.resource(Dispatcher[F])
        buffer          <- Stream.eval(Queue.unbounded[F, CommittableRecord])
        interruptSignal <- Stream.eval(SignallingRef[F, Boolean](false))
        _               <- instantiateScheduler(dispatcher, buffer, interruptSignal)
        stream          <- Stream.fromQueueUnterminated(buffer).interruptWhen(interruptSignal)
      } yield stream
    }

    def checkpointRecords(
      checkpointSettings: KinesisCheckpointSettings // = KinesisCheckpointSettings.defaultInstance
    ): Pipe[F, CommittableRecord, Record] = {
      def checkpoint(
        checkpointSettings: KinesisCheckpointSettings
      ): Pipe[F, CommittableRecord, Record] =
        _.groupWithin(checkpointSettings.maxBatchSize, checkpointSettings.maxBatchWait)
          .collect { case chunk if chunk.size > 0 => chunk.size -> chunk.toList.max }
          .flatMap { case (len, cr) => fs2.Stream.eval(cr.checkpoint[F](len).as(cr.record)).drain }

      def bypass: Pipe[F, CommittableRecord, Record] = _.map(r => r.record)

      _.through(core.groupBy(r => Sync[F].pure(r.shardId))).map {
        case (_, st) =>
          st.broadcastThrough(checkpoint(checkpointSettings), bypass)
      }.parJoinUnbounded
    }
  }

  def create[F[_]: Async](
    schedulerFactory: IRecordProcessorFactory => F[Worker]
  ): DynamoDB[F] = new GenericKinesis[F] {

    override def readFromDynamoDBStream(
      appName: String,
      streamName: String
    ): Stream[F, CommittableRecord] = readChunksFromDynamoDBStream(schedulerFactory)

    override def readFromDynamoDBStream(
      workerConfiguration: KinesisClientLibConfiguration
    ): Stream[F, CommittableRecord] = readChunksFromDynamoDBStream(schedulerFactory)
  }

  def create[F[_]: Async: Concurrent](
    dynamoDBStreamsClient: AmazonDynamoDBStreams,
    dynamoDBClient: AmazonDynamoDB,
    cloudWatchClient: AmazonCloudWatch
  ): DynamoDB[F] = {

    def defaultScheduler(
      workerConfiguration: KinesisClientLibConfiguration,
      dynamoDBStreamsClient: AmazonDynamoDBStreams,
      dynamoDBClient: AmazonDynamoDB,
      cloudWatchClient: AmazonCloudWatch
    ): IRecordProcessorFactory => F[Worker] = { (recordProcessorFactory: IRecordProcessorFactory) =>
      val adapterClient = new AmazonDynamoDBStreamsAdapterClient(dynamoDBStreamsClient)
      StreamsWorkerFactory
        .createDynamoDbStreamsWorker(
          recordProcessorFactory,
          workerConfiguration,
          adapterClient,
          dynamoDBClient,
          cloudWatchClient
        )
        .pure[F]
    }

    new GenericKinesis[F] {
      override def readFromDynamoDBStream(
        appName: String,
        streamName: String
      ): Stream[F, CommittableRecord] =
        for {
          provider <- Stream.eval(Sync[F].delay(DefaultAWSCredentialsProviderChain.getInstance()))
          workerId <- Stream.eval(Sync[F].delay(java.util.UUID.randomUUID().toString))
          conf <- Stream.eval(
                   Sync[F].delay(
                     new KinesisClientLibConfiguration(
                       appName,
                       streamName,
                       provider,
                       workerId
                     ).withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON)
                   )
                 )
          res <- readFromDynamoDBStream(conf)

        } yield res

      override def readFromDynamoDBStream(
        workerConfiguration: KinesisClientLibConfiguration
      ): Stream[F, CommittableRecord] =
        readChunksFromDynamoDBStream(
          defaultScheduler(
            workerConfiguration,
            dynamoDBStreamsClient,
            dynamoDBClient,
            cloudWatchClient
          )
        )
    }
  }
}
