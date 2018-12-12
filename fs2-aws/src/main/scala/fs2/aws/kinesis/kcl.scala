package fs2
package aws
package kinesis

import internal.Internal._
import internal.Exceptions.KinesisCheckpointException
import fs2.concurrent.Queue
import cats.effect.{ConcurrentEffect, IO, Timer}
import cats.implicits._

import com.amazonaws.services.kinesis.model.Record
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{
  Worker,
  KinesisClientLibConfiguration
}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain

package object kcl {
  private def defaultWorker(
      recordProcessorFactory: IRecordProcessorFactory,
      workerConfiguration: KinesisClientLibConfiguration
  ): Worker = {
    new Worker.Builder()
      .recordProcessorFactory(recordProcessorFactory)
      .config(workerConfiguration)
      .build()
  }

  /** Intialize a worker and start streaming records from a Kinesis stream
    * On stream finish (due to error or other), worker will be shutdown
    *
    *  @tparam F effect type of the fs2 stream
    *  @param appName name of the Kinesis application. Used by KCL when resharding
    *  @param streamName name of the Kinesis stream to consume from
    *  @return an infinite fs2 Stream that emits Kinesis Records
    */
  def readFromKinesisStream[F[_]](
      appName: String,
      streamName: String
  )(implicit F: ConcurrentEffect[F]): fs2.Stream[F, CommittableRecord] = {
    val workerConfig = new KinesisClientLibConfiguration(
      appName,
      streamName,
      DefaultAWSCredentialsProviderChain.getInstance(),
      s"${
        import scala.sys.process._
        "hostname".!!.trim()
      }:${java.util.UUID.randomUUID()}"
    )

    readFromKinesisStream(workerConfig)
  }

  /** Intialize a worker and start streaming records from a Kinesis stream
    * On stream finish (due to error or other), worker will be shutdown
    *
    *  @tparam F effect type of the fs2 stream
    *  @param workerConfig custom configuration of the worker including app name, stream name, AWS credential provider, etc
    *  @param streamConfig configuration for the internal stream
    *  @return an infinite fs2 Stream that emits Kinesis Records
    */
  def readFromKinesisStream[F[_]](
      workerConfig: KinesisClientLibConfiguration,
      streamConfig: KinesisStreamSettings = KinesisStreamSettings.defaultInstance
  )(implicit F: ConcurrentEffect[F]): fs2.Stream[F, CommittableRecord] = {
    readFromKinesisStream(defaultWorker(_, workerConfig), streamConfig)
  }

  /** Intialize a worker and start streaming records from a Kinesis stream
    * On stream finish (due to error or other), worker will be shutdown
    *
    *  @tparam F effect type of the fs2 stream
    *  @param workerFactory function to create a Worker from a IRecordProcessorFactory
    *  @param streamConfig configuration for the internal stream
    *  @return an infinite fs2 Stream that emits Kinesis Records
    */
  private[aws] def readFromKinesisStream[F[_]](
      workerFactory: => IRecordProcessorFactory => Worker,
      streamConfig: KinesisStreamSettings
  )(implicit F: ConcurrentEffect[F]): fs2.Stream[F, CommittableRecord] = {

    // Initialize a KCL worker which appends to the internal stream queue on message receipt
    def instantiateWorker(queue: Queue[F, CommittableRecord]): F[Worker] = F.delay {
      workerFactory(
        new IRecordProcessorFactory {
          override def createProcessor() =
            new RecordProcessor(
              record => F.runAsync(queue.enqueue1(record))(_ => IO.unit).unsafeRunSync)
        }
      )
    }

    // Instantiate a new bounded queue and concurrently run the queue populator
    // Expose the elements by dequeuing the internal buffer
    for {
      buffer <- Stream.eval(Queue.bounded[F, CommittableRecord](streamConfig.bufferSize))
      worker = instantiateWorker(buffer)
      stream <- buffer.dequeue concurrently Stream.eval(worker.map(_.run)) onFinalize (worker.map(
        _.shutdown))
    } yield stream
  }

  /** Pipe to checkpoint records in Kinesis, marking them as processed
    * Groups records by shard id, so that each shard is subject to its own clustering of records
    * After accumulating maxBatchSize or reaching maxBatchWait for a respective shard, the latest record is checkpointed
    * By design, all records prior to the checkpointed record are also checkpointed in Kinesis
    *
    *  @tparam F effect type of the fs2 stream
    *  @param checkpointSettings configure maxBatchSize and maxBatchWait time before triggering a checkpoint
    *  @return a stream of Record types representing checkpointed messages
    */
  def checkpointRecords[F[_]](
      checkpointSettings: KinesisCheckpointSettings = KinesisCheckpointSettings.defaultInstance,
      parallelism: Int = 10
  )(implicit F: ConcurrentEffect[F], timer: Timer[F]): fs2.Pipe[F, CommittableRecord, Record] = {
    _.through(groupBy(r => F.delay(r.shardId))).map {
      case (k, st) =>
        st.groupWithin(checkpointSettings.maxBatchSize, checkpointSettings.maxBatchWait)
          .map(_.toList.max)
          .mapAsync(parallelism) { cr =>
            F.async[Record] { cb =>
              if (cr.canCheckpoint) {
                cr.checkpoint
                cb(Right(cr.record))
              } else {
                cb(
                  Left(new KinesisCheckpointException(
                    "Record processor has been shutdown and therefore cannot checkpoint records")))
              }
            }
          }
    }.parJoinUnbounded
  }

  /** Sink to checkpoint records in Kinesis, marking them as processed
    * Groups records by shard id, so that each shard is subject to its own clustering of records
    * After accumulating maxBatchSize or reaching maxBatchWait for a respective shard, the latest record is checkpointed
    * By design, all records prior to the checkpointed record are also checkpointed in Kinesis
    *
    *  @tparam F effect type of the fs2 stream
    *  @param checkpointSettings configure maxBatchSize and maxBatchWait time before triggering a checkpoint
    *  @return a Sink that accepts a stream of CommittableRecords
    */
  def checkpointRecords_[F[_]](
      checkpointSettings: KinesisCheckpointSettings = KinesisCheckpointSettings.defaultInstance
  )(implicit F: ConcurrentEffect[F], timer: Timer[F]): fs2.Sink[F, CommittableRecord] = {
    _.through(checkpointRecords(checkpointSettings))
      .map(_ => ())
  }
}
