package fs2

package aws

package kinesis

import java.util.concurrent.Semaphore
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.kinesis.exceptions.ShutdownException
import software.amazon.kinesis.leases.exceptions.InvalidStateException
import software.amazon.kinesis.lifecycle.events.*
import software.amazon.kinesis.processor.ShardRecordProcessor
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

import scala.jdk.CollectionConverters.*

/** Concrete implementation of the AWS RecordProcessor interface.
  * Wraps incoming records into CommitableRecord types to allow for downstream
  * checkpointing
  *
  *  @constructor create a new instance with a callback function to perform on record receive
  *  @param cb callback function to run on record receive, passing the new CommittableRecord
  */
private[aws] class ChunkedRecordProcessor(cb: Chunk[CommittableRecord] => Unit) extends ShardRecordProcessor {

  val logger: Logger = LoggerFactory.getLogger(classOf[ChunkedRecordProcessor]);

  private[kinesis] var shardId: String                                = _
  private[kinesis] var extendedSequenceNumber: ExtendedSequenceNumber = _
  private[kinesis] var isShutdown: Boolean                            = false
  private[aws] val lastRecordSemaphore                                = new Semaphore(1)

  override def initialize(initializationInput: InitializationInput): Unit = {
    shardId = initializationInput.shardId()
    extendedSequenceNumber = initializationInput.extendedSequenceNumber()
    logger.info(s"initializing chunked record processor (shardId:$shardId, extendedSeq#:$extendedSequenceNumber")
  }

  override def leaseLost(leaseLostInput: LeaseLostInput): Unit = {
    logger.info(s"lease lost on shard $shardId: leaseLostInput:$leaseLostInput")
  }

  override def shardEnded(shardEndedInput: ShardEndedInput): Unit = {
    logger.info(s"shard $shardId ended, checkpointing")
    isShutdown = true
    lastRecordSemaphore.acquire()
    shardEndedInput.checkpointer().checkpoint()
  }

  override def shutdownRequested(shutdownRequestedInput: ShutdownRequestedInput): Unit = {

    logger.info(s"Shutting down processor for shard $shardId, attempting one last checkpoint");

    isShutdown = true

    // https://docs.aws.amazon.com/streams/latest/dev/kcl-migration-from-2-3.html
    try {
      shutdownRequestedInput.checkpointer().checkpoint();
    } catch {
      case e @ (_ : ShutdownException | _ : InvalidStateException) =>
        logger.error(s"Unable to checkpoint shard $shardId before shutdown", e);
    }
  }


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
          case _            => Nil
        }
      else batch
    cb(Chunk(chunk*))
  }
}
