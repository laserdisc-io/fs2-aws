package fs2.aws.dynamodb

import cats.effect.unsafe.IORuntime
import cats.effect.{IO, Resource}
import cats.implicits.*
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, StreamRecord}
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBStreams, model}
import com.amazonaws.services.kinesis.clientlibrary.config.KinesisClientLibConfigurator
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{ShutdownReason, Worker}
import com.amazonaws.services.kinesis.clientlibrary.types.{
  ExtendedSequenceNumber,
  InitializationInput,
  ProcessRecordsInput,
  ShutdownInput
}
import com.amazonaws.services.kinesis.model.Record
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.*
import org.slf4j.LoggerFactory

import java.io.InputStream
import java.util.Date
import java.util.concurrent.{CountDownLatch, Phaser}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

class NewDynamoDBConsumerSpec
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
    PatienceConfig(timeout = scaled(Span(20, Seconds)), interval = scaled(Span(5, Millis)))

  "DynamoDB source" should "successfully read data from the DynamoDB stream" in new WorkerContext with TestData {
    val res = streamResource
      .use(stream =>
        (
          stream.take(1).compile.toList,
          IO.blocking {
            shard1Guard.await()
            recordProcessor.initialize(initializationInput)
            recordProcessor.processRecords(recordsInput)
          }
        ).parMapN((l, _) => l)
      )
      .unsafeToFuture()
      .futureValue

    val commitableRecord: CommittableRecord = res.head
    commitableRecord.record.getData should be(record.getData)
    commitableRecord.recordProcessorStartingSequenceNumber shouldBe initializationInput.getExtendedSequenceNumber
    commitableRecord.shardId shouldBe initializationInput.getShardId
    commitableRecord.millisBehindLatest shouldBe recordsInput.getMillisBehindLatest
  }

  it should "Shutdown the worker if the stream is drained and has not failed" in new WorkerContext with TestData {
    (
      streamResource.use(stream =>
        stream
          .take(1)
          .compile
          .toList
          .flatMap(l => IO.blocking(latch.countDown()) >> IO.pure(l))
      ),
      IO.blocking {
        shard1Guard.await()
        recordProcessor.initialize(initializationInput)
        recordProcessor.processRecords(recordsInput)
      }
    ).parMapN { case (_, _) => () }.unsafeToFuture().futureValue

    verify(mockScheduler, times(1)).shutdown()
  }

  it should "Shutdown the worker if the stream terminates" in new WorkerContext(errorStream = true) with TestData {
    intercept[Exception] {
      (
        streamResource.use(stream =>
          stream
            .take(1)
            .compile
            .toList
            .flatMap(l => IO.blocking(latch.countDown()) >> IO.pure(l))
        ),
        IO.blocking {
          shard1Guard.await()
          recordProcessor.initialize(initializationInput)
          recordProcessor.processRecords(recordsInput)
        }
      ).parMapN { case (_, _) => () }.unsafeToFuture().futureValue
    }

    verify(mockScheduler, times(1)).shutdown()
  }

  it should "not drop messages in case of back-pressure" in new WorkerContext with TestData {

    // Create and send 10 records (to match buffer size)
    val res =
      streamResource
        .use { stream =>
          (
            stream.take(60).compile.toList.flatMap(l => IO.blocking(latch.countDown()) >> IO.pure(l)),
            IO.blocking {
              shard1Guard.await()
              recordProcessor.initialize(initializationInput)
              for (i <- 1 to 10) {
                val record: Record = mock(classOf[RecordAdapter])
                when(record.getSequenceNumber).thenReturn(i.toString)
                recordProcessor.processRecords(recordsInput.withRecords(List(record).asJava))
              }
            },
            // Send a batch that exceeds the internal buffer size
            IO.blocking {
              shard1Guard.await()
              recordProcessor.initialize(initializationInput)
              for (i <- 1 to 50) {
                val record: Record = mock(classOf[RecordAdapter])
                when(record.getSequenceNumber).thenReturn(i.toString)
                recordProcessor.processRecords(recordsInput.withRecords(List(record).asJava))
              }
            }
          ).parMapN { case (msgs, _, _) => msgs }
        }
        .unsafeToFuture()
        .futureValue

    res should have size 60
  }

  it should "not drop messages in case of back-pressure with multiple shard workers" in new WorkerContext
    with TestData {

    val res =
      streamResource
        .use { stream =>
          (
            stream.take(10).compile.toList,
            IO.blocking {
              logger.info("about to acquire lock for record processor #1")
              shard1Guard.await()
              logger.info("acquired lock for record processor #1")
              recordProcessor.initialize(initializationInput)
              for (i <- 1 to 5) {
                val record: Record = mock(classOf[RecordAdapter])
                when(record.getSequenceNumber).thenReturn(i.toString)
                recordProcessor.processRecords(recordsInput.withRecords(List(record).asJava))
              }
            },
            IO.blocking {
              logger.info("about to acquire lock for record processor #2")
              shard2Guard.await()
              logger.info("acquired lock for record processor #2")
              recordProcessor2.initialize(
                new InitializationInput()
                  .withShardId("shard2")
                  .withExtendedSequenceNumber(ExtendedSequenceNumber.AT_TIMESTAMP)
              )
              // Create and send 10 records (to match buffer size)
              for (i <- 1 to 5) {
                val record: Record = mock(classOf[RecordAdapter])
                when(record.getSequenceNumber).thenReturn(i.toString)
                recordProcessor2.processRecords(recordsInput.withRecords(List(record).asJava))
              }
            }
          ).parMapN { case (msgs, _, _) => msgs }

        }
        .unsafeToFuture()
        .futureValue

    // Should process all 10 messages
    res should have size 10
  }

  it should "delay the end of shard checkpoint until all messages are drained" in new WorkerContext with TestData {
    val nRecords = 5L

    val res: Seq[Record] = (
      streamResource.use(stream =>
        stream
          .take(nRecords)
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
        (1L to nRecords).foreach { i =>
          val record: Record = mock(classOf[RecordAdapter])
          when(record.getSequenceNumber).thenReturn(i.toString)
          val ri = new ProcessRecordsInput()
            .withCheckpointer(checkpointer)
            .withMillisBehindLatest(1L)
            .withRecords(List(record).asJava)
          recordProcessor.processRecords(ri)
        }
      } >> IO.delay {
        // Immediately publish end of shard event
        recordProcessor.shutdown(
          new ShutdownInput()
            .withCheckpointer(checkpointer)
            .withShutdownReason(ShutdownReason.TERMINATE)
        )
      }
    ).parMapN { case (msgs, _) => msgs }.unsafeToFuture().futureValue

    res should have size 5
  }

  "KinesisWorker checkpoint pipe" should "checkpoint batch of records with same sequence number" in new WorkerContext() {
    val inFlightRecordsPhaser = new Phaser(1)

    val input = (1 to 3).map { i =>
      val record = mock(classOf[RecordAdapter])
      when(record.getSequenceNumber).thenReturn(i.toString)
      new CommittableRecord(
        "shard-1",
        mock(classOf[ExtendedSequenceNumber]),
        1L,
        record,
        new RecordProcessor(_ => ()),
        checkpointerShard1,
        inFlightRecordsPhaser
      )
    }

    startStream(input)

    eventually(timeout(1.second)) {
      verify(checkpointerShard1).checkpoint(input.last.record)
    }
  }

  it should "checkpoint batch of records of different shards" in new WorkerContext() {
    val checkpointerShard2    = mock(classOf[IRecordProcessorCheckpointer])
    val inFlightRecordsPhaser = new Phaser(1)

    val input = (1L to 6L).map { i =>
      if (i <= 3) {
        val record = mock(classOf[RecordAdapter])
        when(record.getSequenceNumber).thenReturn(i.toString)
        new CommittableRecord(
          "shard-1",
          mock(classOf[ExtendedSequenceNumber]),
          i,
          record,
          new RecordProcessor(_ => ()),
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
          new RecordProcessor(_ => ()),
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

  it should "not checkpoint the batch if the IRecordProcessor has been shutdown with ZOMBIE reason" in new WorkerContext() {

    val cS: IRecordProcessorCheckpointer = mock(
      classOf[IRecordProcessorCheckpointer]
    )

    val rp = new RecordProcessor(_ => ())
    rp.shutdown(
      new ShutdownInput()
        .withShutdownReason(ShutdownReason.ZOMBIE)
        .withCheckpointer(checkpointerShard1)
    )

    val input = (1 to 3).map { i =>
      val record = mock(classOf[RecordAdapter])
      when(record.getSequenceNumber).thenReturn("1")
      new CommittableRecord(
        "shard-1",
        mock(classOf[ExtendedSequenceNumber]),
        1L,
        record,
        rp,
        cS,
        rp.inFlightRecordsPhaser
      )
    }

    startStream(input)

    verify(checkpointerShard1, never()).checkpoint()
  }

  it should "fail with Exception if checkpoint action fails" in new WorkerContext() {
    val checkpointer          = mock(classOf[IRecordProcessorCheckpointer])
    val inFlightRecordsPhaser = new Phaser(1)
    val record                = mock(classOf[RecordAdapter])
    val rp                    = new RecordProcessor(_ => ())
    when(record.getSequenceNumber).thenReturn("1")

    val input = new CommittableRecord(
      "shard-1",
      mock(classOf[ExtendedSequenceNumber]),
      1L,
      record,
      rp,
      checkpointer,
      inFlightRecordsPhaser
    )

    val failure = new RuntimeException("you have no power here")
    when(checkpointer.checkpoint(record)).thenThrow(failure)

    (the[RuntimeException] thrownBy fs2.Stream
      .emits(Seq(input))
      .through(k.checkpointRecords(settings))
      .compile
      .toVector
      .unsafeRunSync() should have).message("you have no power here")

    eventually(verify(checkpointer).checkpoint(input.record))
  }

  it should "bypass all items when checkpoint" in new WorkerContext() {
    val checkpointer          = mock(classOf[IRecordProcessorCheckpointer])
    val inFlightRecordsPhaser = new Phaser(1)
    val rp                    = new RecordProcessor(_ => ())

    val record = mock(classOf[RecordAdapter])
    when(record.getSequenceNumber).thenReturn("1")

    val input = (1L to 100L).map(idx =>
      new CommittableRecord(
        s"shard-1",
        mock(classOf[ExtendedSequenceNumber]),
        idx,
        record,
        rp,
        checkpointer,
        inFlightRecordsPhaser
      )
    )

    fs2.Stream
      .emits(input)
      .through(k.checkpointRecords(settings))
      .compile
      .toVector
      .unsafeToFuture()
      .futureValue should have size 100
  }

  abstract private class WorkerContext(errorStream: Boolean = false) {

    val shard1Guard                     = new CountDownLatch(1)
    val shard2Guard                     = new CountDownLatch(1)
    val latch                           = new CountDownLatch(1)
    protected val mockScheduler: Worker = mock(classOf[Worker])

    var recordProcessorFactory: IRecordProcessorFactory = _
    var recordProcessor: IRecordProcessor               = _
    var recordProcessor2: IRecordProcessor              = _

    doAnswer(_ => latch.await()).when(mockScheduler).run()

    val builder: IRecordProcessorFactory => IO[Worker] = { (x: IRecordProcessorFactory) =>
      recordProcessorFactory = x
      recordProcessor = x.createProcessor()
      shard1Guard.countDown()
      recordProcessor2 = x.createProcessor()
      shard2Guard.countDown()
      mockScheduler.pure[IO]
    }

    val k: DynamoDB[IO] = DynamoDB.create[IO](builder)

    val stream: fs2.Stream[IO, CommittableRecord] =
      k.readFromDynamoDBStream("testStream", "testApp")
        .map(i => if (errorStream) throw new Exception("boom") else i)
        .onFinalize(IO.delay(latch.countDown()))

    private val propertiesInputStream: InputStream = getClass.getResourceAsStream("/kinesis.properties")
    val streamResource: Resource[IO, fs2.Stream[IO, CommittableRecord]] = DefaultDynamoStreamBuilder[IO]()
      .withKinesisClientConf(new KinesisClientLibConfigurator().getConfiguration(propertiesInputStream))
      .withDynamoDBStreams(mock(classOf[AmazonDynamoDBStreams]))
      .withDynamoDBClient(mock(classOf[AmazonDynamoDB]))
      .withCloudWatchClient(mock(classOf[AmazonCloudWatch]))
      .withDefaultRecordProcessor
      .withDefaultBufferSize
      .withScheduler { case (_, _, _, _, factory) =>
        val x = factory
        recordProcessor = x.createProcessor()
        shard1Guard.countDown()
        recordProcessor2 = x.createProcessor()
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

    val settings: KinesisCheckpointSettings =
      KinesisCheckpointSettings(maxBatchSize = Int.MaxValue, maxBatchWait = 500.millis)
        .getOrElse(throw new Error())

    val checkpointerShard1: IRecordProcessorCheckpointer = mock(classOf[IRecordProcessorCheckpointer])

    def startStream(input: Seq[CommittableRecord]): Seq[Record] =
      fs2.Stream
        .emits(input)
        .through(k.checkpointRecords(settings))
        .compile
        .toList
        .unsafeToFuture()
        .futureValue
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
    }.when(checkpointer).checkpoint(any[String], any[Long])

    val initializationInput: InitializationInput =
      new InitializationInput()
        .withShardId("shardId")
        .withExtendedSequenceNumber(ExtendedSequenceNumber.AT_TIMESTAMP)

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

}
