package fs2
package aws

import java.nio.ByteBuffer

import cats.effect.Effect
import com.amazonaws.services.kinesis.producer.UserRecordResult
import com.google.common.util.concurrent.ListenableFuture
import fs2.aws.kinesis.Producer
import fs2.aws.utils.KinesisStub

case class TestKinesisProducerClient[F[_]](respondWith: UserRecordResult,
                                           ops: F[ListenableFuture[UserRecordResult]])
    extends Producer[F] {
  override def putData(streamName: String, partitionKey: String, data: ByteBuffer)(
      implicit e: Effect[F]): F[ListenableFuture[UserRecordResult]] = {
    KinesisStub.save(data)
    ops
  }
}
