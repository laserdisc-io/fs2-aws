package fs2
package aws
package internal

object Exceptions {
  case class KinesisCheckpointException(msg: String) extends Exception

  sealed trait KinesisSettingsException         extends Exception
  case class BufferSizeException(msg: String)   extends KinesisSettingsException
  case class MaxBatchWaitException(msg: String) extends KinesisSettingsException
  case class MaxBatchSizeException(msg: String) extends KinesisSettingsException
}
