package fs2
package aws
package kinesis

import cats.effect.{ ConcurrentEffect, ContextShift }
import software.amazon.awssdk.regions.Region
import software.amazon.kinesis.coordinator.Scheduler
import software.amazon.kinesis.processor.ShardRecordProcessorFactory

package object testkit {
  val TestRecordProcessor = new ChunkedRecordProcessor(_ => ())

  def readFromKinesisStream[F[_]: ConcurrentEffect: ContextShift](
    schedulerFactory: ShardRecordProcessorFactory => Scheduler
  ): Stream[F, CommittableRecord] =
    kinesis.consumer.readFromKinesisStream[F](
      KinesisConsumerSettings("testStream", "testApp", Region.US_EAST_1, 10)
        .getOrElse(throw new RuntimeException("Cannot create Consumer Settings")),
      schedulerFactory
    )
}
