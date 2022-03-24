package fs2.aws.testkit

import java.nio.ByteBuffer
import cats.effect.{ Ref, Sync }
import com.amazonaws.services.kinesis.producer.{ Attempt, UserRecordResult }
import com.google.common.util.concurrent.{ ListenableFuture, SettableFuture }
import fs2.aws.internal.KinesisProducerClient
import cats.implicits.*
import io.circe.Decoder
import io.circe.jawn.CirceSupportParser

import scala.jdk.CollectionConverters.*

case class TestKinesisProducerClient[F[_], T](state: Ref[F, List[T]])(
  implicit decoder: Decoder[T]
) extends KinesisProducerClient[F] {
  override def putData(
    streamName: String,
    partitionKey: String,
    data: ByteBuffer
  )(implicit F: Sync[F]): F[ListenableFuture[UserRecordResult]] =
    for {
      t <- CirceSupportParser
            .parseFromByteBuffer(data)
            .toEither
            .flatMap(_.as[T])
            .liftTo[F]
      _ <- state.modify(orig => (t :: orig, orig))
      res = {
        val future: SettableFuture[UserRecordResult] = SettableFuture.create()
        future.set(new UserRecordResult(List[Attempt]().asJava, "seq #", "shard #", true))
        future
      }

    } yield res
}
