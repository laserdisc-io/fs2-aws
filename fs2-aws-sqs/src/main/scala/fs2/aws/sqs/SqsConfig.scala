package sqs

import scala.concurrent.duration._

case class SqsConfig(
  queueUrl: String,
  pollRate: FiniteDuration = 3.seconds,
  fetchMessageCount: Int = 100,
  bufferSize: Int = 100
)
