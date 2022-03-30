package sqs

import scala.concurrent.duration.*

case class SqsConfig(
    queueUrl: String,
    pollRate: FiniteDuration = 3.seconds,
    fetchMessageCount: Int = 100
)
