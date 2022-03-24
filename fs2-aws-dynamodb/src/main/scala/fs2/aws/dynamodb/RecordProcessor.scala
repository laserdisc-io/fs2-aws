package fs2.aws.dynamodb

import java.util.concurrent.Phaser

import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.amazonaws.services.kinesis.clientlibrary.interfaces.*
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason
import com.amazonaws.services.kinesis.clientlibrary.types.*

import scala.jdk.CollectionConverters.*

/** Concrete implementation of the AWS RecordProcessor interface.
  * Wraps incoming records into CommitableRecord types to allow for downstream
  * checkpointing
  *
  *  @constructor create a new instance with a callback function to perform on record receive
  *  @param cb callback function to run on record receive, passing the new CommittableRecord
  */
class RecordProcessor(
  cb: CommittableRecord => Unit
) extends v2.IRecordProcessor {
  private var shardId: String                                  = _
  private var extendedSequenceNumber: ExtendedSequenceNumber   = _
  var latestCheckpointer: Option[IRecordProcessorCheckpointer] = None
  var shutdown: Option[ShutdownReason]                         = None
  private[aws] val inFlightRecordsPhaser                       = new Phaser(1)

  def isShutdown = shutdown.isDefined

  override def initialize(initializationInput: InitializationInput): Unit = {
    shardId = initializationInput.getShardId
    extendedSequenceNumber = initializationInput.getExtendedSequenceNumber
  }

  override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
    latestCheckpointer = Some(processRecordsInput.getCheckpointer)
    processRecordsInput.getRecords.asScala.foreach { record =>
      // Unfortunately this SDK version doesn't support "is end of shard" indicator
      // on the `ProcessRecordsInput` level, this is why we keep tracking the number
      // of issued records in semaphore
      inFlightRecordsPhaser.register()
      cb(
        CommittableRecord(
          shardId,
          extendedSequenceNumber,
          processRecordsInput.getMillisBehindLatest,
          record.asInstanceOf[RecordAdapter],
          recordProcessor = this,
          processRecordsInput.getCheckpointer,
          inFlightRecordsPhaser
        )
      )
    }
  }

  override def shutdown(shutdownInput: ShutdownInput): Unit = {
    shutdown = Some(shutdownInput.getShutdownReason)
    latestCheckpointer = Some(shutdownInput.getCheckpointer)
    shutdownInput.getShutdownReason match {
      case ShutdownReason.TERMINATE =>
        inFlightRecordsPhaser.arriveAndAwaitAdvance()
        latestCheckpointer.foreach(_.checkpoint())
      case ShutdownReason.ZOMBIE    => ()
      case ShutdownReason.REQUESTED => ()
    }
  }
}
