package fs2
package aws
package kinesis

import com.amazonaws.services.kinesis.clientlibrary.interfaces._
import com.amazonaws.services.kinesis.clientlibrary.types._
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason
import scala.collection.JavaConverters._

/** Concrete implementation of the AWS RecordProcessor interface.
  * Wraps incoming records into CommitableRecord types to allow for downstream
  * checkpointing
  *
  *  @constructor create a new instance with a callback function to perform on record receive
  *  @param cb callback function to run on record receive, passing the new CommittableRecord
  */
private[kinesis] class RecordProcessor(cb: CommittableRecord => Unit) extends v2.IRecordProcessor {
  private var shardId: String                                  = _
  private var extendedSequenceNumber: ExtendedSequenceNumber   = _
  var shutdown: Option[ShutdownReason]                         = None
  var latestCheckpointer: Option[IRecordProcessorCheckpointer] = None

  def isShutdown = shutdown.isDefined

  override def initialize(initializationInput: InitializationInput): Unit = {
    shardId = initializationInput.getShardId
    extendedSequenceNumber = initializationInput.getExtendedSequenceNumber
  }

  override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
    latestCheckpointer = Some(processRecordsInput.getCheckpointer)
    processRecordsInput.getRecords.asScala.foreach { record =>
      cb(
        CommittableRecord(
          shardId,
          extendedSequenceNumber,
          processRecordsInput.getMillisBehindLatest,
          record,
          recordProcessor = this,
          processRecordsInput.getCheckpointer
        )
      )
    }
  }

  override def shutdown(shutdownInput: ShutdownInput): Unit = {
    shutdown = Some(shutdownInput.getShutdownReason)
    latestCheckpointer = Some(shutdownInput.getCheckpointer)
  }
}
