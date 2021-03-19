package fs2.aws.dynamodb

import cats.effect.{ Async, ConcurrentEffect }
import cats.effect.concurrent.Ref
import fs2.{ Chunk, Stream }
import fs2.concurrent.Queue
import io.laserdisc.pure.dynamodb.tagless.DynamoDbAsyncClientOp
import org.reactivestreams.{ Subscriber, Subscription }
import software.amazon.awssdk.services.dynamodb.model.{ AttributeValue, ScanRequest, ScanResponse }
import cats.syntax.functor._
import cats.syntax.option._
import cats.syntax.flatMap._
import scala.jdk.CollectionConverters._

import java.util.{ Map => JMap }
trait StreamScan[F[_]] {

  /** Scans Dynamodb into the FS2 stream
    *
    *  @param scanRequest AWS SDK2 DynamoDB scan request
    *  @param pageSize the page size of the scan, defines how many
    *                  records will be loaded to the memory
    *  @return an fs2 Stream that emits RAW DynamoDB items, terminates, once scan exhausted
    */
  def scanDynamoDB(
    scanRequest: ScanRequest,
    pageSize: Int
  ): Stream[F, Chunk[JMap[String, AttributeValue]]]
}

object StreamScan {
  def apply[F[_]: ConcurrentEffect](ddb: DynamoDbAsyncClientOp[F]): StreamScan[F] =
    new StreamScan[F]() {
      def scanDynamoDB(
        scanRequest: ScanRequest,
        pageSize: Int
      ): Stream[F, Chunk[JMap[String, AttributeValue]]] =
        for {
          queue <- Stream.eval(Queue.bounded[F, Option[Chunk[JMap[String, AttributeValue]]]](1))
          sub   <- Stream.eval(Ref[F].of[Option[Subscription]](None))
          _ <- Stream.eval(
                ddb
                  .scanPaginator(scanRequest)
                  .map(publisher =>
                    //subscribe to the paginator, every time we request to deliver next pageSize items from the DDB table
                    // we use FS2 Queue as bounded buffer with size 1, this way we implement back pressure, not allowing
                    // paginator exhaust memory
                    publisher.subscribe(new Subscriber[ScanResponse] {

                      override def onSubscribe(s: Subscription): Unit =
                        ConcurrentEffect[F]
                          .toIO(sub.set(s.some) >> Async[F].delay(s.request(pageSize)))
                          .unsafeRunSync()

                      override def onNext(t: ScanResponse): Unit =
                        ConcurrentEffect[F]
                          .toIO(
                            for {
                              _ <- queue.enqueue1(Chunk(t.items().asScala.toList: _*).some)
                              s <- sub.get
                              _ <- Async[F].delay(s.map(_.request(pageSize)))
                            } yield ()
                          )
                          .unsafeRunSync()

                      override def onError(t: Throwable): Unit =
                        ConcurrentEffect[F].toIO(Async[F].raiseError(t)).unsafeRunSync()

                      override def onComplete(): Unit =
                        ConcurrentEffect[F].toIO(queue.enqueue1(None)).unsafeRunSync()
                    })
                  )
              )
          stream <- queue.dequeue.unNoneTerminate
        } yield stream

    }
}
