package fs2
package aws
package kinesis

import cats.effect.{Effect, Concurrent}
import fs2.aws.internal.Internal._
import com.google.common.util.concurrent.{ListenableFuture, FutureCallback, Futures}
import com.amazonaws.services.kinesis.producer.UserRecordResult
import scala.concurrent.ExecutionContext

/**
  *  fs2 Streams for publishing data to AWS Kinesis streams
  */
package object publisher {

  /** Writes the bytestream to a Kinesis stream via a Pipe
    *
    *  @tparam F effect type of the stream
    *  @param streamName the name of the Kinesis stream to write to
    *  @param partitionKey the partitionKey to use to determine which shard to write to
    *  @param producer kinesis producer client to use
    *  @return a Pipe that accepts a stream of bytes and returns UserRecordResults
    */
  def writeToKinesis[F[_]](
    streamName: String,
    partitionKey: String,
    producer: KinesisProducerClient[F] = new KinesisProducerClient[F] {}
  )(implicit F: Effect[F], ec: ExecutionContext, concurrent: Concurrent[F]): fs2.Pipe[F, Byte, UserRecordResult] = {

    // Evaluate the operation of invoking the Kinesis client
    def write: fs2.Pipe[F, List[Byte], ListenableFuture[UserRecordResult]] =
      _.flatMap { case byteArray =>
        fs2.Stream.eval(producer.putData(streamName, partitionKey, byteArray))
      }

    // Register the returned future, returning the UserRecordResult
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
  }

  /** Writes the bytestream to a Kinesis stream via a Sink
    *
    *  @tparam F effect type of the stream
    *  @param streamName the name of the Kinesis stream to write to
    *  @param partitionKey the partitionKey to use to determine which shard to write to
    *  @param producer kinesis producer client to use
    *  @return a Sink that accepts a stream of bytes
    */
  def writeToKinesis_[F[_]](
    streamName: String,
    partitionKey: String,
    producer: KinesisProducerClient[F] = new KinesisProducerClient[F] {}
  )(implicit F: Effect[F], ec: ExecutionContext): fs2.Sink[F, Byte] = {

    // Evaluate the operation of invoking the Kinesis client
    def write: fs2.Pipe[F, List[Byte], ListenableFuture[UserRecordResult]] =
      _.flatMap { case byteArray =>
        fs2.Stream.eval(producer.putData(streamName, partitionKey, byteArray))
      }

    // Register the returned future, returning Unit
    def registerCallback: fs2.Sink[F, ListenableFuture[UserRecordResult]] =
      _.map { case f =>
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
  }
}
