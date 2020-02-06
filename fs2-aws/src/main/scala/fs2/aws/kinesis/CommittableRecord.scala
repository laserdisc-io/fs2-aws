package fs2.aws.kinesis

import software.amazon.kinesis.processor.RecordProcessorCheckpointer
import software.amazon.kinesis.retrieval.KinesisClientRecord
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

/** A message type from Kinesis which has not yet been commited or checkpointed.
  *
  *  @constructor create a new commitable record with a name and age.
  *  @param shardId the unique identifier for the shard from which this record originated
  *  @param millisBehindLatest ms behind the latest record, used to detect if the consumer is lagging the producer
  *  @param record the original record document from Kinesis
  *  @param recordProcessor reference to the record processor that is responsible for processing this message
  *  @param checkpointer reference to the checkpointer used to commit this record
  */
case class CommittableRecord(
  shardId: String,
  recordProcessorStartingSequenceNumber: ExtendedSequenceNumber,
  millisBehindLatest: Long,
  record: KinesisClientRecord,
  recordProcessor: RecordProcessor,
  checkpointer: RecordProcessorCheckpointer
) {
  val sequenceNumber: String  = record.sequenceNumber()
  val subSequenceNumber: Long = record.subSequenceNumber()
  def canCheckpoint: Boolean  = !recordProcessor.isShutdown
  def checkpoint(): Unit =
    checkpointer.checkpoint(record.sequenceNumber(), record.subSequenceNumber())
}

object CommittableRecord {

  // Only makes sense to compare Records belonging to the same shard
  // Records that have been batched by the KCL producer all have the
  // same sequence number but will differ by subsequence number
  implicit val orderBySequenceNumber: Ordering[CommittableRecord] =
    Ordering[(String, Long)].on(cr ⇒
      (cr.sequenceNumber, cr.record match {
        case ur: KinesisClientRecord ⇒ ur.subSequenceNumber()
        case _                       ⇒ 0
      })
    )
}
