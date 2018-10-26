package fs2
package aws
package internal

object Exceptions {
  case class KinesisCheckpointException(msg: String) extends Exception
}
