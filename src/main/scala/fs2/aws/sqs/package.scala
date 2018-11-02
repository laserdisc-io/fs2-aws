package fs2

import cats.effect.{ConcurrentEffect, IO}
import fs2.aws.sqs.{ReceiverCallback, SQSConsumerBuilder, SqsConfig}
import fs2.concurrent.Queue
import javax.jms.Message

package object aws {

  def sqsStream[F[_], O](sqsConfig: SqsConfig)(
      implicit F: ConcurrentEffect[F],
      decoder: Message => Either[Throwable, O]): fs2.Stream[F, O] = {
    for {
      q        <- fs2.Stream.eval(Queue.unbounded[F, Either[Throwable, O]])
      callback <- fs2.Stream.eval(callback(q))
      item <- SQSConsumerBuilder[F](sqsConfig, callback)
        .serve(q.dequeue.rethrow)
    } yield item
  }

  def callback[F[_], O](queue: Queue[F, Either[Throwable, O]])(
      implicit F: ConcurrentEffect[F],
      decoder: Message => Either[Throwable, O]): F[ReceiverCallback[Either[Throwable, O]]] = {
    F.delay(new ReceiverCallback[Either[Throwable, O]](r =>
      F.runAsync(queue.enqueue1(r))(_ => IO.unit).unsafeRunSync()))
  }

}
