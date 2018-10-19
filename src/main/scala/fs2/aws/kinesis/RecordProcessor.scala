package fs2
package aws
package kinesis

import com.amazonaws.services.kinesis.clientlibrary.interfaces._
import com.amazonaws.services.kinesis.clientlibrary.types._
import com.amazonaws.services.kinesis.model.Record
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason
import scala.collection.JavaConverters._


private[kinesis] class RecordProcessor(cb: Record => Unit) extends v2.IRecordProcessor {
   private var shardId: String = _
   private var extendedSequenceNumber: ExtendedSequenceNumber = _
   var shutdown: Option[ShutdownReason] = None
   var latestCheckpointer: Option[IRecordProcessorCheckpointer] = None

  def getShardId = shardId
  def getSequenceNumber = extendedSequenceNumber


   override def initialize(initializationInput: InitializationInput): Unit = {
     shardId = initializationInput.getShardId
     extendedSequenceNumber = initializationInput.getExtendedSequenceNumber
   }

   override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
     latestCheckpointer = Some(processRecordsInput.getCheckpointer)
     processRecordsInput.getRecords.asScala.foreach(cb)
   }

   override def shutdown(shutdownInput: ShutdownInput): Unit = {
     shutdown = Some(shutdownInput.getShutdownReason)
     latestCheckpointer = Some(shutdownInput.getCheckpointer)
   }
}
