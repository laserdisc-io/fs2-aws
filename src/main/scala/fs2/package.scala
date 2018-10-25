package fs2

import fs2.concurrent.Queue
import cats.implicits._
import cats.effect.Concurrent
import cats.effect.concurrent.Ref

package object internal {

  /** Helper flow to group elements of a stream into K substreams.
    * Grows with the number of distinct 'K' selectors
    *
    *  @tparam F effect type of the fs2 stream
    *  @param selector partitioning function based on the element
    *  @return a FS2 pipe producing a new sub-stream of elements grouped by the selector
    */
  def groupBy[F[_], A, K](selector: A => F[K])(implicit F: Concurrent[F]): Pipe[F, A, (K, Stream[F, A])] = {
    in =>
    Stream.eval(Ref.of[F, Map[K, Queue[F, Option[A]]]](Map.empty)).flatMap { st =>
      val cleanup = {
        import alleycats.std.all._
        st.get.flatMap(_.traverse_(_.enqueue1(None)))
      }

      (in ++ Stream.eval_(cleanup))
        .evalMap { el =>
          (selector(el), st.get).mapN { (key, queues) =>
            queues.get(key).fold {
              for {
                newQ <- Queue.unbounded[F, Option[A]] // Create a new queue
                _ <- st.modify(x => (x + (key -> newQ), x)) // Update the ref of queues
                _ <- newQ.enqueue1(el.some)
              } yield (key -> newQ.dequeue.unNoneTerminate).some
            }(_.enqueue1(el.some) as None)
          }.flatten
        }.unNone.onFinalize(cleanup)
    }
  }
}
