package fs2
package aws

import java.nio.ByteBuffer
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
    implicit val ec: ExecutionContext             = ExecutionContext.global
    implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)
    implicit val timer: Timer[IO]                 = IO.timer(ec)
    import fs2.aws.internal.Internal._
    val successResult = new UserRecordResult(List[Attempt]().asJava, "seq #", "shard #", true)
    val ops = IO {
      val future: SettableFuture[UserRecordResult] = SettableFuture.create()
      future.set(successResult)
      future
    }

    val kinesisProducerTestClient: KinesisProducerClient[IO] = new KinesisProducerClient[IO] {
      override def putData(streamName: String, partitionKey: String, data: ByteBuffer)(
          implicit e: Effect[IO]): IO[ListenableFuture[UserRecordResult]] = {
        KinesisStub.save(data)
        ops
      }
    }
  }

  "Publishing data to a Kinesis stream via writeToKinesis" should "successfully save data to the stream and return the successful result" in new KinesisProducerTestContext {
    val output = fs2.Stream
      .eval(IO.pure("someData"))
      .flatMap(i => fs2.Stream.emit(("partitionKey", ByteBuffer.wrap(i.toString.getBytes))))
      .through(writeToKinesis[IO]("test-stream", kinesisProducerTestClient))
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
      .flatMap(i => fs2.Stream.emit(("partitionKey", ByteBuffer.wrap(i.toString.getBytes))))
      .to(writeToKinesis_[IO]("test-stream", kinesisProducerTestClient))
      .compile
      .toVector
      .unsafeRunSync

    KinesisStub._data.size should be(1)
    KinesisStub._data.head should be(ByteBuffer.wrap("someData".getBytes))
  }

  "Publishing data to a Kinesis stream via writeToKinesis1_" should "successfully save data to the stream bypassing original payload" in new KinesisProducerTestContext {
    implicit def encoder(in: String): ByteBuffer = ByteBuffer.wrap(in.getBytes)
    val result = fs2.Stream
      .eval(IO.pure("someData"))
      .flatMap(i => fs2.Stream.emit(("partitionKey", i)))
      .through(writeToKinesis1[IO, String]("test-stream", kinesisProducerTestClient))
      .compile
      .toVector
      .unsafeRunSync

    KinesisStub._data.size should be(1)
    KinesisStub._data.head should be(ByteBuffer.wrap("someData".getBytes))
    result should be(Vector("someData" -> successResult))
  }

  "error thrown when invoking writeToKinesis" should "return an error in the stream" in new KinesisProducerTestContext {
    override val ops = IO {
      throw new Exception("couldn't connect to kinesis")
    }

    val output = fs2.Stream
      .eval(IO.pure("someData"))
      .flatMap(i => fs2.Stream.emit(("partitionKey", ByteBuffer.wrap(i.toString.getBytes))))
      .through(writeToKinesis[IO]("test-stream", kinesisProducerTestClient))
      .attempt
      .compile
      .toVector
      .unsafeRunSync

    output.size should be(1)
    output.head.isLeft should be(true)
    output.head.left.get.getMessage should be("couldn't connect to kinesis")
  }

  "error thrown when invoking writeToKinesis_" should "return an error in the stream" in new KinesisProducerTestContext {
    override val ops = IO {
      throw new Exception("couldn't connect to kinesis")
    }

    assertThrows[Exception] {
      fs2.Stream
        .eval(IO.pure("someData"))
        .flatMap(i => fs2.Stream.emit(("partitionKey", ByteBuffer.wrap(i.toString.getBytes))))
        .to(writeToKinesis_[IO]("test-stream", kinesisProducerTestClient))
        .compile
        .toVector
        .unsafeRunSync
    }
  }

}
