package fs2.aws.dynamodb

import java.util

import cats.effect.Sync
import cats.implicits._
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import org.scanamo.{ DynamoFormat, DynamoObject, DynamoReadError }

package object parsers {
  def parseDynamoEvent[F[_]: Sync, T](
    record: RecordAdapter
  )(implicit df: DynamoFormat[T]): F[DynamoDBBeforeAfter[T]] =
    record.getInternalObject.getEventName match {
      case "INSERT" =>
        parseDynamoRecord(record.getInternalObject.getDynamodb.getNewImage)
          .map(after => Insert(after))
      case "MODIFY" =>
        for {
          before <- parseDynamoRecord(record.getInternalObject.getDynamodb.getOldImage)
          after  <- parseDynamoRecord(record.getInternalObject.getDynamodb.getNewImage)
        } yield Update(before, after)
      case "REMOVE" =>
        parseDynamoRecord(record.getInternalObject.getDynamodb.getOldImage)
          .map(after => Insert(after))
    }

  def parseDynamoRecord[F[_]: Sync, T](
    image: util.Map[String, AttributeValue]
  )(implicit df: DynamoFormat[T]): F[T] =
    df.read(
        DynamoObject(image).toAttributeValue
      )
      .leftMap(e => new RuntimeException(DynamoReadError.describe(e)))
      .liftTo[F]

}
