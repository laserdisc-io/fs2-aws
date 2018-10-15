package fs2
package aws

import cats.effect.{IO, Effect}
import org.scalatest.{FlatSpec, Matchers}
import fs2.aws.kinesis.publisher._
// import fs2.aws.utils._

import com.amazonaws.services.kinesis.producer.{UserRecordResult, Attempt}
import com.google.common.util.concurrent.{ListenableFuture, SettableFuture}
import scala.collection.JavaConverters._

class KinesisProducerSpec extends FlatSpec with Matchers {
  trait KinesisProducerTestContext {
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

  "Publishing data to a Kinesis stream" should "successfully save data to the stream" in new KinesisProducerTestContext {
      fs2.Stream
        .eval(IO.pure("someData"))
        .flatMap(i => fs2.Stream.emits(i.toString.getBytes))
        .through(writeData[IO]("test-stream", kinesisProducerTestClient))
        .compile
        .toVector
        .unsafeRunSync

    KinesisStub._data.size should be(1)
    KinesisStub._data.head should be("someData".getBytes)
  }

  "error thrown when writing to stream" should "be handled appropriately" in new KinesisProducerTestContext {
    override val ops = IO {
      throw new Exception("couldn't conenct to kinesis")
    }

    fs2.Stream
      .eval(IO.pure("someData"))
      .flatMap(i => fs2.Stream.emits(i.toString.getBytes))
      .through(writeData[IO]("test-stream", kinesisProducerTestClient))
      .compile
      .toVector
      .unsafeRunSync
  }
}
