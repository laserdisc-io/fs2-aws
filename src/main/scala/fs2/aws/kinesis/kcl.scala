package fs2
package aws
package kinesis

import fs2.concurrent.Queue
import cats.effect.{ConcurrentEffect, IO}

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker
import com.amazonaws.services.kinesis.model.Record


package object kcl {
  def readFromStream[F[_]](
    workerFactory: => IRecordProcessorFactory => Worker,
    config: KinesisWorkerSettings = KinesisWorkerSettings.defaultInstance
  )(implicit F: ConcurrentEffect[F]): fs2.Stream[F, Record] = {

    def instantiateWorker(queue: Queue[F, Record]) = F.delay {
      workerFactory(
        new IRecordProcessorFactory {
          override def createProcessor() = {
            new RecordProcessor(record => F.runAsync(queue.enqueue1(record))(_ => IO.unit).unsafeRunSync)
          }
        }
      ).run
    }

    Stream.eval(Queue.bounded[F, Record](config.bufferSize)) flatMap { buffer =>
      val worker = Stream.eval(instantiateWorker(buffer)).drain
      buffer.dequeue.take(1) concurrently worker
    }
  }
}
