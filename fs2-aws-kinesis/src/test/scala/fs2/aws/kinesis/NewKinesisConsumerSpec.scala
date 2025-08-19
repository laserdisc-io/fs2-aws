package fs2.aws.kinesis

import cats.effect.unsafe.IORuntime
import cats.effect.{IO, Resource}
import cats.implicits.*
import fs2.Chunk
import fs2.aws.kinesis.models.KinesisModels.{AppName, StreamName}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.*
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.checkpoint.ShardRecordProcessorCheckpointer
import software.amazon.kinesis.coordinator.Scheduler
import software.amazon.kinesis.lifecycle.events.*
import software.amazon.kinesis.processor.{ShardRecordProcessor, ShardRecordProcessorFactory}
import software.amazon.kinesis.retrieval.KinesisClientRecord
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

import java.nio.ByteBuffer
import java.time.Instant
import java.util.concurrent.{CountDownLatch, Semaphore}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

class NewKinesisConsumerSpec
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterEach
    with Eventually
    with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val runtime: IORuntime   = IORuntime.global

  val logger = LoggerFactory.getLogger(getClass)

  implicit def sList2jList[A](sList: List[A]): java.util.List[A] = sList.asJava

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(10, Seconds)), interval = scaled(Span(500, Millis)))

  "KinesisWorker source" should "successfully read data from the Kinesis stream" in new WorkerContext with TestData {

    val res =
      streamResource
        .use(stream =>
          (
            stream.flatMap(fs2.Stream.chunk).take(1).compile.toList,
            IO.blocking {
              shard1Guard.await()
              recordProcessor.initialize(initializationInput)
              recordProcessor.processRecords(recordsInput.build())
            }
          ).parMapN { case (msgs, _) => msgs }
        )
        .unsafeToFuture()
        .futureValue

    val commitableRecord = res.head
    commitableRecord.record.data() should be(record.data())
    commitableRecord.recordProcessorStartingSequenceNumber shouldBe initializationInput
      .extendedSequenceNumber()
    commitableRecord.shardId shouldBe initializationInput.shardId()
    commitableRecord.millisBehindLatest shouldBe recordsInput.build().millisBehindLatest()
  }

  it should "Shutdown the worker if the stream is drained and has not failed" in new WorkerContext with TestData {
    (
      streamResource.use(stream =>
        stream
          .flatMap(fs2.Stream.chunk)
          .take(1)
          .compile
          .toList
          .flatMap(l => IO.blocking(latch.countDown()) >> IO.pure(l))
      ),
      IO.blocking {
        shard1Guard.await()
        recordProcessor.initialize(initializationInput)
        recordProcessor.processRecords(recordsInput.build())
      }
    ).parMapN { case (_, _) => () }.unsafeToFuture().futureValue

    verify(mockScheduler, times(1)).shutdown()
  }

  it should "shutdown the worker if the stream terminates" in new WorkerContext(errorStream = true) with TestData {
    intercept[Exception] {
      (
        streamResource.use(stream =>
          stream
            .flatMap(fs2.Stream.chunk)
            .take(1)
            .compile
            .toList
            .flatMap(l => IO.blocking(latch.countDown()) >> IO.pure(l))
        ),
        IO.blocking {
          shard1Guard.await()
          recordProcessor.initialize(initializationInput)
          recordProcessor.processRecords(recordsInput.build())
        }
      ).parMapN { case (_, _) => () }.unsafeToFuture().futureValue
    }

    verify(mockScheduler, times(1)).shutdown()
  }

  it should "not drop messages in case of back-pressure" in new WorkerContext with TestData {

    // Create and send 10 records (to match buffer size)
    val res = (
      streamResource.use(stream => stream.flatMap(fs2.Stream.chunk).take(60).compile.toList),
      IO.blocking {
        logger.info("about to acquire lock for record processor #1")
        shard1Guard.await()
        logger.info("acquired lock for record processor #1")
        recordProcessor.initialize(initializationInput)
        for (i <- 1 to 10) {
          val record = mock(classOf[KinesisClientRecord])
          when(record.sequenceNumber()).thenReturn(i.toString)
          recordProcessor.processRecords(recordsInput.records(List(record)).build())
        }
        logger.info("completed ingestion #1")
      },
      // Send a batch that exceeds the internal buffer size
      IO.blocking {
        logger.info("about to acquire lock for record processor #2")
        shard1Guard.await()
        logger.info("acquired lock for record processor #2")
        recordProcessor.initialize(initializationInput)
        logger.info("Initialized #2")
        for (i <- 1 to 50) {
          val record = mock(classOf[KinesisClientRecord])
          when(record.sequenceNumber()).thenReturn(i.toString)
          recordProcessor.processRecords(recordsInput.records(List(record)).build())
          logger.info(s"processed $i message #2")
        }
        logger.info("completed ingestion #2")
      }
    ).parMapN { case (msgs, _, _) => msgs }.unsafeToFuture().futureValue

    res should have size 60
  }

  it should "not drop messages in case of back-pressure with multiple shard workers" in new WorkerContext
    with TestData {

    val res = (
      streamResource.use(stream => stream.flatMap(fs2.Stream.chunk).take(10).compile.toList),
      IO.blocking {
        shard1Guard.await()
        recordProcessor.initialize(initializationInput)
        for (i <- 1 to 5) {
          val record = mock(classOf[KinesisClientRecord])
          when(record.sequenceNumber()).thenReturn(i.toString)
          recordProcessor.processRecords(recordsInput.records(List(record)).build())
        }
      },
      IO.blocking {
        shard2Guard.await()
        recordProcessor2.initialize(
          InitializationInput
            .builder()
            .shardId("shard2")
            .extendedSequenceNumber(ExtendedSequenceNumber.AT_TIMESTAMP)
            .build()
        )
        // Create and send 10 records (to match buffer size)
        for (i <- 1 to 5) {
          val record = mock(classOf[KinesisClientRecord])
          when(record.sequenceNumber()).thenReturn(i.toString)
          recordProcessor2.processRecords(recordsInput.records(List(record)).build())
        }
      }
    ).parMapN { case (msgs, _, _) => msgs }.unsafeToFuture().futureValue

    // Should process all 10 messages
    res should have size 10
  }

  it should "delay the end of shard checkpoint until all messages are drained" in new WorkerContext with TestData {
    val nRecords = 5

    val res: Seq[KinesisClientRecord] = (
      streamResource.use(stream =>
        stream
          .flatMap(fs2.Stream.chunk)
          .take(nRecords.toLong)
          // emulate message processing latency to reproduce the situation when End of Shard arrives BEFORE
          // all in-flight records are done
          .parEvalMap(3)(msg => IO.sleep(200.millis) >> IO.pure(msg))
          .through(
            k.checkpointRecords(
              KinesisCheckpointSettings(maxBatchSize = Int.MaxValue, maxBatchWait = 500.millis)
                .getOrElse(throw new Error())
            )
          )
          .compile
          .toList
          .flatMap(l => IO.blocking(latch.countDown()) >> IO.pure(l))
      ),
      IO.blocking {
        shard1Guard.await()
        recordProcessor.initialize(initializationInput)
        (1 to nRecords).foreach { i =>
          val record = mock(classOf[KinesisClientRecord])
          when(record.sequenceNumber()).thenReturn(i.toString)
          recordProcessor.processRecords(
            recordsInput.isAtShardEnd(i == 5).records(List(record)).build()
          )
        }
      } >> IO.blocking {
        // Immediately publish end of shard event
        recordProcessor.shardEnded(
          ShardEndedInput.builder().checkpointer(checkpointer).build()
        )
      }
    ).parMapN { case (msgs, _) => msgs }.unsafeToFuture().futureValue

    res should have size 5
  }

  "KinesisWorker checkpoint pipe" should "checkpoint batch of records with same sequence number" in new WorkerContext() {
    val lastRecordSemaphore = new Semaphore(1)

    val input = (1 to 3).map { i =>
      val record = mock(classOf[KinesisClientRecord])
      when(record.sequenceNumber()).thenReturn("1")
      when(record.subSequenceNumber()).thenReturn(i.toLong)
      new CommittableRecord(
        "shard-1",
        mock(classOf[ExtendedSequenceNumber]),
        1L,
        record,
        chunkedRecordProcessor,
        checkpointerShard1,
        lastRecordSemaphore
      )
    }

    startStream(input)

    eventually(timeout(1.second)) {
      verify(checkpointerShard1)
        .checkpoint(input.last.record.sequenceNumber(), input.last.record.subSequenceNumber())
    }
  }

  it should "checkpoint batch of records of different shards" in new WorkerContext() {
    val checkpointerShard2 = mock(classOf[ShardRecordProcessorCheckpointer])

    val lastRecordSemaphore = new Semaphore(1)

    val input = (1L to 6L).map { i =>
      if (i <= 3) {
        val record = mock(classOf[KinesisClientRecord])
        when(record.sequenceNumber()).thenReturn(i.toString)
        new CommittableRecord(
          "shard-1",
          mock(classOf[ExtendedSequenceNumber]),
          i,
          record,
          chunkedRecordProcessor,
          checkpointerShard1,
          lastRecordSemaphore
        )
      } else {
        val record = mock(classOf[KinesisClientRecord])
        when(record.sequenceNumber()).thenReturn(i.toString)
        new CommittableRecord(
          "shard-2",
          mock(classOf[ExtendedSequenceNumber]),
          i,
          record,
          chunkedRecordProcessor,
          checkpointerShard2,
          lastRecordSemaphore
        )
      }
    }

    startStream(input)

    eventually(timeout(3.seconds)) {
      verify(checkpointerShard1)
        .checkpoint(input(2).record.sequenceNumber(), input(2).record.subSequenceNumber())
      verify(checkpointerShard2)
        .checkpoint(input.last.record.sequenceNumber(), input.last.record.subSequenceNumber())
    }

  }

  it should "checkpoint one last time if the RecordProcessor has reached the end of the shard" in new WorkerContext() {
    chunkedRecordProcessor.shardEnded(
      ShardEndedInput.builder().checkpointer(checkpointerShard1).build()
    )

    val input = (1 to 3).map { i =>
      val record = mock(classOf[KinesisClientRecord])
      when(record.sequenceNumber()).thenReturn("1")
      when(record.subSequenceNumber()).thenReturn(i.toLong)
      new CommittableRecord(
        "shard-1",
        mock(classOf[ExtendedSequenceNumber]),
        1L,
        record,
        chunkedRecordProcessor,
        checkpointerShard1,
        chunkedRecordProcessor.lastRecordSemaphore,
        i == 3
      )
    }

    startStream(input)

    verify(checkpointerShard1, times(1)).checkpoint()
  }

  it should "fail with Exception if checkpoint action fails" in new WorkerContext() {
    val checkpointer = mock(classOf[ShardRecordProcessorCheckpointer])

    val lastRecordSemaphore = new Semaphore(1)
    val record              = mock(classOf[KinesisClientRecord])
    when(record.sequenceNumber()).thenReturn("1")

    val input = new CommittableRecord(
      "shard-1",
      mock(classOf[ExtendedSequenceNumber]),
      1L,
      record,
      chunkedRecordProcessor,
      checkpointer,
      lastRecordSemaphore
    )

    val failure = new RuntimeException("you have no power here")
    when(checkpointer.checkpoint(record.sequenceNumber, record.subSequenceNumber))
      .thenThrow(failure)

    (the[RuntimeException] thrownBy fs2.Stream
      .emits(Seq(input))
      .through(k.checkpointRecords(settings))
      .compile
      .toVector
      .unsafeRunSync() should have).message("you have no power here")

    eventually(
      verify(checkpointer)
        .checkpoint(input.record.sequenceNumber(), input.record.subSequenceNumber())
    )
  }

  it should "bypass all items when checkpoint" in new WorkerContext() {
    val checkpointer = mock(classOf[ShardRecordProcessorCheckpointer])

    val lastRecordSemaphore = new Semaphore(1)
    val record              = mock(classOf[KinesisClientRecord])
    when(record.sequenceNumber()).thenReturn("1")

    val input = (1L to 100L).map(idx =>
      new CommittableRecord(
        s"shard-1",
        mock(classOf[ExtendedSequenceNumber]),
        idx,
        record,
        chunkedRecordProcessor,
        checkpointer,
        lastRecordSemaphore
      )
    )

    fs2.Stream
      .emits(input)
      .through(k.checkpointRecords(settings))
      .compile
      .toVector
      .unsafeRunSync() should have size 100
  }

  abstract private class WorkerContext(errorStream: Boolean = false) {

    val shard1Guard                        = new CountDownLatch(1)
    val shard2Guard                        = new CountDownLatch(1)
    val latch                              = new CountDownLatch(1)
    protected val mockScheduler: Scheduler = mock(classOf[Scheduler])

//    var recordProcessorFactory: ShardRecordProcessorFactory = _
    var recordProcessor: ShardRecordProcessor  = _
    var recordProcessor2: ShardRecordProcessor = _
    val chunkedRecordProcessor                 = new ChunkedRecordProcessor(_ => ())

    doAnswer(_ => latch.await()).when(mockScheduler).run()
//    doThrow(new RuntimeException("boom"))
//      .when(mockScheduler)
//      .run()

    val builder: ShardRecordProcessorFactory => IO[Scheduler] = { (x: ShardRecordProcessorFactory) =>
//      recordProcessorFactory = x
      recordProcessor = x.shardRecordProcessor()
      shard1Guard.countDown()
      recordProcessor2 = x.shardRecordProcessor()
      shard2Guard.countDown()
      mockScheduler.pure[IO]
    }

    val config: KinesisConsumerSettings =
      KinesisConsumerSettings("testStream", "testApp")

    val appNameRes    = Resource.eval(IO.fromEither(AppName(config.appName).leftMap(new IllegalArgumentException(_))))
    val streamNameRes =
      Resource.eval(IO.fromEither(StreamName(config.streamName).leftMap(new IllegalArgumentException(_))))

    val streamResource: Resource[IO, fs2.Stream[IO, Chunk[CommittableRecord]]] = DefaultKinesisStreamBuilder[IO]()
      .withAppName(appNameRes)
      .withKinesisClient(mock(classOf[KinesisAsyncClient]))
      .withDynamoDBClient(mock(classOf[DynamoDbAsyncClient]))
      .withCloudWatchClient(mock(classOf[CloudWatchAsyncClient]))
      .withDefaultSchedulerId
      .withSingleStreamTracker(streamNameRes)
      .withDefaultStreamTracker
      .withDefaultSchedulerConfigs
      .withDefaultBufferSize
      .withScheduler { cb =>
        val x = cb.shardRecordProcessorFactory()
        recordProcessor = x.shardRecordProcessor()
        shard1Guard.countDown()
        recordProcessor2 = x.shardRecordProcessor()
        shard2Guard.countDown()
        Resource.pure(mockScheduler)
      }
      .build
      .map(stream =>
        stream
          .evalTap(_ => IO.sleep(100.millis))
          .map(i => if (errorStream) throw new Exception("boom") else i)
          .onFinalize(IO.delay(latch.countDown()))
      )

    val k: Kinesis[IO] = Kinesis.create[IO](builder)

    val settings: KinesisCheckpointSettings =
      KinesisCheckpointSettings(maxBatchSize = Int.MaxValue, maxBatchWait = 500.millis)
        .getOrElse(throw new Error())

    val checkpointerShard1: ShardRecordProcessorCheckpointer = mock(classOf[ShardRecordProcessorCheckpointer])

    def startStream(input: Seq[CommittableRecord]): Seq[KinesisClientRecord] =
      fs2.Stream
        .emits(input)
        .through(k.checkpointRecords(settings))
        .compile
        .toList
        .unsafeRunSync()
  }

  private trait TestData {
    @volatile var endOfShardSeen = false

    protected val checkpointer: ShardRecordProcessorCheckpointer = mock(
      classOf[ShardRecordProcessorCheckpointer]
    )

    doAnswer { _ =>
      endOfShardSeen = true
      null
    }.when(checkpointer).checkpoint()

    doAnswer { _ =>
      if (endOfShardSeen) throw new Exception("Checkpointing after End Of Shard")
      null
    }.when(checkpointer).checkpoint(any[String], any[Long])

    val initializationInput: InitializationInput =
      InitializationInput
        .builder()
        .shardId("shardId")
        .extendedSequenceNumber(ExtendedSequenceNumber.AT_TIMESTAMP)
        .build()

    val record: KinesisClientRecord =
      KinesisClientRecord
        .builder()
        .approximateArrivalTimestamp(Instant.now())
        .partitionKey("partitionKey")
        .sequenceNumber("sequenceNum")
        .data(ByteBuffer.wrap("test".getBytes))
        .build()

    val recordsInput: ProcessRecordsInput.ProcessRecordsInputBuilder =
      ProcessRecordsInput
        .builder()
        .checkpointer(checkpointer)
        .millisBehindLatest(1L)
        .records(List(record).asJava)
  }

}
