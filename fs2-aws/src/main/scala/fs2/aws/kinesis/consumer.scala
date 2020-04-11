package fs2.aws.kinesis

import java.util.UUID

import cats.effect.{ Blocker, ConcurrentEffect, ContextShift, IO, Sync, Timer }
import cats.implicits._

import fs2.aws.core
import fs2.{ Chunk, Pipe, RaiseThrowable, Stream }
import fs2.aws.internal.Exceptions.KinesisCheckpointException
import fs2.concurrent.Queue

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest
import software.amazon.kinesis.common.{ ConfigsBuilder, InitialPositionInStreamExtended }
import software.amazon.kinesis.coordinator.Scheduler
import software.amazon.kinesis.processor.ShardRecordProcessorFactory
import software.amazon.kinesis.retrieval.KinesisClientRecord

object consumer {
  def mkDefaultKinesisClient(settings: KinesisConsumerSettings): KinesisAsyncClient =
    KinesisAsyncClient
      .builder()
      .region(settings.region)
      .credentialsProvider(
        settings.stsAssumeRole
          .map(stsSettings =>
            StsAssumeRoleCredentialsProvider
              .builder()
              .stsClient(StsClient.builder.build())
              .refreshRequest(
                AssumeRoleRequest
                  .builder()
                  .roleArn(stsSettings.roleArn)
                  .roleSessionName(stsSettings.roleSessionName)
                  .build()
              )
              .build()
          )
          .getOrElse(DefaultCredentialsProvider.create())
      )
      .httpClientBuilder(NettyNioAsyncHttpClient.builder().maxConcurrency(settings.maxConcurrency))
      .build()

  private def defaultScheduler(
    recordProcessorFactory: ShardRecordProcessorFactory,
    settings: KinesisConsumerSettings,
    kinesisClient: KinesisAsyncClient
  ): Scheduler = {

    val dynamoClient: DynamoDbAsyncClient =
      DynamoDbAsyncClient.builder().region(settings.region).build()
    val cloudWatchClient: CloudWatchAsyncClient =
      CloudWatchAsyncClient.builder().region(settings.region).build()

    val configsBuilder: ConfigsBuilder = new ConfigsBuilder(
      settings.streamName,
      settings.appName,
      kinesisClient,
      dynamoClient,
      cloudWatchClient,
      UUID.randomUUID().toString,
      recordProcessorFactory
    )

    val retrievalConfig = configsBuilder.retrievalConfig()
    retrievalConfig.initialPositionInStreamExtended(
      settings.initialPositionInStream match {
        case Left(position) =>
          InitialPositionInStreamExtended.newInitialPosition(position)

        case Right(date) =>
          InitialPositionInStreamExtended.newInitialPositionAtTimestamp(date)
      }
    )

    new Scheduler(
      configsBuilder.checkpointConfig(),
      configsBuilder.coordinatorConfig(),
      configsBuilder.leaseManagementConfig(),
      configsBuilder.lifecycleConfig(),
      configsBuilder.metricsConfig(),
      configsBuilder.processorConfig(),
      retrievalConfig
    )
  }

  /** Intialize a worker and start streaming records from a Kinesis stream
    * On stream finish (due to error or other), worker will be shutdown
    *
    * @tparam F effect type of the fs2 stream
    * @param appName    name of the Kinesis application. Used by KCL when resharding
    * @param streamName name of the Kinesis stream to consume from
    * @param region     AWS region in which the Kinesis stream resides
    * @return an infinite fs2 Stream that emits Kinesis Records
    */
  def readFromKinesisStream[F[_]: ConcurrentEffect: ContextShift](
    appName: String,
    streamName: String,
    region: Region
  )(implicit rt: RaiseThrowable[F]): Stream[F, CommittableRecord] =
    KinesisConsumerSettings.build(streamName, appName, region) match {
      case Right(config) => readFromKinesisStream(config)
      case Left(err)     => Stream.raiseError(err)
    }

  /** Initialize a worker and start streaming records from a Kinesis stream
    * On stream finish (due to error or other), worker will be shutdown
    *
    * @tparam F effect type of the fs2 stream
    * @param consumerConfig configuration parameters for the KCL
    * @return an infinite fs2 Stream that emits Kinesis Records
    */
  def readFromKinesisStream[F[_]: ConcurrentEffect: ContextShift](
    consumerConfig: KinesisConsumerSettings
  ): Stream[F, CommittableRecord] =
    readFromKinesisStream(
      consumerConfig,
      defaultScheduler(_, consumerConfig, mkDefaultKinesisClient(consumerConfig))
    )

