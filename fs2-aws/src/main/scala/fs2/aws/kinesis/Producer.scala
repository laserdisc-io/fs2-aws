package fs2.aws.kinesis

import java.nio.ByteBuffer

import cats.effect.Effect
import com.amazonaws.services.kinesis.producer.UserRecordResult
import com.google.common.util.concurrent.ListenableFuture

trait Producer[F[_]] {
  def putData(streamName: String, partitionKey: String, data: ByteBuffer)(
      implicit F: Effect[F]): F[ListenableFuture[UserRecordResult]]
}
