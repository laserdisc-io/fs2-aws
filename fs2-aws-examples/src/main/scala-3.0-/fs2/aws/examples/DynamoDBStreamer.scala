package fs2.aws.examples

import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudwatch.{AmazonCloudWatch, AmazonCloudWatchClientBuilder}
import com.amazonaws.services.dynamodbv2.{
  AmazonDynamoDB,
  AmazonDynamoDBClientBuilder,
  AmazonDynamoDBStreams,
  AmazonDynamoDBStreamsClientBuilder
}
import fs2.aws.dynamodb.{DynamoDB, parsers}
import io.circe.Json
import io.laserdisc.scanamo.circe.CirceDynamoFormat.*
import org.typelevel.log4cats.slf4j.Slf4jLogger

object DynamoDBStreamer extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    resources.use { case (streams, ddb, watch) =>
      for {
        logger <- Slf4jLogger.fromName[IO]("example")

        ddbStream = DynamoDB.create[IO](streams, ddb, watch)
        _ <- ddbStream
          .readFromDynamoDBStream(
            "app-name",
            "ddb-stream-arn"
          )
          .evalMap(cr => parsers.parseDynamoEvent[IO, Json](cr.record))
          .evalTap(msg => logger.info(s"received $msg"))
          .compile
          .drain
      } yield ExitCode.Success
    }

  def resources: Resource[IO, (AmazonDynamoDBStreams, AmazonDynamoDB, AmazonCloudWatch)] =
    for {
      dynamoDBStreamsClient <- Resource.make(
        IO.delay(
          AmazonDynamoDBStreamsClientBuilder
            .standard()
            .withRegion(Regions.US_EAST_1)
            .build()
        )
      )(res => IO.delay(res.shutdown()))

      dynamoDBClient <- Resource.make(
        IO.delay(
          AmazonDynamoDBClientBuilder
            .standard()
            .withRegion(Regions.US_EAST_1)
            .build()
        )
      )(res => IO.delay(res.shutdown()))
      cloudWatchClient <- Resource.make(
        IO.delay(
          AmazonCloudWatchClientBuilder
            .standard()
            .withRegion(Regions.US_EAST_1)
            .build()
        )
      )(res => IO.delay(res.shutdown()))

    } yield (dynamoDBStreamsClient, dynamoDBClient, cloudWatchClient)
}
