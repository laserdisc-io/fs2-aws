package fs2
package aws

import cats.effect.{IO, Effect, ContextShift, Timer}
import org.scalatest.{FlatSpec, Matchers, BeforeAndAfterEach}
import fs2.aws.kinesis.publisher._
import com.amazonaws.services.kinesis.producer.{UserRecordResult, Attempt}
import com.google.common.util.concurrent.{ListenableFuture, SettableFuture}
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

class KinesisProducerSpec extends FlatSpec with Matchers with BeforeAndAfterEach {
  override def beforeEach {
    KinesisStub.clear
  }

  trait KinesisProducerTestContext {
    implicit val ec: ExecutionContext = ExecutionContext.global
    implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)
    implicit val timer: Timer[IO] = IO.timer(ec)
    import fs2.aws.internal.Internal._

    val ops = IO {
      val future: SettableFuture[UserRecordResult] = SettableFuture.create()
      future.set(new UserRecordResult(List[Attempt]().asJava, "seq #", "shard #", true))
      future
    }

    val kinesisProducerTestClient: KinesisProducerClient[IO] = new KinesisProducerClient[IO] {
      override def putData(streamName: String, partitionKey: String, data: List[Byte])(implicit e: Effect[IO]): IO[ListenableFuture[UserRecordResult]] = {
        KinesisStub.save(data)
        ops
      }
    }
  }

  "Publishing data to a Kinesis stream" should "successfully save data to the stream and return the successful result" in new KinesisProducerTestContext {
    val output = fs2.Stream
      .eval(IO.pure("someData"))
      .flatMap(i => fs2.Stream.emits(i.toString.getBytes))
      .through(writeToKinesis[IO]("test-stream", "partition-key", kinesisProducerTestClient))
      .compile
      .toVector
      .unsafeRunSync

    output.size should be(1)
    output.head.isRight should be(true)

    KinesisStub._data.size should be(1)
    KinesisStub._data.head should be("someData".getBytes)
  }

  "error thrown when writing to stream" should "return the exception wrapped in an Either" in new KinesisProducerTestContext {
    override val ops = IO {
      throw new Exception("couldn't connect to kinesis")
    }

    val output = fs2.Stream
      .eval(IO.pure("someData"))
      .flatMap(i => fs2.Stream.emits(i.toString.getBytes))
      .through(writeToKinesis[IO]("test-stream", "partition-key", kinesisProducerTestClient))
      .compile
      .toVector
      .unsafeRunSync

    output.size should be(1)
    output.head.isLeft should be(true)
    output.head.left.get.getMessage should be("couldn't connect to kinesis")
  }
}
