package fs2
package aws
package kinesis

import fs2.internal._
import fs2.concurrent.Queue
import cats.effect.{ConcurrentEffect, IO, Timer}
import cats.implicits._

import com.amazonaws.services.kinesis.model.Record
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory

package object kcl {
  /** Intialize a worker and start streaming records from a Kinesis stream
    * On stream finish (due to error or other), worker will be shutdown
    *
    *  @tparam F effect type of the fs2 stream
    *  @param workerFactory function to create a Worker from a IRecordProcessorFactory
    *  @param config configuration settings for the KCL worker
    *  @return an infinite fs2 Stream that emits Kinesis Records
    */
  def readFromKinesisStream[F[_]](
    workerFactory: => IRecordProcessorFactory => Worker,
    config: KinesisWorkerStreamSettings = KinesisWorkerStreamSettings.defaultInstance
  )(implicit F: ConcurrentEffect[F]): fs2.Stream[F, CommittableRecord] = {

    // Initialize a KCL worker which appends to the internal stream queue on message receipt
    def instantiateWorker(queue: Queue[F, CommittableRecord]): F[Worker] = F.delay {
      workerFactory(
        new IRecordProcessorFactory {
          override def createProcessor() =
            new RecordProcessor(record => F.runAsync(queue.enqueue1(record))(_ => IO.unit).unsafeRunSync)
        }
      )
    }

    // Instantiate a new bounded queue and concurrently run the queue populator
    // Expose the elements by dequeuing the internal buffer
    for {
      buffer <- Stream.eval(Queue.bounded[F, CommittableRecord](config.bufferSize))
      worker = instantiateWorker(buffer)
      stream <- buffer.dequeue concurrently Stream.eval(worker.map(_.run)) onFinalize(worker.map(_.shutdown))
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
    checkpointSettings: KinesisWorkerCheckpointSettings = KinesisWorkerCheckpointSettings.defaultInstance
  )(implicit F: ConcurrentEffect[F], timer: Timer[F]): fs2.Pipe[F, CommittableRecord, Record] = {
    _.through(groupBy(r => F.delay(r.shardId)))
      .map { case (k, st) =>
        st.tupleLeft(k)
          .groupWithin(checkpointSettings.maxBatchSize, checkpointSettings.maxBatchWait)
          .map(_.toList.max)
          .mapAsync(1) { cr =>
            F.async[Record](_ =>
              if (cr._2.canCheckpoint)
                Right(cr._2.checkpoint)
              else
                Left(throw new Exception("Record processor has been shutdown - cannot checkpoint records"))
            )
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
    checkpointSettings: KinesisWorkerCheckpointSettings = KinesisWorkerCheckpointSettings.defaultInstance
  )(implicit F: ConcurrentEffect[F], timer: Timer[F]): fs2.Sink[F, CommittableRecord] = {
    _.through(checkpointRecords(checkpointSettings))
      .map(_ => ())
  }
}
