package fs2.aws.kinesis

import software.amazon.kinesis.lifecycle.events._
import software.amazon.kinesis.processor.ShardRecordProcessor
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

import scala.concurrent.duration.FiniteDuration

/** Concrete implementation of the AWS RecordProcessor interface.
  * Wraps incoming records into CommitableRecord types to allow for downstream
  * checkpointing
  */
private[aws] trait RecordProcessor extends ShardRecordProcessor {
  val terminateGracePeriod: FiniteDuration

  private[kinesis] var shardId: String                                = _
  private[kinesis] var extendedSequenceNumber: ExtendedSequenceNumber = _
  private[kinesis] var isShutdown: Boolean                            = false

  override def initialize(initializationInput: InitializationInput): Unit = {
    shardId = initializationInput.shardId()
    extendedSequenceNumber = initializationInput.extendedSequenceNumber()
  }

  override def leaseLost(leaseLostInput: LeaseLostInput): Unit = {}

  override def shardEnded(shardEndedInput: ShardEndedInput): Unit = {
    isShutdown = true
    Thread.sleep(terminateGracePeriod.toMillis)
    shardEndedInput.checkpointer().checkpoint()
  }

  override def shutdownRequested(shutdownRequestedInput: ShutdownRequestedInput): Unit = {
    isShutdown = true
    Thread.sleep(terminateGracePeriod.toMillis)
    shutdownRequestedInput.checkpointer().checkpoint()
  }
}
