package fs2
package aws
package kinesis

import java.nio.ByteBuffer
import cats.effect.{Effect, Concurrent}
import fs2.aws.internal.Internal._
import com.google.common.util.concurrent.{ListenableFuture, FutureCallback, Futures}
import com.amazonaws.services.kinesis.producer.UserRecordResult
import scala.concurrent.ExecutionContext

/**
  *  fs2 Streams for publishing data to AWS Kinesis streams
  */
package object publisher {

  /** Writes the (partitionKey, ByteBuffer) to a Kinesis stream via a Pipe
    *
    *  @tparam F effect type of the stream
    *  @param streamName the name of the Kinesis stream to write to
    *  @param producer kinesis producer client to use
    *  @return a Pipe that accepts a tuple consisting of the partition key string and a ByteBuffer of data  and returns UserRecordResults
    */
  def writeToKinesis[F[_]](
      streamName: String,
      producer: KinesisProducerClient[F] = new KinesisProducerClient[F] {}
  )(implicit F: Effect[F],
    ec: ExecutionContext,
    concurrent: Concurrent[F]): fs2.Pipe[F, (String, ByteBuffer), UserRecordResult] = {

    // Evaluate the operation of invoking the Kinesis client
    def write: fs2.Pipe[F, (String, ByteBuffer), ListenableFuture[UserRecordResult]] =
      _.flatMap {
        case (partitionKey, byteArray) =>
          fs2.Stream.eval(producer.putData(streamName, partitionKey, byteArray))
      }

    // Register the returned future, returning the UserRecordResult
    def registerCallback: fs2.Pipe[F, ListenableFuture[UserRecordResult], UserRecordResult] =
      _.mapAsync(1) {
        case f =>
          Effect[F].async[UserRecordResult] { cb =>
            Futures.addCallback(
              f,
              new FutureCallback[UserRecordResult] {
                override def onFailure(t: Throwable): Unit             = cb(Left(t))
                override def onSuccess(result: UserRecordResult): Unit = cb(Right(result))
              },
              (command: Runnable) => ec.execute(command)
            )
          }
      }

    _.through(write)
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
      producer: KinesisProducerClient[F] = new KinesisProducerClient[F] {}
  )(implicit F: Effect[F],
    ec: ExecutionContext,
    concurrent: Concurrent[F]): fs2.Sink[F, (String, ByteBuffer)] = {
    _.through(writeToKinesis(streamName, producer))
      .map(_ => ())
  }
}
