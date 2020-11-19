package fs2.aws.kinesis

import java.util.concurrent.Semaphore

import fs2.Chunk
import software.amazon.kinesis.lifecycle.events._
import software.amazon.kinesis.processor.ShardRecordProcessor
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

import scala.jdk.CollectionConverters._

/** Concrete implementation of the AWS RecordProcessor interface.
  * Wraps incoming records into CommitableRecord types to allow for downstream
  * checkpointing
  *
  *  @constructor create a new instance with a callback function to perform on record receive
  *  @param cb callback function to run on record receive, passing the new CommittableRecord
  */
private[aws] class ChunkedRecordProcessor(cb: Chunk[CommittableRecord] => Unit)
    extends ShardRecordProcessor {
  private[kinesis] var shardId: String                                = _
  private[kinesis] var extendedSequenceNumber: ExtendedSequenceNumber = _
  private[kinesis] var isShutdown: Boolean                            = false
  private[aws] val lastRecordSemaphore                                = new Semaphore(1)

  override def initialize(initializationInput: InitializationInput): Unit = {
    shardId = initializationInput.shardId()
    extendedSequenceNumber = initializationInput.extendedSequenceNumber()
  }

  override def leaseLost(leaseLostInput: LeaseLostInput): Unit = {}

  override def shardEnded(shardEndedInput: ShardEndedInput): Unit = {
    isShutdown = true
    lastRecordSemaphore.acquire()
    shardEndedInput.checkpointer().checkpoint()
  }

  override def shutdownRequested(shutdownRequestedInput: ShutdownRequestedInput): Unit =
    isShutdown = true

  override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
    if (processRecordsInput.isAtShardEnd)
      lastRecordSemaphore.acquire()
    val batch = processRecordsInput
      .records()
      .asScala
      .toList
      .map { record =>
        CommittableRecord(
          shardId,
          extendedSequenceNumber,
          processRecordsInput.millisBehindLatest(),
          record,
          recordProcessor = this,
          processRecordsInput.checkpointer(),
          lastRecordSemaphore
        )
      }

    val chunk =
      if (processRecordsInput.isAtShardEnd)
        batch match {
          case head :+ last => head :+ last.copy(isLastInShard = true)
          case Nil          => Nil
        }
      else batch
    cb(Chunk(chunk: _*))
  }
}
