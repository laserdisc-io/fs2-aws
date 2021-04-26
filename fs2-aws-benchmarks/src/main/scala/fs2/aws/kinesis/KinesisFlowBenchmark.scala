package fs2.aws.kinesis

import cats.effect.unsafe.IORuntime
import cats.effect.{ IO, Sync }
import cats.effect.implicits._
import cats.syntax.parallel._
import fs2.aws.testkit.SchedulerFactoryTestContext
import org.mockito.MockitoSugar.mock
import org.openjdk.jmh.annotations.{ Benchmark, Scope, State }
import software.amazon.awssdk.services.kinesis.model
import software.amazon.kinesis.lifecycle.events.{ InitializationInput, ProcessRecordsInput }
import software.amazon.kinesis.processor.RecordProcessorCheckpointer
import software.amazon.kinesis.retrieval.KinesisClientRecord
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

import java.nio.ByteBuffer
import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._
object KinesisFlowBenchmark {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val runtime: IORuntime   = IORuntime.global

  @State(Scope.Thread)
  class ThreadState {
    val records = (0 to 50000).map(seq =>
      KinesisClientRecord
        .builder()
        .sequenceNumber(s"$seq")
        .approximateArrivalTimestamp(Instant.now())
        .data(ByteBuffer.wrap(Array.fill(20)((scala.util.Random.nextInt(256) - 128).toByte)))
        .partitionKey(s"shard${scala.util.Random.nextInt(256)}")
        .encryptionType(model.EncryptionType.NONE)
        .build()
    )
  }

  class KinesisFlowBenchmark {
    @Benchmark
    def KinesisStream(state: ThreadState): Unit =
      (for {
        processorContext <- IO.delay(new SchedulerFactoryTestContext[IO](shards = 10))
        k                = Kinesis.create(processorContext)
        streamUnderTest  = k.readFromKinesisStream("foo", "bar")
        _ <- (
              streamUnderTest
                .through(k.checkpointRecords(KinesisCheckpointSettings.defaultInstance))
                .take(state.records.size * 10)
                .onFinalize(IO.delay(processorContext.latch.countDown()))
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
                            .records(state.records.asJava)
                            .build()
                        )
                      }
                  }.parUnorderedSequence
                }
            ).parMapN { case _ => () }
      } yield ()).unsafeRunSync()
  }
}
