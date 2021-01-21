package fs2.aws.kinesis

import java.nio.ByteBuffer
import java.time.Instant

import cats.effect.{ ContextShift, IO, Sync, Timer }
import cats.implicits._
import fs2.aws.kinesis
import fs2.aws.testkit.SchedulerFactoryTestContext
import org.mockito.MockitoSugar.mock
import org.openjdk.jmh.annotations.{ Benchmark, Scope, State }
import software.amazon.awssdk.services.kinesis.model
import software.amazon.kinesis.lifecycle.events.{ InitializationInput, ProcessRecordsInput }
import software.amazon.kinesis.processor.RecordProcessorCheckpointer
import software.amazon.kinesis.retrieval.KinesisClientRecord
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._
object KinesisFlowBenchmark {

  implicit val ec: ExecutionContext             = ExecutionContext.global
  implicit val timer: Timer[IO]                 = IO.timer(ec)
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)

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
        processorContext <- IO.delay(new SchedulerFactoryTestContext(shards = 10))
        streamUnderTest  = kinesis.testkit.readFromKinesisStream[IO](processorContext)
        _ <- (
              streamUnderTest
              //                  .evalMap(r => Sync[IO].pure(r.record))
                .through(consumer.checkpointRecords[IO]())
                .take(state.records.size * 10)
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
