package fs2.aws.utils

import java.nio.ByteBuffer

object KinesisStub {
  var _data: List[ByteBuffer] = List()

  def clear() = _data = List()

  def save(data: ByteBuffer) =
    _data = _data :+ data
}
