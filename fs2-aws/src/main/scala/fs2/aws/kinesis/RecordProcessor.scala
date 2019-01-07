package fs2.aws.kinesis

import software.amazon.kinesis.lifecycle.events._
import software.amazon.kinesis.processor.ShardRecordProcessor
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

/** Concrete implementation of the AWS RecordProcessor interface.
  * Wraps incoming records into CommitableRecord types to allow for downstream
  * checkpointing
  *
  *  @constructor create a new instance with a callback function to perform on record receive
  *  @param cb callback function to run on record receive, passing the new CommittableRecord
  */
private[aws] class RecordProcessor(cb: CommittableRecord => Unit,
                                   terminateGracePeriod: FiniteDuration)
    extends ShardRecordProcessor {
  private var shardId: String                                = _
  private var extendedSequenceNumber: ExtendedSequenceNumber = _
  private[kinesis] var isShutdown: Boolean                   = false

  override def initialize(initializationInput: InitializationInput): Unit = {
    shardId = initializationInput.shardId()
    extendedSequenceNumber = initializationInput.extendedSequenceNumber()
  }

  override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
    processRecordsInput.records().asScala.foreach { record =>
      cb(
        CommittableRecord(
          shardId,
          extendedSequenceNumber,
          processRecordsInput.millisBehindLatest(),
          record,
          recordProcessor = this,
          processRecordsInput.checkpointer()
        )
      )
    }
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
    shutdownRequestedInput.checkpointer().checkpoint();
  }
}
