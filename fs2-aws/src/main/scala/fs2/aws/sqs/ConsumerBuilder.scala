package fs2.aws.sqs

import fs2.Stream
import cats.effect.Effect

trait ConsumerBuilder[F[_]] {
  def start: F[SQSConsumer]

  def serve[A](stream: fs2.Stream[F, A])(implicit F: Effect[F]): fs2.Stream[F, A] = {
    fs2.Stream
      .bracket(start)(con => F.delay(con.shutdown()))
      .flatMap(con => Stream.eval(F.delay(con.startConsumer())).drain ++ stream)
  }
}
