package fs2.aws.dynamodb.models

import monix.newtypes.Newsubtype

object DynamoDBModels {
  type BufferSize = BufferSize.Type

  object BufferSize extends Newsubtype[Int] {
    def apply(value: Int): Either[String, BufferSize] =
      if (value > 0) Right(unsafeCoerce(value))
      else Left(s"Size is not positive $value")
  }
}
