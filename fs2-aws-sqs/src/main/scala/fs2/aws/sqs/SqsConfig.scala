package fs2.aws.sqs

import scala.concurrent.duration.*

/** @param fetchMessageCount max number of messages to return in one query. Must be between 1 and 10.
  *                          Visit [[https://docs.aws.amazon.com/AWSSimpleQueueService/latest/APIReference/API_ReceiveMessage.html Receive Message API]] for more info
  */
case class SqsConfig(
    queueUrl: String,
    pollRate: FiniteDuration = 3.seconds,
    fetchMessageCount: Int = 10
)
