package fs2.aws.sqs

import scala.concurrent.duration.*

/** @param pollRate interval between receive calls. Zero suits long polling (`_.waitTimeSeconds(20)` via `SQS.createWithReceiveRequest`,
  *                 or the queue's `ReceiveMessageWaitTimeSeconds`), where each call waits for messages itself. Without long
  *                 polling, zero makes the stream busy-loop on empty receives.
  * @param fetchMessageCount max number of messages to return in one query. Must be between 1 and 10.
  *                          Visit [[https://docs.aws.amazon.com/AWSSimpleQueueService/latest/APIReference/API_ReceiveMessage.html Receive Message API]] for more info
  *
  *                          A receive hides all its messages at once, but `sqsStream` hands them over one at a time, so a heartbeat
  *                          around the current message leaves the rest of the batch to time out. If handling can outlast the queue's
  *                          visibility timeout, use 1 or extend the whole batch (`sqsStreamBatched`).
  */
case class SqsConfig(
    queueUrl: String,
    pollRate: FiniteDuration = 3.seconds,
    fetchMessageCount: Int = 10
)
