package fs2

package aws

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import com.amazonaws.services.kinesis.producer.{Attempt, UserRecordResult}
import com.google.common.util.concurrent.SettableFuture
import fs2.aws.kinesis.publisher.*
import fs2.aws.utils.KinesisStub
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.ByteBuffer
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*

class KinesisProducerSpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach {
  override def beforeEach(): Unit =
    KinesisStub.clear()

  trait KinesisProducerTestContext {
    implicit val ec: ExecutionContext = ExecutionContext.global
    implicit val runtime: IORuntime   = IORuntime.global
    val result                        = new UserRecordResult(List[Attempt]().asJava, "seq #", "shard #", true)

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
      .through(
        writeToKinesis[IO]("test-stream", producer = TestKinesisProducerClient[IO](result, ops))
      )
      .attempt
      .compile
      .toVector
      .unsafeRunSync()

    output.size should be(1)
    output.head.isRight should be(true)

    KinesisStub._data.size should be(1)
    KinesisStub._data.head should be(ByteBuffer.wrap("someData".getBytes))
  }

  "Publishing data to a Kinesis stream via writeToKinesis_" should "successfully save data to the stream" in new KinesisProducerTestContext {
    fs2.Stream
      .eval(IO.pure("someData"))
      .flatMap(i => fs2.Stream.emit(("partitionKey", ByteBuffer.wrap(i.getBytes))))
      .through(
        writeToKinesis_[IO]("test-stream", producer = TestKinesisProducerClient[IO](result, ops))
      )
      .compile
      .toVector
      .unsafeRunSync()

    KinesisStub._data.size should be(1)
    KinesisStub._data.head should be(ByteBuffer.wrap("someData".getBytes))
  }

  "Publishing data to a Kinesis stream via writeToKinesis1_" should "successfully save data to the stream bypassing original payload" in new KinesisProducerTestContext {
    implicit def encoder(in: String): ByteBuffer = ByteBuffer.wrap(in.getBytes)

    val res = fs2.Stream
      .eval(IO.pure("someData"))
      .flatMap(i => fs2.Stream.emit(("partitionKey", i)))
      .through(
        writeObjectToKinesis[IO, String](
          "test-stream",
          producer = TestKinesisProducerClient[IO](result, ops)
        )
      )
      .compile
      .toVector
      .unsafeRunSync()

    KinesisStub._data.size should be(1)
    KinesisStub._data.head should be(ByteBuffer.wrap("someData".getBytes))
    res should be(Vector("someData" -> result))
  }

  "error thrown when invoking writeToKinesis" should "return an error in the stream" in new KinesisProducerTestContext {

    val output = fs2.Stream
      .eval(IO.pure("someData"))
      .flatMap(i => fs2.Stream.emit(("partitionKey", ByteBuffer.wrap(i.getBytes))))
      .through(
        writeToKinesis[IO](
          "test-stream",
          producer = TestKinesisProducerClient[IO](
            result,
            IO {
              throw new Exception("couldn't connect to kinesis")
            }
          )
        )
      )
      .attempt
      .compile
      .toVector
      .unsafeRunSync()

    output.size should be(1)
    output.head.isLeft should be(true)
    output.head.left.getOrElse(throw new Error()).getMessage should be(
      "couldn't connect to kinesis"
    )
  }

  "error thrown when invoking writeToKinesis_" should "return an error in the stream" in new KinesisProducerTestContext {
    assertThrows[Exception] {
      fs2.Stream
        .eval(IO.pure("someData"))
        .flatMap(i => fs2.Stream.emit(("partitionKey", ByteBuffer.wrap(i.toString.getBytes))))
        .through(
          writeToKinesis_[IO](
            "test-stream",
            producer = TestKinesisProducerClient[IO](
              result,
              IO {
                throw new Exception("couldn't connect to kinesis")
              }
            )
          )
        )
        .compile
        .toVector
        .unsafeRunSync()
    }
  }

}
