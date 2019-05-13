package fs2.aws

import java.nio.ByteBuffer
import java.time.Instant
import java.util.concurrent.Semaphore

import cats.effect.{ContextShift, IO, Timer}
import fs2.aws.kinesis.consumer._
import fs2.aws.kinesis.{
  CommittableRecord,
  KinesisCheckpointSettings,
  KinesisConsumerSettings,
  SingleRecordProcessor
}
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.scalatest.concurrent.Eventually
import org.scalatest.time._
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import software.amazon.awssdk.regions.Region
import software.amazon.kinesis.checkpoint.ShardRecordProcessorCheckpointer
import software.amazon.kinesis.coordinator.Scheduler
import software.amazon.kinesis.lifecycle.events._
import software.amazon.kinesis.processor.{ShardRecordProcessor, ShardRecordProcessorFactory}
import software.amazon.kinesis.retrieval.KinesisClientRecord
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class KinesisConsumerSpec extends FlatSpec with Matchers with BeforeAndAfterEach with Eventually {

  implicit val ec: ExecutionContext             = ExecutionContext.global
  implicit val timer: Timer[IO]                 = IO.timer(ec)
  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)

  implicit def sList2jList[A](sList: List[A]): java.util.List[A] = sList.asJava

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(2, Seconds)), interval = scaled(Span(5, Millis)))

  "KinesisWorker source" should "successfully read data from the Kinesis stream" in new WorkerContext
  with TestData {
    semaphore.acquire()
    recordProcessor.initialize(initializationInput)
    recordProcessor.processRecords(recordsInput.build())

    eventually(verify(mockScheduler, times(1)).run())

    eventually(timeout(1.second)) {
      val commitableRecord = output.head
      commitableRecord.record.data() should be(record.data())
      commitableRecord.recordProcessorStartingSequenceNumber shouldBe initializationInput
        .extendedSequenceNumber()
      commitableRecord.shardId shouldBe initializationInput.shardId()
      commitableRecord.millisBehindLatest shouldBe recordsInput.build().millisBehindLatest()
    }
    semaphore.release()
  }

  it should "not shutdown the worker if the stream is drained but has not failed" in new WorkerContext
  with TestData {
    semaphore.acquire()
    recordProcessor.initialize(initializationInput)
    recordProcessor.processRecords(recordsInput.records(List(record)).build())

    eventually(verify(mockScheduler, times(0)).shutdown())
    semaphore.release()
  }

  it should "shutdown the worker if the stream terminates" in new WorkerContext(errorStream = true)
  with TestData {
    semaphore.acquire()
    recordProcessor.initialize(initializationInput)
    recordProcessor.processRecords(recordsInput.records(List(record)).build())

    eventually(verify(mockScheduler, times(1)).shutdown())
  }

  it should "not drop messages in case of back-pressure" in new WorkerContext with TestData {
    semaphore.acquire()
    // Create and send 10 records (to match buffer size)
    for (i <- 1 to 10) {
      val record = mock(classOf[KinesisClientRecord])
      when(record.sequenceNumber()).thenReturn(i.toString)
      recordProcessor.processRecords(recordsInput.records(List(record)).build())
    }

    // Should process all 10 messages
    eventually(output.size shouldBe 10)

    // Send a batch that exceeds the internal buffer size
    for (i <- 1 to 50) {
      val record = mock(classOf[KinesisClientRecord])
      when(record.sequenceNumber()).thenReturn(i.toString)
      recordProcessor.processRecords(recordsInput.records(List(record)).build())
    }

    // Should have processed all 60 messages
    eventually(output.size shouldBe 60)

    eventually(verify(mockScheduler, times(0)).shutdown())
    semaphore.release()
  }

  it should "not drop messages in case of back-pressure with multiple shard workers" in new WorkerContext
  with TestData {
    semaphore.acquire()
    recordProcessor.initialize(initializationInput)
    recordProcessor2.initialize(
      InitializationInput
        .builder()
        .shardId("shard2")
        .extendedSequenceNumber(ExtendedSequenceNumber.AT_TIMESTAMP)
        .build())

    // Create and send 10 records (to match buffer size)
    for (i <- 1 to 5) {
      val record = mock(classOf[KinesisClientRecord])
      when(record.sequenceNumber()).thenReturn(i.toString)
      recordProcessor.processRecords(recordsInput.records(List(record)).build())
      recordProcessor2.processRecords(recordsInput.records(List(record)).build())
    }

    // Should process all 10 messages
    eventually(output.size shouldBe 10)

    // Each shard is assigned its own worker thread, so we get messages
    // from each thread simultaneously.
    def simulateWorkerThread(rp: ShardRecordProcessor): Future[Unit] = {
      Future {
        for (i <- 1 to 25) { // 10 is a buffer size
          val record = mock(classOf[KinesisClientRecord])
          when(record.sequenceNumber()).thenReturn(i.toString)
          rp.processRecords(recordsInput.records(List(record)).build())
        }
      }
    }

    simulateWorkerThread(recordProcessor)
    simulateWorkerThread(recordProcessor2)

    // Should have processed all 60 messages
    eventually(output.size shouldBe 60)
    semaphore.release()
  }

  "KinesisWorker checkpoint pipe" should "checkpoint batch of records with same sequence number" in new KinesisWorkerCheckpointContext {
    val input = (1 to 3) map { i =>
      val record = mock(classOf[KinesisClientRecord])
      when(record.sequenceNumber()).thenReturn("1")
      when(record.subSequenceNumber()).thenReturn(i.toLong)
      new CommittableRecord(
        "shard-1",
        mock(classOf[ExtendedSequenceNumber]),
        1L,
        record,
        recordProcessor,
        checkpointerShard1
      )
    }

    startStream(input)

    eventually(timeout(1.second)) {
      verify(checkpointerShard1).checkpoint(input.last.record.sequenceNumber(),
                                            input.last.record.subSequenceNumber())
    }
  }

  it should "checkpoint batch of records of different shards" in new KinesisWorkerCheckpointContext {
    val checkpointerShard2 = mock(classOf[ShardRecordProcessorCheckpointer])

    val input = (1 to 6) map { i =>
      if (i <= 3) {
        val record = mock(classOf[KinesisClientRecord])
        when(record.sequenceNumber()).thenReturn(i.toString)
        new CommittableRecord(
          "shard-1",
          mock(classOf[ExtendedSequenceNumber]),
          i,
          record,
          recordProcessor,
          checkpointerShard1
        )
      } else {
        val record = mock(classOf[KinesisClientRecord])
        when(record.sequenceNumber()).thenReturn(i.toString)
        new CommittableRecord(
          "shard-2",
          mock(classOf[ExtendedSequenceNumber]),
          i,
          record,
          recordProcessor,
          checkpointerShard2
        )
      }
    }

    startStream(input)

    eventually(timeout(3.seconds)) {
      verify(checkpointerShard1).checkpoint(input(2).record.sequenceNumber(),
                                            input(2).record.subSequenceNumber())
      verify(checkpointerShard2).checkpoint(input.last.record.sequenceNumber(),
                                            input.last.record.subSequenceNumber())
    }

  }

  it should "checkpoint one last time if the RecordProcessor has reached the end of the shard" in new KinesisWorkerCheckpointContext {
    recordProcessor.shardEnded(ShardEndedInput.builder().checkpointer(checkpointerShard1).build())

    val input = (1 to 3) map { i =>
      val record = mock(classOf[KinesisClientRecord])
      when(record.sequenceNumber()).thenReturn("1")
      when(record.subSequenceNumber()).thenReturn(i.toLong)
      new CommittableRecord(
        "shard-1",
        mock(classOf[ExtendedSequenceNumber]),
        1L,
        record,
        recordProcessor,
        checkpointerShard1
      )
    }

    startStream(input)

    verify(checkpointerShard1, times(1)).checkpoint()
  }

  it should "fail with Exception if checkpoint action fails" in new KinesisWorkerCheckpointContext {
    val checkpointer = mock(classOf[ShardRecordProcessorCheckpointer])

    val record = mock(classOf[KinesisClientRecord])
    when(record.sequenceNumber()).thenReturn("1")

    val input = new CommittableRecord(
      "shard-1",
      mock(classOf[ExtendedSequenceNumber]),
      1L,
      record,
      recordProcessor,
      checkpointer
    )

    val failure = new RuntimeException("you have no power here")
    when(checkpointer.checkpoint(record.sequenceNumber, record.subSequenceNumber))
      .thenThrow(failure)

    the[RuntimeException] thrownBy fs2.Stream
      .emits(Seq(input))
      .through(checkpointRecords[IO](settings))
      .compile
      .toVector
      .unsafeRunSync should have message "you have no power here"

    eventually(
      verify(checkpointer).checkpoint(input.record.sequenceNumber(),
                                      input.record.subSequenceNumber()))
  }

  it should "bypass all items when checkpoint" in new KinesisWorkerCheckpointContext {
    val checkpointer = mock(classOf[ShardRecordProcessorCheckpointer])

    val record = mock(classOf[KinesisClientRecord])
    when(record.sequenceNumber()).thenReturn("1")

    val input = (1 to 100).map(
      idx =>
        new CommittableRecord(
          s"shard-1",
          mock(classOf[ExtendedSequenceNumber]),
          idx,
          record,
          recordProcessor,
          checkpointer
      ))

    fs2.Stream
      .emits(input)
      .through(checkpointRecords[IO](settings))
      .compile
      .toVector
      .unsafeRunSync should have size 100
  }

  private abstract class WorkerContext(backpressureTimeout: FiniteDuration = 1.minute,
                                       errorStream: Boolean = false) {

    val semaphore = new Semaphore(1)
    semaphore.acquire()
    var output: List[CommittableRecord] = List()

    protected val mockScheduler: Scheduler = mock(classOf[Scheduler])

    when(mockScheduler.run()).thenAnswer(new org.mockito.stubbing.Answer[Unit] {
      def answer(invocation: InvocationOnMock): Unit = ()
    })

    when(mockScheduler.shutdown()).thenAnswer(new org.mockito.stubbing.Answer[Unit] {
      def answer(invocation: InvocationOnMock): Unit = ()
    })

    var recordProcessorFactory: ShardRecordProcessorFactory = _
    var recordProcessor: ShardRecordProcessor               = _
    var recordProcessor2: ShardRecordProcessor              = _

    val builder = { x: ShardRecordProcessorFactory =>
      recordProcessorFactory = x
      recordProcessor = x.shardRecordProcessor()
      recordProcessor2 = x.shardRecordProcessor()
      semaphore.release()
      mockScheduler
    }

    val config =
      KinesisConsumerSettings("testStream", "testApp", Region.US_EAST_1, 10, 10, 10.seconds).right.get

    val stream =
      readFromKinesisStream[IO](config, builder)
        .through(_.evalMap(i => IO(output = output :+ i)))
        .map(i => if (errorStream) throw new Exception("boom") else i)
        .compile
        .toVector
        .unsafeRunAsync(_ => ())
  }

  private trait TestData {
    protected val checkpointer: ShardRecordProcessorCheckpointer = mock(
      classOf[ShardRecordProcessorCheckpointer])

    val initializationInput = {
      InitializationInput
        .builder()
        .shardId("shardId")
        .extendedSequenceNumber(ExtendedSequenceNumber.AT_TIMESTAMP)
        .build()
    }

    val record: KinesisClientRecord =
      KinesisClientRecord
        .builder()
        .approximateArrivalTimestamp(Instant.now())
        .partitionKey("partitionKey")
        .sequenceNumber("sequenceNum")
        .data(ByteBuffer.wrap("test".getBytes))
        .build()

    val recordsInput =
      ProcessRecordsInput
        .builder()
        .checkpointer(checkpointer)
        .millisBehindLatest(1L)
        .records(List(record).asJava)
  }

  private trait KinesisWorkerCheckpointContext {
    val recordProcessor    = new SingleRecordProcessor(_ => (), 1.seconds)
    val checkpointerShard1 = mock(classOf[ShardRecordProcessorCheckpointer])
    val settings =
      KinesisCheckpointSettings(maxBatchSize = Int.MaxValue, maxBatchWait = 500.millis).right
        .getOrElse(throw new Error())

    def startStream(input: Seq[CommittableRecord]) =
      fs2.Stream
        .emits(input)
        .through(checkpointRecords[IO](settings))
        .compile
        .toVector
        .unsafeRunAsync(_ => ())
  }
}
