package fs2.aws.kinesis

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
private[aws] class SingleRecordProcessor(cb: CommittableRecord => Unit,
                                         override val terminateGracePeriod: FiniteDuration)
    extends RecordProcessor {
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
}
