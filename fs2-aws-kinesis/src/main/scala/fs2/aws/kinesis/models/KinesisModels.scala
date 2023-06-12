package fs2.aws.kinesis.models

import monix.newtypes.Newsubtype

object KinesisModels {
  type BufferSize  = BufferSize.Type
  type AppName     = AppName.Type
  type SchedulerId = SchedulerId.Type
  type StreamName  = StreamName.Type
  object AppName extends Newsubtype[String] {
    def apply(value: String): Either[String, AppName] =
      if (value.nonEmpty) Right(unsafeCoerce(value))
      else Left(s"App name is empty $value")
  }
  object SchedulerId extends Newsubtype[String] {
    def apply(value: String): SchedulerId = unsafeCoerce(value)
  }

  object StreamName extends Newsubtype[String] {
    def apply(value: String): Either[String, StreamName] =
      if (value.nonEmpty) Right(unsafeCoerce(value))
      else Left(s"Stream name is empty $value")
  }

  object BufferSize extends Newsubtype[Int] {
    def apply(value: Int): Either[String, BufferSize] =
      if (value > 0) Right(unsafeCoerce(value))
      else Left(s"Size is not positive $value")
  }
}
