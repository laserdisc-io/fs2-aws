package sqs

import scala.concurrent.duration.*

@deprecated("Use fs2.aws.sqs.SqsConfig instead")
class SqsConfig(
    queueUrl: String,
    pollRate: FiniteDuration,
    fetchMessageCount: Int
) extends fs2.aws.sqs.SqsConfig(queueUrl, pollRate, fetchMessageCount) {
  override def copy(
      queueUrl: String = this.queueUrl,
      pollRate: FiniteDuration = this.pollRate,
      fetchMessageCount: Int = this.fetchMessageCount
  ): SqsConfig =
    new SqsConfig(queueUrl, pollRate, fetchMessageCount)
}

@deprecated("Use fs2.aws.sqs.SqsConfig instead")
object SqsConfig extends ((String, FiniteDuration, Int) => SqsConfig) {
  def apply(
      queueUrl: String,
      pollRate: FiniteDuration = 3.seconds,
      fetchMessageCount: Int = 10
  ): SqsConfig = new SqsConfig(queueUrl, pollRate, fetchMessageCount)

  def unapply(value: SqsConfig): Some[(String, FiniteDuration, Int)] = Some(
    (value.queueUrl, value.pollRate, value.fetchMessageCount)
  )
}
