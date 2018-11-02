package fs2

import cats.effect.{ConcurrentEffect, ContextShift, IO}
import cats.syntax.either._
import eu.timepit.refined.types.string.TrimmedString
import fs2.aws.sqs.SQSConsumerBuilder
import fs2.concurrent.Queue
import javax.jms.{Message, MessageListener}

import scala.util.control.Exception

package object aws {

  case class SqsConfig(queueName: TrimmedString)

  def sqsStream[F[_], O](sqsConfig: SqsConfig)(
      implicit F: ConcurrentEffect[F],
      cs: ContextShift[F],
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
      cs: ContextShift[F],
      decoder: Message => Either[Throwable, O]): F[ReceiverCallback[Either[Throwable, O]]] = {
    F.delay(new ReceiverCallback[Either[Throwable, O]](r =>
      F.runAsync(queue.enqueue1(r))(_ => IO.unit).unsafeRunSync()))
  }

  class ReceiverCallback[A](f: A => Unit)(implicit messageParser: Message => A)
      extends MessageListener {
    def onMessage(message: Message): Unit = {
      Exception.nonFatalCatch either {
        messageParser.andThen(f)(message)
        message.acknowledge()
      } leftMap (e => System.err.println("Error processing message: " + e.getMessage))
    }

  }

}
