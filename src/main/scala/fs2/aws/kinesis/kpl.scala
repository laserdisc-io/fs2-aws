package fs2
package aws

import com.google.common.util.concurrent.{ListenableFuture, FutureCallback, Futures}
import cats.implicits._
import cats.effect.Effect
import fs2.aws.internal.Internal._

import com.amazonaws.services.kinesis.producer.UserRecordResult


package object kpl {
  def writeData[F[_]](
    streamName: String,
    producer: KinesisProducerClient[F] = new KinesisProducerClient[F] {}
  )(implicit F: Effect[F]): fs2.Sink[F, Byte] = {
    def write: fs2.Pipe[F, List[Byte], ListenableFuture[UserRecordResult]] =
      _.flatMap {
        case byteArray =>
          fs2.Stream.eval(producer.putData(streamName, "partitionKey", byteArray))
      }

    def saveCallback: fs2.Sink[F, ListenableFuture[UserRecordResult]] =
      _.map {
        case f =>
          val callback: FutureCallback[UserRecordResult] = new FutureCallback[UserRecordResult] {
            override def onFailure(t: Throwable): Unit = println(s"write failed: $t")
            override def onSuccess(result: UserRecordResult): Unit = println("write succeeded")
          }

          Futures.addCallback(f, callback)
      }


    _.chunks
      .flatMap(m => fs2.Stream(m.toList))
      .through(write)
      .through(saveCallback)
  }
}
