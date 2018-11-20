package fs2
package aws

import java.nio.ByteBuffer

import cats.effect.{ContextShift, IO, Timer}
import com.amazonaws.services.kinesis.producer.{Attempt, UserRecordResult}
import com.google.common.util.concurrent.SettableFuture
import fs2.aws.kinesis.publisher._
import fs2.aws.utils.KinesisStub
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

class KinesisProducerSpec extends FlatSpec with Matchers with BeforeAndAfterEach {
  override def beforeEach {
    KinesisStub.clear()
  }

  trait KinesisProducerTestContext {
    implicit val ec: ExecutionContext             = ExecutionContext.global
    implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)
    implicit val timer: Timer[IO]                 = IO.timer(ec)
    val result                                    = new UserRecordResult(List[Attempt]().asJava, "seq #", "shard #", true)
    val ops = IO {
      val future: SettableFuture[UserRecordResult] = SettableFuture.create()
      future.set(result)
      future
    }

  }

  "Publishing data to a Kinesis stream via writeToKinesis" should "successfully save data to the stream and return the successful result" in new KinesisProducerTestContext {
    val output = fs2.Stream
      .eval(IO.pure("someData"))
      .flatMap(i => fs2.Stream.emit(("partitionKey", ByteBuffer.wrap(i.getBytes))))
      .through(writeToKinesis[IO]("test-stream", TestKinesisProducerClient[IO](result, ops)))
      .attempt
      .compile
      .toVector
      .unsafeRunSync

    output.size should be(1)
    output.head.isRight should be(true)

    KinesisStub._data.size should be(1)
    KinesisStub._data.head should be(ByteBuffer.wrap("someData".getBytes))
  }

  "Publishing data to a Kinesis stream via writeToKinesis_" should "successfully save data to the stream" in new KinesisProducerTestContext {
    fs2.Stream
      .eval(IO.pure("someData"))
      .flatMap(i => fs2.Stream.emit(("partitionKey", ByteBuffer.wrap(i.getBytes))))
      .to(writeToKinesis_[IO]("test-stream", TestKinesisProducerClient[IO](result, ops)))
      .compile
      .toVector
      .unsafeRunSync

    KinesisStub._data.size should be(1)
    KinesisStub._data.head should be(ByteBuffer.wrap("someData".getBytes))
  }

  "Publishing data to a Kinesis stream via writeToKinesis1_" should "successfully save data to the stream bypassing original payload" in new KinesisProducerTestContext {
    implicit def encoder(in: String): ByteBuffer = ByteBuffer.wrap(in.getBytes)
    val res = fs2.Stream
      .eval(IO.pure("someData"))
      .flatMap(i => fs2.Stream.emit(("partitionKey", i)))
      .through(
        writeObjectToKinesis[IO, String]("test-stream", TestKinesisProducerClient[IO](result, ops)))
      .compile
      .toVector
      .unsafeRunSync

    KinesisStub._data.size should be(1)
    KinesisStub._data.head should be(ByteBuffer.wrap("someData".getBytes))
    res should be(Vector("someData" -> result))
  }

  "error thrown when invoking writeToKinesis" should "return an error in the stream" in new KinesisProducerTestContext {

    val output = fs2.Stream
      .eval(IO.pure("someData"))
      .flatMap(i => fs2.Stream.emit(("partitionKey", ByteBuffer.wrap(i.getBytes))))
      .through(writeToKinesis[IO]("test-stream", TestKinesisProducerClient[IO](result, IO {
        throw new Exception("couldn't connect to kinesis")
      })))
      .attempt
      .compile
      .toVector
      .unsafeRunSync

    output.size should be(1)
    output.head.isLeft should be(true)
    output.head.left.get.getMessage should be("couldn't connect to kinesis")
  }

  "error thrown when invoking writeToKinesis_" should "return an error in the stream" in new KinesisProducerTestContext {
    assertThrows[Exception] {
      fs2.Stream
        .eval(IO.pure("someData"))
        .flatMap(i => fs2.Stream.emit(("partitionKey", ByteBuffer.wrap(i.toString.getBytes))))
        .to(writeToKinesis_[IO]("test-stream", TestKinesisProducerClient[IO](result, IO {
          throw new Exception("couldn't connect to kinesis")
        })))
        .compile
        .toVector
        .unsafeRunSync
    }
  }

}
