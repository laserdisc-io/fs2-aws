package fs2.aws.testkit

import java.util.concurrent.CountDownLatch

import org.mockito.Mockito.mock
import software.amazon.kinesis.coordinator.Scheduler
import software.amazon.kinesis.processor.{ ShardRecordProcessor, ShardRecordProcessorFactory }

import scala.collection.mutable.ListBuffer

class SchedulerFactoryTestContext(shards: Int) extends (ShardRecordProcessorFactory => Scheduler) {

  val processorsAreReady = new CountDownLatch(1)

  private[this] val mockScheduler: Scheduler = mock(classOf[Scheduler])

  private[this] val shardProcessors = ListBuffer.empty[ShardRecordProcessor]

  override def apply(pf: ShardRecordProcessorFactory): Scheduler = {
    (0 until shards) foreach (_ => shardProcessors += pf.shardRecordProcessor())
    processorsAreReady.countDown()
    mockScheduler
  }

  def getShardProcessors: List[ShardRecordProcessor] = {
    processorsAreReady.await()
    shardProcessors.toList
  }
}
