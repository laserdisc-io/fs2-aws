package fs2

import cats.effect.concurrent.Deferred
import cats.effect.{ ConcurrentEffect, IO }
import fs2.aws.sqs.{ ConsumerBuilder, ReceiverCallback, SqsConfig }
import fs2.concurrent.Queue
import javax.jms.{ Message, MessageListener }

package object aws {

  def sqsStream[F[_], O](
    sqsConfig: SqsConfig,
    builder: (SqsConfig, MessageListener) => ConsumerBuilder[F],
    d: Option[Deferred[F, MessageListener]] = None
  )(implicit F: ConcurrentEffect[F], decoder: Message => Either[Throwable, O]): fs2.Stream[F, O] =
    for {
      q        <- fs2.Stream.eval(Queue.unbounded[F, Either[Throwable, O]])
      callback <- fs2.Stream.eval(callback(q))
      _        <- fs2.Stream.eval(d.map(_.complete(callback)).getOrElse(F.unit))
      item     <- builder(sqsConfig, callback).serve(q.dequeue.rethrow)
    } yield item

  def callback[F[_], O](
    queue: Queue[F, Either[Throwable, O]]
  )(
    implicit F: ConcurrentEffect[F],
    decoder: Message => Either[Throwable, O]
  ): F[ReceiverCallback[Either[Throwable, O]]] =
    F.delay(
      new ReceiverCallback[Either[Throwable, O]](r =>
        F.runAsync(queue.enqueue1(r))(_ => IO.unit).unsafeRunSync()
      )
    )

}