  /** Intialize a worker and start streaming records from a Kinesis stream
    * On stream finish (due to error or other), worker will be shutdown
    *
    * @tparam F effect type of the fs2 stream
    * @param appName    name of the Kinesis application. Used by KCL when resharding
    * @param streamName name of the Kinesis stream to consume from
    * @param region     AWS region in which the Kinesis stream resides
    * @param kinesisClient preconfigured kineiss klient, usefull when you need STS access
    * @return an infinite fs2 Stream that emits Kinesis Records
    */
  def readFromKinesisStream[F[_]: ConcurrentEffect: ContextShift](
    appName: String,
    streamName: String,
    region: Region,
    kinesisClient: KinesisAsyncClient
  )(implicit rt: RaiseThrowable[F]): Stream[F, CommittableRecord] =
    KinesisConsumerSettings.build(streamName, appName, region) match {
      case Right(config) => readFromKinesisStream(config, kinesisClient)
      case Left(err)     => Stream.raiseError(err)
    }

  /** Initialize a worker and start streaming records from a Kinesis stream
    * On stream finish (due to error or other), worker will be shutdown
    *
    * @tparam F effect type of the fs2 stream
    * @param consumerConfig configuration parameters for the KCL
    * @param kinesisClient preconfigured kineiss klient, usefull when you need STS access
    * @return an infinite fs2 Stream that emits Kinesis Records
    */
  def readFromKinesisStream[F[_]: ConcurrentEffect: ContextShift](
    consumerConfig: KinesisConsumerSettings,
    kinesisClient: KinesisAsyncClient
  ): Stream[F, CommittableRecord] =
    readFromKinesisStream(consumerConfig, defaultScheduler(_, consumerConfig, kinesisClient))

  /** Initialize a worker and start streaming records from a Kinesis stream
    * On stream finish (due to error or other), worker will be shutdown
    *
    * @tparam F effect type of the fs2 stream
    * @param consumerConfig configuration parameters for the KCL
    * @return an infinite fs2 Stream that emits Kinesis Records Chunks
    */
  def readChunkedFromKinesisStream[F[_]: ConcurrentEffect: ContextShift](
    consumerConfig: KinesisConsumerSettings
  ): Stream[F, Chunk[CommittableRecord]] =
    readChunksFromKinesisStream(
      consumerConfig,
      defaultScheduler(_, consumerConfig, mkDefaultKinesisClient(consumerConfig))
    )

  /** Initialize a worker and start streaming records from a Kinesis stream
    * On stream finish (due to error or other), worker will be shutdown
    *
    * @tparam F effect type of the fs2 stream
    * @param consumerConfig configuration parameters for the KCL
    * @param kinesisClient preconfigured kineiss klient, usefull when you need STS access
    * @return an infinite fs2 Stream that emits Kinesis Records Chunks
    */
  def readChunkedFromKinesisStream[F[_]: ConcurrentEffect: ContextShift](
    consumerConfig: KinesisConsumerSettings,
    kinesisClient: KinesisAsyncClient
  ): Stream[F, Chunk[CommittableRecord]] =
    readChunksFromKinesisStream(consumerConfig, defaultScheduler(_, consumerConfig, kinesisClient))

  /** Intialize a worker and start streaming records from a Kinesis stream
    * On stream finish (due to error or other), worker will be shutdown
    *
    * @tparam F effect type of the fs2 stream
    * @param workerFactory function to create a Worker from a IRecordProcessorFactory
    * @param streamConfig  configuration for the internal stream
    * @return an infinite fs2 Stream that emits Kinesis Records
    */
  private[aws] def readFromKinesisStream[F[_]: ConcurrentEffect: ContextShift](
    streamConfig: KinesisConsumerSettings,
    workerFactory: ShardRecordProcessorFactory => Scheduler
  ): Stream[F, CommittableRecord] = {

    // Initialize a KCL worker which appends to the internal stream queue on message receipt
    def instantiateWorker(queue: Queue[F, CommittableRecord]): Stream[F, Scheduler] = Stream.emit {
      workerFactory(() =>
        new SingleRecordProcessor(
          record =>
            ConcurrentEffect[F].runAsync(queue.enqueue1(record))(_ => IO.unit).unsafeRunSync,
          streamConfig.terminateGracePeriod
        )
      )
    }

    // Instantiate a new bounded queue and concurrently run the queue populator
    // Expose the elements by dequeuing the internal buffer
    for {
      buffer <- Stream.eval(Queue.bounded[F, CommittableRecord](streamConfig.bufferSize))
      worker <- instantiateWorker(buffer)
      stream <- buffer.dequeue concurrently Stream.eval(
                 Blocker[F].use(blocker => blocker.delay(worker.run()))
               ) onFinalize Sync[F].delay(worker.shutdown())
    } yield stream
  }

