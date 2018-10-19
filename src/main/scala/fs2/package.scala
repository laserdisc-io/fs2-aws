package fs2

import fs2.concurrent.Queue
import cats.effect.Effect
import cats.effect.concurrent.Ref

package object internal {
  // Grows with the number of distinct `K`
  def partitions[F[_] <: Effect[F], A, K](selector: A => F[K])(implicit F: Effect[F]): Pipe[F, A, (K, Stream[F, A])] = in =>
  Stream.eval(Ref.of[F, Map[K, Queue[F, Option[A]]]](Map.empty)).flatMap { st =>
    val cleanup = {
      import alleycats.std.all._
      st.get.flatMap(_.traverse_(_.enqueue1(None)))
    }

    (in ++ Stream.eval_(cleanup)).evalMap { el =>
      (selector(el), st.get).mapN { (key, queues) =>
        queues.get(key).fold(
          for {
            newQ <- Queue.unbounded[F, Option[A]]
            _ <- st.modify(_ + (key -> newQ))
            _ <- newQ.enqueue1(el.some)
          } yield (key -> newQ.dequeue.unNoneTerminate).some
        )(_.enqueue1(el.some) as None)
      }.flatten
    }.unNone.onFinalize(cleanup)
  }
}
