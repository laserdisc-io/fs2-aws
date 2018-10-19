package fs2
package aws

import cats.effect.{IO, ContextShift}
import org.scalatest.{FlatSpec, Matchers, BeforeAndAfterEach}
import org.scalatest.concurrent.Eventually
import org.mockito.Mockito._
import kinesis.kcl._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessorFactory, IRecordProcessor}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker
import com.amazonaws.services.kinesis.clientlibrary.types._
import com.amazonaws.services.kinesis.model.Record

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import java.util.Date
import java.nio.ByteBuffer
import scala.collection.JavaConverters._

class KinesisConsumerSpec extends FlatSpec with Matchers with BeforeAndAfterEach with Eventually {

  "Reading data from a Kinesis stream via readFromStream" should "successfully read data from the Kinesis stream" in new WorkerContext with TestData {
    recordProcessor.initialize(initializationInput)
    recordProcessor.processRecords(recordsInput)

    eventually(timeout(1.second)) {
      output.head should be(record)
    }
  }

  private abstract class WorkerContext(backpressureTimeout: FiniteDuration = 1.minute) {
    implicit val ec: ExecutionContext = ExecutionContext.global
    implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)

    var output: List[Record] = List()

    protected val mockWorker = mock(classOf[Worker])

    when(mockWorker.run()).thenAnswer(
      new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit = ()
      })

    var recordProcessorFactory: IRecordProcessorFactory = _
    var recordProcessor: IRecordProcessor = _

    val builder = { x: IRecordProcessorFactory =>
      recordProcessorFactory = x
      recordProcessor = x.createProcessor()
      mockWorker
    }

    val stream =
      readFromStream[IO](builder)
        .through(_.evalMap(i => IO(output = output :+ i)))
        .compile
        .toVector
        .unsafeRunAsync(_ => ())
  }

  private trait TestData {
    protected val checkpointer = mock(classOf[IRecordProcessorCheckpointer])

    val initializationInput = {
      new InitializationInput()
        .withShardId("shardId")
        .withExtendedSequenceNumber(ExtendedSequenceNumber.AT_TIMESTAMP)
    }
    val record =
      new Record()
        .withApproximateArrivalTimestamp(new Date())
        .withEncryptionType("encryption")
        .withPartitionKey("partitionKey")
        .withSequenceNumber("sequenceNum")
        .withData(ByteBuffer.wrap("test".getBytes))
    val recordsInput =
      new ProcessRecordsInput()
        .withCheckpointer(checkpointer)
        .withMillisBehindLatest(1L)
        .withRecords(List(record).asJava)
  }
}
