package fs2.aws.sqs

import cats.effect.unsafe.IORuntime
import cats.effect.{IO, Resource}
import io.laserdisc.pure.sqs.tagless.{Interpreter, SqsAsyncClientOp}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.*

import java.net.URI
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.jdk.CollectionConverters.*

class SqsSpec extends AnyWordSpec with Matchers {

  implicit val runtime: IORuntime = IORuntime.global

  val sqsOpResource: Resource[IO, SqsAsyncClientOp[IO]] = mkAWSSQSClient()

  val allMsgs: Seq[String]              = List("Barry", "Dmytro", "Ryan", "John", "Vlad")
  val msgStream: fs2.Stream[IO, String] = fs2.Stream(allMsgs*).covary[IO]

  "SQS" should {

    "handle basic publish and consume" in
      sqsOpResource
        .use { sqsOp =>
          for {
            sqs <- mkQueueAndClient(sqsOp, "publish-and-consume-test")
            _   <- msgStream.through(sqs.sendMessagePipe).compile.drain
            res <- sqs.sqsStream.take(5).compile.toList
            _ = res.map(_.body()) should be(allMsgs)
          } yield ()

        }
        .unsafeRunSync()

    "handle message visibility" in
      sqsOpResource
        .use { sqsOp =>
          for {

            // first, write the messages
            sqs <- mkQueueAndClient(sqsOp, "message-visibility-test", defaultQueueVisibility = 1.hour)
            _   <- msgStream.through(sqs.sendMessagePipe).compile.drain

            // now fetch them (we need access to their receipt handles to decrease timeout)
            firstPoll <- sqs.sqsStream.take(5).compile.toList
            _ = firstPoll should have size 5 // sanity check

            // mark two to be made invisible longer
            holdLonger = firstPoll.filter(msg => List("Dmytro", "John").contains(msg.body()))
            _          = holdLonger should have size 2
            _ <- fs2.Stream(holdLonger*).through(sqs.changeMessageVisibilityPipe(1.second)).compile.drain

            // wait past the default visibility, then try and pull all 5 messages again
            _          <- IO.sleep(3.seconds)
            secondPoll <- sqs.sqsStream.take(5).interruptAfter(5.seconds).compile.toList

            // but only those messages whose visibility we reduced should be available
            _ = secondPoll.map(_.body()) should equal(List("Dmytro", "John"))

          } yield ()
        }
        .unsafeRunSync()

    "handle delete" in
      sqsOpResource
        .use { sqsOp =>
          for {
            // first, write the messages
            sqs <- mkQueueAndClient(sqsOp, "message-delete-test", defaultQueueVisibility = 1.second)
            _   <- msgStream.through(sqs.sendMessagePipe).compile.drain

            // get handles to all the messages
            firstPoll <- sqs.sqsStream.take(5).compile.toList
            _ = firstPoll should have size 5 // sanity check

            // pick two specific handles and delete them
            toDelete = firstPoll.filter(msg => List("Barry", "Vlad").contains(msg.body()))
            _        = toDelete should have size 2
            _ <- fs2.Stream(toDelete*).through(sqs.deleteMessagePipe).compile.drain

            // wait past the default visibility, then try and pull all 5 messages again
            _          <- IO.sleep(3.seconds)
            secondPoll <- sqs.sqsStream.take(5).interruptAfter(5.seconds).compile.toList
            _ = secondPoll.map(_.body()).toSet should be(Set("Dmytro", "Ryan", "John"))

          } yield ()
        }
        .unsafeRunSync()

  }

  def mkQueueAndClient(
      sqsOp: SqsAsyncClientOp[IO],
      streamPrefix: String,
      defaultQueueVisibility: FiniteDuration = 30.seconds
  ): IO[SQS[IO]] =

    for {
      queueUrl <- mkQueue(streamPrefix, defaultQueueVisibility)
      t <- SQS.create[IO](
        SqsConfig(queueUrl = queueUrl, pollRate = 500.milliseconds, fetchMessageCount = 5),
        sqsOp
      )
    } yield t

  def mkQueue(prefix: String, defaultVisibility: FiniteDuration): IO[String] =
    sqsOpResource
      .use(
        _.createQueue(
          CreateQueueRequest
            .builder()
            .queueName(prefix + "-" + System.currentTimeMillis())
            .attributes(Map(QueueAttributeName.VISIBILITY_TIMEOUT -> defaultVisibility.toSeconds.toString).asJava)
            .build()
        ).map(_.queueUrl())
      )

  def mkAWSSQSClient(): Resource[IO, SqsAsyncClientOp[IO]] =
    Interpreter[IO].SqsAsyncClientOpResource(
      SqsAsyncClient
        .builder()
        .credentialsProvider(
          StaticCredentialsProvider.create(
            AwsBasicCredentials.create("accesskey", "secretkey")
          )
        )
        .endpointOverride(URI.create(s"http://localhost:4566"))
        .region(Region.US_EAST_1)
    )
}