  private[aws] def readChunksFromKinesisStream[F[_]: ConcurrentEffect: ContextShift](
    streamConfig: KinesisConsumerSettings,
    workerFactory: =>ShardRecordProcessorFactory => Scheduler
  ): Stream[F, Chunk[CommittableRecord]] = {

    // Initialize a KCL worker which appends to the internal stream queue on message receipt
    def instantiateWorker(queue: Queue[F, Chunk[CommittableRecord]]): fs2.Stream[F, Scheduler] =
      Stream.emit(
        workerFactory(() =>
          new ChunkedRecordProcessor(
            records =>
              ConcurrentEffect[F].runAsync(queue.enqueue1(records))(_ => IO.unit).unsafeRunSync,
            streamConfig.terminateGracePeriod
          )
        )
      )

    // Instantiate a new bounded queue and concurrently run the queue populator
    // Expose the elements by dequeuing the internal buffer
    for {
      buffer <- Stream.eval(Queue.bounded[F, Chunk[CommittableRecord]](streamConfig.bufferSize))
      worker <- instantiateWorker(buffer)
      stream <- buffer.dequeue concurrently Stream.eval(
                 Blocker[F].use(blocker => blocker.delay(worker.run()))
               ) onFinalize Sync[F].delay(worker.shutdown())
    } yield stream
  }

  /** Pipe to checkpoint records in Kinesis, marking them as processed
    * Groups records by shard id, so that each shard is subject to its own clustering of records
    * After accumulating maxBatchSize or reaching maxBatchWait for a respective shard, the latest record is checkpointed
    * By design, all records prior to the checkpointed record are also checkpointed in Kinesis
    *
    * @tparam F effect type of the fs2 stream
    * @param checkpointSettings configure maxBatchSize and maxBatchWait time before triggering a checkpoint
    * @return a stream of Record types representing checkpointed messages
    */
  def checkpointRecords[F[_]](
    checkpointSettings: KinesisCheckpointSettings = KinesisCheckpointSettings.defaultInstance,
    parallelism: Int = 10
  )(
    implicit F: ConcurrentEffect[F],
    timer: Timer[F]
  ): Pipe[F, CommittableRecord, KinesisClientRecord] = {
    def checkpoint(checkpointSettings: KinesisCheckpointSettings, parallelism: Int)(
      implicit F: ConcurrentEffect[F],
      timer: Timer[F]
    ): Pipe[F, CommittableRecord, KinesisClientRecord] =
      _.groupWithin(checkpointSettings.maxBatchSize, checkpointSettings.maxBatchWait)
        .collect { case chunk if chunk.size > 0 => chunk.toList.max }
        .flatMap { cr =>
          fs2.Stream.eval_(
            F.async[KinesisClientRecord] { cb =>
              if (cr.canCheckpoint) {
                cr.checkpoint()
                cb(Right(cr.record))
              } else {
                cb(
                  Left(
                    KinesisCheckpointException(
                      "Record processor has been shutdown and therefore cannot checkpoint records"
                    )
                  )
                )
              }
            }
          )
        }

    def bypass: Pipe[F, CommittableRecord, KinesisClientRecord] = _.map(r => r.record)

    _.through(core.groupBy(r => F.delay(r.shardId))).map {
      case (_, st) =>
        st.broadcastThrough(checkpoint(checkpointSettings, parallelism), bypass)
    }.parJoinUnbounded
  }

  /** Sink to checkpoint records in Kinesis, marking them as processed
    * Groups records by shard id, so that each shard is subject to its own clustering of records
    * After accumulating maxBatchSize or reaching maxBatchWait for a respective shard, the latest record is checkpointed
    * By design, all records prior to the checkpointed record are also checkpointed in Kinesis
    *
    * @tparam F effect type of the fs2 stream
    * @param checkpointSettings configure maxBatchSize and maxBatchWait time before triggering a checkpoint
    * @return a Sink that accepts a stream of CommittableRecords
    */
  def checkpointRecords_[F[_]](
    checkpointSettings: KinesisCheckpointSettings = KinesisCheckpointSettings.defaultInstance
  )(implicit F: ConcurrentEffect[F], timer: Timer[F]): Pipe[F, CommittableRecord, Unit] =
    _.through(checkpointRecords(checkpointSettings)).void
}
