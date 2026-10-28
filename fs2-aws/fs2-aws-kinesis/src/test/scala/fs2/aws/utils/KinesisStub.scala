package fs2.aws.utils

import java.nio.ByteBuffer

object KinesisStub {
  var _data: List[ByteBuffer] = List()

  def clear(): Unit = _data = List()

  def save(data: ByteBuffer): Unit =
    _data = _data :+ data
}
