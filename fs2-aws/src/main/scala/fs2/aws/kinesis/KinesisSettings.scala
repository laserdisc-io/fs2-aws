package fs2.aws.kinesis

import fs2.aws.internal.Exceptions._
import software.amazon.awssdk.regions.Region
import software.amazon.kinesis.common.{InitialPositionInStream, InitialPositionInStreamExtended}

import scala.concurrent.duration._

/** Settings for configuring the Kinesis consumer
  *
  *  @param streamName name of the Kinesis stream to read from
  *  @param appName name of the application which the KCL daemon should assume
  *  @param region AWS region in which the Kinesis stream resides. Defaults to US-EAST-1
  *  @param maxConcurrency max size of the KinesisAsyncClient HTTP thread pool. Defaults to Int.MaxValue.
  *  @param bufferSize size of the internal buffer used when reading messages from Kinesis
  *  @param terminateGracePeriod period of time to allow processing of records before performing the final checkpoint and terminating
  *  @param initialPositionInStreamExtended initial position in Kinesis stream to start processing messages from
  */
class KinesisConsumerSettings private (
    val streamName: String,
    val appName: String,
    val region: Region,
    val maxConcurrency: Int,
    val bufferSize: Int,
    val terminateGracePeriod: FiniteDuration,
    val initialPositionInStreamExtended: InitialPositionInStreamExtended
)

object KinesisConsumerSettings {
  def apply(
      streamName: String,
      appName: String,
      region: Region = Region.US_EAST_1,
      maxConcurrency: Int = Int.MaxValue,
      bufferSize: Int = 10,
      terminateGracePeriod: FiniteDuration = 10.seconds,
      initialPositionInStreamExtended: InitialPositionInStreamExtended =
        InitialPositionInStreamExtended.newInitialPosition(InitialPositionInStream.LATEST)
  ): Either[Throwable, KinesisConsumerSettings] =
    (bufferSize, maxConcurrency, terminateGracePeriod) match {
      case (bs, _, _) if bs < 1 => Left(BufferSizeException("Must be greater than 0"))
      case (_, mc, _) if mc < 1 => Left(MaxConcurrencyException("Must be greater than 0"))
      case (bs, mc, period) =>
        Right(
          new KinesisConsumerSettings(streamName,
                                      appName,
                                      region,
                                      mc,
                                      bs,
                                      period,
                                      initialPositionInStreamExtended)
        )
    }
}

/** Settings for configuring the Kinesis checkpointer pipe
  *
  *  @param maxBatchSize the maximum number of records to aggregate before checkpointing the cluster of records. Passing 1 means checkpoint on every record
  *  @param maxBatchWait the maximum amount of time to wait before checkpointing the cluster of records
  */
class KinesisCheckpointSettings private (
    val maxBatchSize: Int,
    val maxBatchWait: FiniteDuration
)

object KinesisCheckpointSettings {
  val defaultInstance = new KinesisCheckpointSettings(1000, 10.seconds)

  def apply(maxBatchSize: Int,
            maxBatchWait: FiniteDuration): Either[Throwable, KinesisCheckpointSettings] =
    (maxBatchSize, maxBatchWait) match {
      case (s, _) if s <= 0 =>
        Left(MaxBatchSizeException("Must be greater than 0"))
      case (_, w) if w <= 0.milliseconds =>
        Left(MaxBatchWaitException(
          "Must be greater than 0 milliseconds. To checkpoint immediately, pass 1 to the max batch size."))
      case (s, w) =>
        Right(new KinesisCheckpointSettings(s, w))
    }
}
