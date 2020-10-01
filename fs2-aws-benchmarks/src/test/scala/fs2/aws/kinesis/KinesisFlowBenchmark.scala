package fs2.aws.kinesis

import java.nio.ByteBuffer
import java.time.Instant

import cats.effect.{ ContextShift, IO, Sync, Timer }
import fs2.aws.kinesis
import fs2.aws.testkit.SchedulerFactoryTestContext
import org.scalameter.api._
import software.amazon.awssdk.services.kinesis.model
import software.amazon.kinesis.lifecycle.events.{ InitializationInput, ProcessRecordsInput }
import software.amazon.kinesis.retrieval.KinesisClientRecord
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._
import cats.implicits._
import org.mockito.MockitoSugar.mock
import software.amazon.kinesis.processor.RecordProcessorCheckpointer

object KinesisFlowBenchmark extends Bench.LocalTime {

  implicit val ec: ExecutionContext             = ExecutionContext.global
  implicit val timer: Timer[IO]                 = IO.timer(ec)
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)

  val sizes = Gen.range("size")(300, 1500, 300)
  val records = for {
    size <- sizes
  } yield {
    (0 to size).map(_ =>
      KinesisClientRecord
        .builder()
        .sequenceNumber("0")
        .approximateArrivalTimestamp(Instant.now())
        .data(ByteBuffer.wrap(Array.fill(20)((scala.util.Random.nextInt(256) - 128).toByte)))
        .partitionKey(s"shard${scala.util.Random.nextInt(256)}")
        .encryptionType(model.EncryptionType.NONE)
        .build()
    )
  }

  performance of "Kinesis Stream" in {
    measure method "stream" in {
      using(records) in { records =>
        (for {
          processorContext <- IO.delay(new SchedulerFactoryTestContext(shards = 10))
          streamUnderTest  = kinesis.testkit.readFromKinesisStream[IO](processorContext)
          _ <- (
                streamUnderTest
                //                  .evalMap(r => Sync[IO].pure(r.record))
                  .through(consumer.checkpointRecords[IO]())
                  .take(records.size * 10)
                  .compile
                  .drain,
                Sync[IO]
                  .delay(processorContext.getShardProcessors.zipWithIndex)
                  .flatMap {
                    _.map {
                      case (p, idx) =>
                        Sync[IO].delay {
                          val shard = s"shard$idx"
                          p.initialize(
                            InitializationInput
                              .builder()
                              .shardId(shard)
                              .extendedSequenceNumber(ExtendedSequenceNumber.AT_TIMESTAMP)
                              .build()
                          )

                          p.processRecords(
                            ProcessRecordsInput
                              .builder()
                              .checkpointer(mock[RecordProcessorCheckpointer])
                              .records(records.asJava)
                              .build()
                          )
                        }
                    }.parUnorderedSequence
                  }
              ).parMapN { case _ => () }
        } yield ()).unsafeRunSync()
      }
    }
  }
}
