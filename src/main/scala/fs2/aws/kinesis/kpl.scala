package fs2
package aws
package kinesis

import com.google.common.util.concurrent.{ListenableFuture, FutureCallback, Futures}
import cats.effect.{Effect, Concurrent}
import fs2.aws.internal.Internal._
import com.amazonaws.services.kinesis.producer.UserRecordResult
import scala.concurrent.ExecutionContext


package object publisher {
  def writeToKinesis[F[_]](
    streamName: String,
    partitionKey: String,
    producer: KinesisProducerClient[F] = new KinesisProducerClient[F] {}
  )(implicit F: Effect[F], ec: ExecutionContext, concurrent: Concurrent[F]): fs2.Pipe[F, Byte, Either[Throwable, UserRecordResult]] = {

    def write: fs2.Pipe[F, List[Byte], ListenableFuture[UserRecordResult]] =
      _.flatMap { case byteArray =>
        fs2.Stream.eval(producer.putData(streamName, partitionKey, byteArray))
      }

    def registerCallback: fs2.Pipe[F, ListenableFuture[UserRecordResult], UserRecordResult] =
      _.mapAsync(1) { case f =>
        Effect[F].async[UserRecordResult] { cb =>
          Futures.addCallback(
            f,
            new FutureCallback[UserRecordResult] {
              override def onFailure(t: Throwable): Unit = cb(Left(t))
              override def onSuccess(result: UserRecordResult): Unit = cb(Right(result))
            },
            (command: Runnable) => ec.execute(command))
        }
      }

    _.chunks
      .flatMap(m => fs2.Stream(m.toList)) // Flatten chunks to list of bytes
      .fold[List[Byte]](List())(_ ++ _) // Drain the source completely so that all bytes are aggregated into a single List
      .through(write)
      .through(registerCallback)
      .attempt
  }
}
