package fs2.aws.kinesis

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive

import fs2.aws.internal.Exceptions._
import software.amazon.awssdk.regions.Region
import software.amazon.kinesis.common.InitialPositionInStream

import java.net.URI
import java.util.Date
import scala.concurrent.duration._
import eu.timepit.refined.auto._

/** Settings for configuring the Kinesis consumer
  *
  *  @param streamName name of the Kinesis stream to read from
  *  @param appName name of the application which the KCL daemon should assume
  *  @param region AWS region in which the Kinesis stream resides. Defaults to US-EAST-1
  *  @param maxConcurrency max size of the KinesisAsyncClient HTTP thread pool. Defaults to Int.MaxValue.
  *  @param bufferSize size of the internal buffer used when reading messages from Kinesis
  *  @param stsAssumeRole If present, the configured client will be setup for AWS cross-account access using the provided tokens
  *  @param endpoint endpoint for the clients that are created. Default to None (i.e. AWS) but can be overridden (e.g. to localhost)
  *  @param retrievalMode FanOut (push) or Polling (pull). Defaults to FanOut (the new default in KCL 2.x).
  */
class KinesisConsumerSettings private (
  val streamName: String,
  val appName: String,
  val region: Region,
  val maxConcurrency: Int,
  val bufferSize: Int,
  val stsAssumeRole: Option[STSAssumeRoleSettings],
  val initialPositionInStream: Either[InitialPositionInStream, Date],
  val endpoint: Option[URI],
  val retrievalMode: RetrievalMode
)

object KinesisConsumerSettings {
  def apply(
    streamName: String,
    appName: String,
    region: Region = Region.US_EAST_1,
    maxConcurrency: Int Refined Positive = Int.MaxValue,
    bufferSize: Int Refined Positive = 10,
    stsAssumeRole: Option[STSAssumeRoleSettings] = None,
    initialPositionInStream: Either[InitialPositionInStream, Date] = Left(
      InitialPositionInStream.LATEST
    ),
    endpoint: Option[String] = None,
    retrievalMode: RetrievalMode = FanOut
  ): KinesisConsumerSettings =
    new KinesisConsumerSettings(
      streamName,
      appName,
      region,
      maxConcurrency,
      bufferSize,
      stsAssumeRole,
      initialPositionInStream,
      endpoint.map(URI.create),
      retrievalMode
    )
}

sealed trait RetrievalMode
case object FanOut  extends RetrievalMode
case object Polling extends RetrievalMode

/**
  * Used when constructing a [KinesisConsumerSettings] instance that will by used by a client for cross-account access.
  *
  * This currently implements only the minimum required fields defined in:
  * https://docs.aws.amazon.com/cli/latest/reference/sts/assume-role.html
  *
  * @param roleArn The Amazon Resource Name (ARN) of the role to assume.
  * @param roleSessionName An identifier for the assumed role session.
  * @param externalId A unique identifier that might be required when you assume a role in another account.
  * @param durationSeconds The duration, in seconds, of the role session.
  */
class STSAssumeRoleSettings private (
  val roleArn: String,
  val roleSessionName: String,
  val externalId: Option[String],
  val durationSeconds: Option[Int]
)

object STSAssumeRoleSettings {
  def apply(
    roleArn: String,
    roleSessionName: String,
    externalId: Option[String] = None,
    durationSeconds: Option[Int] = None
  ) =
    new STSAssumeRoleSettings(roleArn, roleSessionName, externalId, durationSeconds)
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
