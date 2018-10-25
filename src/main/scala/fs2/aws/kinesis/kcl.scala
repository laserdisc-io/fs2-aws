package fs2
package aws
package kinesis

import fs2.internal._
import fs2.concurrent.Queue
import cats.effect.{ConcurrentEffect, IO, Timer}
import cats.implicits._

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker
import com.amazonaws.services.kinesis.model.Record

package object kcl {
  /** Intialize a worker and start streaming records from a Kinesis stream
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
    def instantiateWorker(queue: Queue[F, CommittableRecord]) = F.delay {
      workerFactory(
        new IRecordProcessorFactory {
          override def createProcessor() = {
            new RecordProcessor(record => F.runAsync(queue.enqueue1(record))(_ => IO.unit).unsafeRunSync)
          }
        }
      ).run
    }

    // Instantiate a new bounded queue and concurrently run the queue populator and expose the elements
    // via a dequeuer
    Stream.eval(Queue.bounded[F, CommittableRecord](config.bufferSize)) flatMap { buffer =>
      val worker = Stream.eval(instantiateWorker(buffer)).drain
      buffer.dequeue concurrently worker
    }
  }

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
              if (cr._2.canCheckpoint) {
                Right(cr._2.checkpoint)
              }
              else
                Left(throw new Exception("record processor shutdown"))
            )
          }
      }.parJoinUnbounded
  }

  def checkpointRecords_[F[_]](
    checkpointSettings: KinesisWorkerCheckpointSettings = KinesisWorkerCheckpointSettings.defaultInstance
  )(implicit F: ConcurrentEffect[F], timer: Timer[F]): fs2.Sink[F, CommittableRecord] = {
    _.through(checkpointRecords(checkpointSettings))
      .map(_ => ())
  }
}
