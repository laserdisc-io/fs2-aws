package fs2
package aws
package kinesis

import scala.concurrent.duration._

case class KinesisWorkerStreamSettings(
  bufferSize: Int,
  backpressureTimeout: FiniteDuration) {
  require(
    bufferSize >= 1, "Buffer size must be greater than 0"
  )
}

case class KinesisWorkerCheckpointSettings(
  maxBatchSize: Int,
  maxBatchWait: FiniteDuration
) {
  require(
    maxBatchSize >= 1,
    "Batch size must be greater than 0; use size 1 to commit records one at a time"
  )
}

object KinesisWorkerStreamSettings {
  val defaultInstance = KinesisWorkerStreamSettings(10, 1.minute)
}

object KinesisWorkerCheckpointSettings {
  val defaultInstance = KinesisWorkerCheckpointSettings(1000, 10.seconds)
}
