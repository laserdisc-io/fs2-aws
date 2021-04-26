package fs2.aws.dynamodb

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.implicits._
import com.amazonaws.services.dynamodbv2.model
import com.amazonaws.services.dynamodbv2.model.{ AttributeValue, StreamRecord }
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{
  IRecordProcessor,
  IRecordProcessorFactory
}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{ ShutdownReason, Worker }
import com.amazonaws.services.kinesis.clientlibrary.types._
import com.amazonaws.services.kinesis.model.Record
import fs2.aws.dynamodb._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time._

import java.util.Date
import java.util.concurrent.{ CountDownLatch, Phaser, Semaphore }
import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class DynamoDBConsumerSpec
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterEach
    with Eventually {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val runtime: IORuntime   = IORuntime.global

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(2, Seconds)), interval = scaled(Span(5, Millis)))

  "KinesisWorker source" should "successfully read data from the Kinesis stream" in new WorkerContext
    with TestData {

    val res = (
      stream.take(1).compile.toList,
      IO.delay {
        semaphore.acquire()
        recordProcessor.initialize(initializationInput)
        recordProcessor.processRecords(recordsInput)
      }
    ).parMapN { case (msgs, _) => msgs }.unsafeRunSync()

    verify(mockWorker, times(1)).run()

    val commitableRecord = res.head
    commitableRecord.record.getData                        should be(record.getData)
    commitableRecord.recordProcessorStartingSequenceNumber shouldBe initializationInput.getExtendedSequenceNumber
    commitableRecord.shardId                               shouldBe initializationInput.getShardId
    commitableRecord.millisBehindLatest                    shouldBe recordsInput.getMillisBehindLatest
  }

  it should "Shutdown the worker if the stream is drained and has not failed" in new WorkerContext
    with TestData {
    (
      stream.take(1).compile.toList,
      IO.delay {
        semaphore.acquire()
        recordProcessor.initialize(initializationInput)
        recordProcessor.processRecords(recordsInput)
      }
    ).parMapN { case (_, _) => () }.unsafeRunSync()

    verify(mockWorker, times(1)).shutdown()
  }

  it should "shutdown the worker if the stream terminates" in new WorkerContext(errorStream = true)
    with TestData {
    intercept[Exception] {
      (
        stream.take(1).compile.toList,
        IO.delay {
          semaphore.acquire()
          recordProcessor.initialize(initializationInput)
          recordProcessor.processRecords(recordsInput)
        }
      ).parMapN { case (_, _) => () }.unsafeRunSync()
    }
    verify(mockWorker, times(1)).shutdown()
  }

  it should "not drop messages in case of back-pressure, Create and send 10 records (to match buffer size)" in new WorkerContext
    with TestData {
    //
    val res = (
      stream.take(10).compile.toList,
      IO.delay {
        semaphore.acquire()
        recordProcessor.initialize(initializationInput)
        for (i <- 1 to 10) {
          val record: Record = mock(classOf[RecordAdapter])
          when(record.getSequenceNumber).thenReturn(i.toString)
          recordProcessor.processRecords(recordsInput.withRecords(List(record).asJava))
        }
      }
    ).parMapN { case (msgs, _) => msgs }.unsafeRunSync()

    res should have size 10
  }

  it should "not drop messages in case of back-pressure, Send a batch that exceeds the internal buffer size" in new WorkerContext
    with TestData {
    val res2 = (
      stream.take(50).compile.toList,
      IO.delay {
        semaphore.acquire()
        recordProcessor.initialize(initializationInput)
        for (i <- 1 to 50) {
          val record: Record = mock(classOf[RecordAdapter])
          when(record.getSequenceNumber).thenReturn(i.toString)
          recordProcessor.processRecords(recordsInput.withRecords(List(record).asJava))
        }
      }
    ).parMapN { case (msgs, _) => msgs }.unsafeRunSync()

    res2 should have size 50
  }

  it should "not drop messages in case of back-pressure with multiple shard workers" in new WorkerContext
    with TestData {

    val res = (
      stream.take(10).compile.toList,
      IO.delay {
        semaphore.acquire()
        recordProcessor.initialize(initializationInput)
        for (i <- 1 to 5) {
          val record: Record = mock(classOf[RecordAdapter])
          when(record.getSequenceNumber).thenReturn(i.toString)
          recordProcessor.processRecords(recordsInput.withRecords(List(record).asJava))
        }
      },
      IO.delay {
        semaphore.acquire()
        recordProcessor2.initialize(initializationInput.withShardId("shard2"))

        // Create and send 10 records (to match buffer size)
        for (i <- 1 to 5) {
          val record: Record = mock(classOf[RecordAdapter])
          when(record.getSequenceNumber).thenReturn(i.toString)
          recordProcessor2.processRecords(recordsInput.withRecords(List(record).asJava))
        }
      }
    ).parMapN { case (msgs, _, _) => msgs }.unsafeRunSync()

    // Should process all 10 messages
    res should have size 10
  }

  it should "delay the end of shard checkpoint until all messages are drained" in new WorkerContext
    with TestData {
    val nRecords = 5
    val res = (
      stream
        .take(nRecords)
        //emulate message processing latency to reproduce the situation when End of Shard arrives BEFORE
        // all in-flight records are done
        .parEvalMap(3)(msg => IO.sleep(200 millis) >> IO.pure(msg))
        .through(
          checkpointRecords[IO](
            KinesisCheckpointSettings(maxBatchSize = Int.MaxValue, maxBatchWait = 500.millis)
              .getOrElse(throw new Error())
          )
        )
        .compile
        .toList,
      IO.delay {
        semaphore.acquire()
        recordProcessor.initialize(initializationInput)
        (1 to nRecords).foreach { i =>
          val record: Record = mock(classOf[RecordAdapter])
          when(record.getSequenceNumber).thenReturn(i.toString)
          val ri = new ProcessRecordsInput()
            .withCheckpointer(checkpointer)
            .withMillisBehindLatest(1L)
            .withRecords(List(record).asJava)
          recordProcessor.processRecords(ri)
        }
        //Immediately publish end of shard event
        recordProcessor.shutdown(
          new ShutdownInput()
            .withCheckpointer(checkpointer)
            .withShutdownReason(ShutdownReason.TERMINATE)
        )
      }
    ).parMapN { case (msgs, _) => msgs }.unsafeRunSync()

    res should have size 5
  }

  "KinesisWorker checkpoint pipe" should "checkpoint batch of records with same sequence number" in new KinesisWorkerCheckpointContext {
    val inFlightRecordsPhaser = new Phaser(1)
    val input: immutable.IndexedSeq[CommittableRecord] = (1 to 3) map { i =>
      val record = mock(classOf[RecordAdapter])
      when(record.getSequenceNumber).thenReturn(i.toString)
      new CommittableRecord(
        "shard-1",
        mock(classOf[ExtendedSequenceNumber]),
        1L,
        record,
        recordProcessor,
        checkpointerShard1,
        inFlightRecordsPhaser
      )
    }

    startStream(input)

    eventually(timeout(1.second)) {
      verify(checkpointerShard1).checkpoint(input.last.record)
    }
  }

  it should "checkpoint batch of records of different shards" in new KinesisWorkerCheckpointContext {
    val checkpointerShard2: IRecordProcessorCheckpointer =
      mock(classOf[IRecordProcessorCheckpointer])

    val inFlightRecordsPhaser = new Phaser(1)
    val input: immutable.IndexedSeq[CommittableRecord] = (1 to 6) map { i =>
      if (i <= 3) {
        val record = mock(classOf[RecordAdapter])
        when(record.getSequenceNumber).thenReturn(i.toString)
        new CommittableRecord(
          "shard-1",
          mock(classOf[ExtendedSequenceNumber]),
          i,
          record,
          recordProcessor,
          checkpointerShard1,
          inFlightRecordsPhaser
        )
      } else {
        val record = mock(classOf[RecordAdapter])
        when(record.getSequenceNumber).thenReturn(i.toString)
        new CommittableRecord(
          "shard-2",
          mock(classOf[ExtendedSequenceNumber]),
          i,
          record,
          recordProcessor,
          checkpointerShard2,
          inFlightRecordsPhaser
        )
      }
    }

    startStream(input)

    eventually(timeout(3.seconds)) {
      verify(checkpointerShard1).checkpoint(input(2).record)
      verify(checkpointerShard2).checkpoint(input.last.record)
    }

  }

  it should "not checkpoint the batch if the IRecordProcessor has been shutdown with ZOMBIE reason" in new KinesisWorkerCheckpointContext {
    recordProcessor.shutdown(
      new ShutdownInput()
        .withShutdownReason(ShutdownReason.ZOMBIE)
        .withCheckpointer(checkpointerShard1)
    )

    val input: immutable.IndexedSeq[CommittableRecord] = (1 to 3) map { i =>
      val record = mock(classOf[RecordAdapter])
      when(record.getSequenceNumber).thenReturn("1")
      new CommittableRecord(
        "shard-1",
        mock(classOf[ExtendedSequenceNumber]),
        1L,
        record,
        recordProcessor,
        checkpointerShard1,
        recordProcessor.inFlightRecordsPhaser
      )
    }

    startStream(input)

    verify(checkpointerShard1, times(0)).checkpoint()
  }

  it should "fail with Exception if checkpoint action fails" in new KinesisWorkerCheckpointContext {
    val checkpointer: IRecordProcessorCheckpointer = mock(classOf[IRecordProcessorCheckpointer])
    val inFlightRecordsPhaser                      = new Phaser(1)
    val record: RecordAdapter                      = mock(classOf[RecordAdapter])
    when(record.getSequenceNumber).thenReturn("1")

    val input = new CommittableRecord(
      "shard-1",
      mock(classOf[ExtendedSequenceNumber]),
      1L,
      record,
      recordProcessor,
      checkpointer,
      inFlightRecordsPhaser
    )

    val failure = new RuntimeException("you have no power here")
    when(checkpointer.checkpoint(record)).thenThrow(failure)

    the[RuntimeException] thrownBy fs2.Stream
      .emits(Seq(input))
      .through(checkpointRecords[IO](settings))
      .compile
      .toVector
      .unsafeRunSync() should have message "you have no power here"

    eventually(verify(checkpointer).checkpoint(input.record))
  }

  abstract private class WorkerContext(errorStream: Boolean = false) {

    val semaphore = new Semaphore(1)
    semaphore.acquire()
    var output = List.newBuilder[CommittableRecord]

    protected val mockWorker: Worker = mock(classOf[Worker])

    val latch = new CountDownLatch(1)
    doAnswer(_ => latch.await()).when(mockWorker).run()

    when(mockWorker.shutdown()).thenAnswer((invocation: InvocationOnMock) => ())

    var recordProcessorFactory: IRecordProcessorFactory = _
    var recordProcessor: IRecordProcessor               = _
    var recordProcessor2: IRecordProcessor              = _

    val builder: IRecordProcessorFactory => IO[Worker] = { x: IRecordProcessorFactory =>
      recordProcessorFactory = x
      recordProcessor = x.createProcessor()
      semaphore.release()
      recordProcessor2 = x.createProcessor()
      semaphore.release()
      IO(mockWorker)
    }

    val config: KinesisStreamSettings =
      KinesisStreamSettings(bufferSize = 10, 10.seconds)
        .getOrElse(throw new RuntimeException("cannot create Kinesis Settings"))

    val stream: fs2.Stream[IO, CommittableRecord] =
      readFromDynamoDBStream[IO](builder, config)
        .map(i => if (errorStream) throw new Exception("boom") else i)
  }

  private trait TestData {
    @volatile var endOfShardSeen = false

    protected val checkpointer: IRecordProcessorCheckpointer = mock(
      classOf[IRecordProcessorCheckpointer]
    )

    doAnswer { _ =>
      endOfShardSeen = true
      null
    }.when(checkpointer).checkpoint()

    doAnswer { _ =>
      if (endOfShardSeen) throw new Exception("Checkpointing after End Of Shard")
      null
    }.when(checkpointer).checkpoint(any[Record])

    val initializationInput: InitializationInput = {
      new InitializationInput()
        .withShardId("shardId")
        .withExtendedSequenceNumber(ExtendedSequenceNumber.AT_TIMESTAMP)
    }

    val record: Record =
      new RecordAdapter(
        new model.Record()
          .withDynamodb(new StreamRecord().addNewImageEntry("name", new AttributeValue("Barry")))
      ).withApproximateArrivalTimestamp(new Date())
        .withEncryptionType("encryption")

    val recordsInput: ProcessRecordsInput =
      new ProcessRecordsInput()
        .withCheckpointer(checkpointer)
        .withMillisBehindLatest(1L)
        .withRecords(List(record).asJava)
  }

  private trait KinesisWorkerCheckpointContext {
    val recordProcessor = new RecordProcessor(_ => ())
    val checkpointerShard1: IRecordProcessorCheckpointer = mock(
      classOf[IRecordProcessorCheckpointer]
    )
    val settings: KinesisCheckpointSettings =
      KinesisCheckpointSettings(maxBatchSize = 100, maxBatchWait = 500.millis)
        .getOrElse(throw new RuntimeException("Cannot create Kinesis Checkpoint Settings"))

    def startStream(input: Seq[CommittableRecord]): Unit =
      fs2.Stream
        .emits(input)
        .through(checkpointRecords[IO](settings))
        .compile
        .toVector
        .unsafeRunAsync(_ => ())
  }
}
