package fs2.aws.dynamodb

import cats.Functor
import cats.effect.std.{Dispatcher, Queue}
import cats.effect.{Async, Deferred, Ref}
import cats.implicits.*
import fs2.{Chunk, Stream}
import io.laserdisc.pure.dynamodb.tagless.DynamoDbAsyncClientOp
import org.reactivestreams.{Subscriber, Subscription}
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, ScanRequest, ScanResponse}

import java.util.Map as JMap
import scala.jdk.CollectionConverters.*
trait StreamScan[F[_]] {

  /** Scans Dynamodb into the FS2 stream
    *
    *  @param scanRequest AWS SDK2 DynamoDB scan request
    *  @param pageSize the maximum number of records requested per scan page; must be positive
    *  @return an fs2 Stream that emits RAW DynamoDB items, terminates, once scan exhausted
    */
  def scanDynamoDB(
      scanRequest: ScanRequest,
      pageSize: Int
  ): Stream[F, Chunk[JMap[String, AttributeValue]]]
}

object StreamScan {
  def apply[F[_]: Functor: Async](ddb: DynamoDbAsyncClientOp[F]): StreamScan[F] =
    new StreamScan[F]() {
      def scanDynamoDB(
          scanRequest: ScanRequest,
          pageSize: Int
      ): Stream[F, Chunk[JMap[String, AttributeValue]]] =
        if (pageSize <= 0) Stream.raiseError[F](new IllegalArgumentException("pageSize must be greater than 0"))
        else for {
          dispatcher <- Stream.resource(Dispatcher.parallel[F])
          queue      <- Stream.eval(Queue.bounded[F, Option[Chunk[JMap[String, AttributeValue]]]](1))
          sub        <- Stream.eval(Ref[F].of[Option[Subscription]](None))
          error      <- Stream.eval(Deferred[F, Throwable])
          _          <- Stream.eval(
            ddb
              .scanPaginator(scanRequest.toBuilder().limit(pageSize).build())
              .map { publisher =>
                // Limit each DynamoDB response to pageSize items and request one response at a time.
                // The bounded FS2 queue provides back pressure while a page is consumed.
                publisher.subscribe(new Subscriber[ScanResponse] {

                  override def onSubscribe(s: Subscription): Unit =
                    dispatcher
                      .unsafeRunSync(
                        sub.set(s.some) >> Async[F].delay(s.request(1))
                      )

                  override def onNext(t: ScanResponse): Unit =
                    dispatcher
                      .unsafeRunSync(
                        for {
                          _ <- queue.offer(Chunk(t.items().asScala.toList*).some)
                          s <- sub.get
                          _ <- Async[F].delay(s.map(_.request(1)))
                        } yield ()
                      )

                  override def onError(t: Throwable): Unit =
                    dispatcher.unsafeRunSync(error.complete(t) >> queue.offer(None))

                  override def onComplete(): Unit =
                    dispatcher.unsafeRunSync(queue.offer(None))
                })
              }
          )
          stream <- (Stream.fromQueueNoneTerminated(queue) ++ Stream.eval(error.tryGet).flatMap {
            case Some(value) => Stream.raiseError[F](value)
            case None        => Stream.empty
          }).onFinalize(sub.get.flatMap(_.traverse_(s => Async[F].delay(s.cancel()))))
        } yield stream

    }
}
