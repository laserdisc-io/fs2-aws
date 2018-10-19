package fs2
package aws
package kinesis

import com.amazonaws.services.kinesis.clientlibrary.types.{ ExtendedSequenceNumber, UserRecord }
import com.amazonaws.services.kinesis.model.Record
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer


case class CommittableRecord(
  shardId: String,
  recordProcessorStartingSequenceNumber: ExtendedSequenceNumber,
  millisBehindLatest: Long,
  record: Record,
  recordProcessor: RecordProcessor,
  checkpointer: IRecordProcessorCheckpointer
) {
  val sequenceNumber: String = record.getSequenceNumber

  def canCheckpoint(): Boolean = !recordProcessor.isShutdown
  def checkpoint(): Unit = checkpointer.checkpoint(record)
}

object CommittableRecord {

  // Only makes sense to compare Records belonging to the same shard
  // Records that have been batched by the KCL producer all have the
  // same sequence number but will differ by subsequence number
  implicit val orderBySequenceNumber: Ordering[CommittableRecord] =
    Ordering[(String, Long)].on(cr ⇒
      (cr.sequenceNumber, cr.record match {
         case ur: UserRecord ⇒ ur.getSubSequenceNumber
         case _ ⇒ 0
       }))

}
