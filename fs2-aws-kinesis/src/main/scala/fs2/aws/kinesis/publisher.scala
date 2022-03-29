package fs2.aws.kinesis

import java.nio.ByteBuffer

import cats.effect.{Async, Sync}
import com.amazonaws.services.kinesis.producer.UserRecordResult
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import fs2.aws.internal.*
import fs2.{Pipe, Stream}
import cats.implicits.*

import scala.concurrent.ExecutionContext

/** fs2 Streams for publishing data to AWS Kinesis streams
  */
object publisher {

  def write[F[_]: Sync](
      streamName: String,
      producer: KinesisProducerClient[F]
  ): Pipe[F, (String, ByteBuffer), ListenableFuture[UserRecordResult]] =
    _.flatMap { case (partitionKey, byteArray) =>
      Stream.eval(producer.putData(streamName, partitionKey, byteArray))
    }

  def writeObjectAndBypass[F[_]: Sync, I](
      streamName: String,
      producer: KinesisProducerClient[F],
      encoder: I => ByteBuffer
  ): Pipe[F, (String, I), (I, ListenableFuture[UserRecordResult])] =
    _.evalMap { case (partitionKey, entity) =>
      producer.putData(streamName, partitionKey, encoder(entity)).map(entity -> _)
    }

  // Register the returned future, returning the UserRecordResult

  /** Writes the (partitionKey, ByteBuffer) to a Kinesis stream via a Pipe
    *
    * @tparam F effect type of the stream
    * @param streamName the name of the Kinesis stream to write to
    * @param parallelism number of concurrent writes to race simultaneously
    * @param producer   kinesis producer client to use
    * @return a Pipe that accepts a tuple consisting of the partition key string and a ByteBuffer of data  and returns UserRecordResults
    */
  def writeToKinesis[F[_]: Async](
      streamName: String,
      parallelism: Int = 10,
      producer: KinesisProducerClient[F] = new KinesisProducerClientImpl[F]
  )(implicit ec: ExecutionContext): Pipe[F, (String, ByteBuffer), UserRecordResult] = {

    // Evaluate the operation of invoking the Kinesis client
    def registerCallback: Pipe[F, ListenableFuture[UserRecordResult], UserRecordResult] =
      _.mapAsync(parallelism) { f =>
        Async[F].async_[UserRecordResult] { cb =>
          Futures.addCallback(
            f,
            new FutureCallback[UserRecordResult] {
              override def onFailure(t: Throwable): Unit = cb(Left(t))

              override def onSuccess(result: UserRecordResult): Unit = cb(Right(result))
            },
            (command: Runnable) => ec.execute(command)
          )
        }
      }

    _.through(write(streamName, producer))
      .through(registerCallback)
  }

  /** Writes the (partitionKey, ByteBuffer) to a Kinesis stream via a Pipe
    *
    * @tparam F effect type of the stream
    * @param streamName the name of the Kinesis stream to write to
    * @param parallelism number of concurrent writes to race simultaneously
    * @param producer   kinesis producer client to use
    * @return a Pipe that accepts a tuple consisting of the partition key string and a ByteBuffer of data  and returns Unit
    *         this is most fast versKinesisConsumerSpecion of producer, since we do not care about the result of kinesis right, hence we don't wait
    *         for it to publish next message
    */
  def writeAndForgetToKinesis[F[_]: Sync](
      streamName: String,
      parallelism: Int = 10,
      producer: KinesisProducerClient[F] = new KinesisProducerClientImpl[F]
  ): Pipe[F, (String, ByteBuffer), Unit] =
    _.through(write(streamName, producer)).void

  /** Writes the (partitionKey, payload) to a Kinesis stream via a Pipe
    *
    * @tparam F effect type of the stream
    * @tparam I type of payload
    * @param streamName the name of the Kinesis stream to write to
    * @param parallelism number of concurrent writes to race simultaneously
    * @param producer   kinesis producer client to use
    * @param encoder implicit I => ByteBuffer encoder
    * @return a Pipe that accepts a tuple consisting of the partition key string and a ByteBuffer of data  and returns UserRecordResults
    */
  def writeObjectToKinesis[F[_]: Async, I](
      streamName: String,
      parallelism: Int = 10,
      producer: KinesisProducerClient[F] = new KinesisProducerClientImpl[F]
  )(implicit
      ec: ExecutionContext,
      encoder: I => ByteBuffer
  ): Pipe[F, (String, I), (I, UserRecordResult)] =
    _.flatMap { case (key, i) =>
      Stream((key, encoder(i)))
        .through(writeToKinesis(streamName, parallelism, producer))
        .map(i -> _)
    }

  /** Writes the (partitionKey, payload) to a Kinesis stream via a Pipe
    *
    * @tparam F effect type of the stream
    * @tparam I type of payload
    * @param streamName the name of the Kinesis stream to write to
    * @param parallelism number of concurrent writes to race simultaneously
    * @param producer   kinesis producer client to use
    * @param encoder implicit I => ByteBuffer encoder
    * @return a Pipe that accepts a tuple consisting of the partition key string and an entity and returns original entity
    */
  def writeAndForgetObjectToKinesis[F[_]: Sync, I](
      streamName: String,
      parallelism: Int = 10,
      producer: KinesisProducerClient[F] = new KinesisProducerClientImpl[F]
  )(implicit
      encoder: I => ByteBuffer
  ): Pipe[F, (String, I), I] =
    _.through(writeObjectAndBypass(streamName, producer, encoder)).map { case (evt, _) => evt }

  /** Writes the bytestream to a Kinesis stream via a Sink
    *
    * @tparam F effect type of the stream
    * @param streamName   the name of the Kinesis stream to write to
    * @param parallelism  the max number of writes to race concurrently
    * @param producer     kinesis producer client to use
    * @return a Sink that accepts a stream of bytes
    */
  def writeToKinesis_[F[_]: Async](
      streamName: String,
      parallelism: Int = 10,
      producer: KinesisProducerClient[F] = new KinesisProducerClientImpl[F]
  )(implicit ec: ExecutionContext): Pipe[F, (String, ByteBuffer), Unit] =
    _.through(writeToKinesis(streamName, parallelism, producer)).void
}
