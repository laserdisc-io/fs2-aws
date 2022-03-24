package fs2.aws

import cats.effect.{ Concurrent, Ref }
import cats.implicits.*
import cats.effect.std.Queue
import fs2.{ Pipe, Stream }

package object core {

  /** Helper flow to group elements of a stream into K substreams.
    * Grows with the number of distinct 'K' selectors
    *
    * Start with an empty Map of keys to queues
    * On element received, invoke the selector function to yield the key denoting which queue this element belongs to
    * If we already have an existing queue for that respective key, append the element lifted in an Option to the queue
    * If a queue for that key does not exist, create a new queue, append it to the queue mapping, and then enqueue the element lifted in an Option
    * For each queue, drain the queue yielding a stream of elements
    * After the stream has been emptied, enqueue a single None to the queue so that the stream halts
    *
    *  @tparam F effect type of the fs2 stream
    *  @param selector partitioning function based on the element
    *  @return a FS2 pipe producing a new sub-stream of elements grouped by the selector
    */
  def groupBy[F[_], A, K](
    selector: A => F[K]
  )(implicit F: Concurrent[F]): Pipe[F, A, (K, Stream[F, A])] = { in =>
    Stream.eval(Ref.of[F, Map[K, Queue[F, Option[A]]]](Map.empty)).flatMap { queueMap =>
      val cleanup = {
        queueMap.get.flatMap(_.values.toList.traverse_(_.offer(None)))
      }

      (in ++ Stream.exec(cleanup))
        .evalMap { elem =>
          (selector(elem), queueMap.get).mapN { (key, queues) =>
            queues
              .get(key)
              .fold {
                for {
                  newQ <- Queue.unbounded[F, Option[A]] // Create a new queue
                  _    <- queueMap.modify(queues => (queues + (key -> newQ), queues))
                  // Enqueue the element lifted into an Option to the new queue
                  _ <- newQ.offer(elem.some)
                } yield (key -> Stream.fromQueueNoneTerminated(newQ, 100)).some
              }(_.offer(elem.some).as(None))
          }.flatten
        }
        .unNone
        .onFinalize(cleanup)
    }
  }
}
