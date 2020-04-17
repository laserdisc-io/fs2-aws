package fs2.aws.dynamodb.parsers

sealed trait DynamoDBBeforeAfter[T]

case class Insert[T](after: T)            extends DynamoDBBeforeAfter[T]
case class Update[T](before: T, after: T) extends DynamoDBBeforeAfter[T]
case class Delete[T](before: T)           extends DynamoDBBeforeAfter[T]
case object Unsupported extends DynamoDBBeforeAfter[Nothing] {
  def apply[T]: DynamoDBBeforeAfter[T] = this.asInstanceOf[DynamoDBBeforeAfter[T]]
}
