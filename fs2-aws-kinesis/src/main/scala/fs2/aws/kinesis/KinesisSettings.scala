package fs2.aws.kinesis

import eu.timepit.refined.auto.*
import eu.timepit.refined.types.numeric.*
import fs2.aws.internal.Exceptions.*
import software.amazon.awssdk.regions.Region
import software.amazon.kinesis.common.InitialPositionInStream

import java.util.Date
import scala.concurrent.duration.*

class KinesisConsumerSettings private (
    val kcl: KinesisKCLSettings,
    val fs2: KinesisFS2Settings
)

object KinesisConsumerSettings {
  def apply(
      streamName: String,
      appName: String,
      region: Region = Region.US_EAST_1,
      bufferSize: PosInt = PosInt.unsafeFrom(10),
      initialPositionInStream: Either[InitialPositionInStream, Date] = Left(
        InitialPositionInStream.LATEST
      ),
      retrievalMode: RetrievalMode = FanOut
  ): KinesisConsumerSettings =
    new KinesisConsumerSettings(
      KinesisKCLSettings(streamName, appName, region, initialPositionInStream, retrievalMode),
      KinesisFS2Settings(bufferSize)
    )
}

/** Settings for configuring the KCL
  *
  *  @param streamName name of the Kinesis stream to read from
  *  @param appName name of the application which the KCL daemon should assume
  *  @param region AWS region in which the Kinesis stream resides. Defaults to US-EAST-1
  *  @param retrievalMode FanOut (push) or Polling (pull). Defaults to FanOut (the new default in KCL 2.x).
  */
class KinesisKCLSettings private (
    val streamName: String,
    val appName: String,
    val region: Region,
    val initialPositionInStream: Either[InitialPositionInStream, Date],
    val retrievalMode: RetrievalMode
)

object KinesisKCLSettings {

  def apply(
      streamName: String,
      appName: String,
      region: Region = Region.US_EAST_1,
      initialPositionInStream: Either[InitialPositionInStream, Date] = Left(
        InitialPositionInStream.LATEST
      ),
      retrievalMode: RetrievalMode = FanOut
  ): KinesisKCLSettings =
    new KinesisKCLSettings(
      streamName,
      appName,
      region,
      initialPositionInStream,
      retrievalMode
    )
}

sealed trait RetrievalMode
case object FanOut  extends RetrievalMode
case object Polling extends RetrievalMode

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

  def apply(
      maxBatchSize: Int,
      maxBatchWait: FiniteDuration
  ): Either[Throwable, KinesisCheckpointSettings] =
    (maxBatchSize, maxBatchWait) match {
      case (s, _) if s <= 0 =>
        Left(MaxBatchSizeException("Must be greater than 0"))
      case (_, w) if w <= 0.milliseconds =>
        Left(
          MaxBatchWaitException(
            "Must be greater than 0 milliseconds. To checkpoint immediately, pass 1 to the max batch size."
          )
        )
      case (s, w) =>
        Right(new KinesisCheckpointSettings(s, w))
    }
}

/** Settings for configuring the Kinesis consumer's FS2 Stream
  *
  *   @param bufferSize size of the internal buffer used when reading messages from Kinesis
  */
class KinesisFS2Settings private (val bufferSize: PosInt)

object KinesisFS2Settings {
  def apply(bufferSize: PosInt = PosInt.unsafeFrom(10)): KinesisFS2Settings = new KinesisFS2Settings(bufferSize)
}
