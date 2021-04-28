package fs2
package aws
package kinesis

import cats.effect.{ ConcurrentEffect, ContextShift }
import software.amazon.awssdk.regions.Region
import software.amazon.kinesis.coordinator.Scheduler
import software.amazon.kinesis.processor.ShardRecordProcessorFactory
import eu.timepit.refined.auto._
import scala.annotation.nowarn

package object testkit {
  val TestRecordProcessor = new ChunkedRecordProcessor(_ => ())

  @nowarn
  def readFromKinesisStream[F[_]: ConcurrentEffect: ContextShift](
    schedulerFactory: ShardRecordProcessorFactory => Scheduler
  ): Stream[F, CommittableRecord] =
    kinesis.consumer.readFromKinesisStream[F](
      KinesisConsumerSettings("testStream", "testApp", Region.US_EAST_1, 10),
      schedulerFactory
    )
}
