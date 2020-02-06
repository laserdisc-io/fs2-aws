package fs2.aws.kinesis

import fs2.Chunk
import software.amazon.kinesis.lifecycle.events._

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

/** Concrete implementation of the AWS RecordProcessor interface.
  * Wraps incoming records into CommitableRecord types to allow for downstream
  * checkpointing
  *
  *  @constructor create a new instance with a callback function to perform on record receive
  *  @param cb callback function to run on record receive, passing the new CommittableRecord
  */
private[aws] class ChunkedRecordProcessor(
  cb: Chunk[CommittableRecord] => Unit,
  override val terminateGracePeriod: FiniteDuration
) extends RecordProcessor {

  override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
    val batch = processRecordsInput
      .records()
      .asScala
      .map { record =>
        CommittableRecord(
          shardId,
          extendedSequenceNumber,
          processRecordsInput.millisBehindLatest(),
          record,
          recordProcessor = this,
          processRecordsInput.checkpointer()
        )
      }
    cb(Chunk(batch: _*))
  }
}
