package fs2
package aws
package kinesis

import scala.concurrent.duration._

case class KinesisWorkerSettings(
  bufferSize: Int,
  backpressureTimeout: FiniteDuration) {
  require(
    bufferSize >= 1, "Buffer size must be greater than 0"
  )
}

object KinesisWorkerSettings {
  val defaultInstance = KinesisWorkerSettings(10, 1.minute)
}
