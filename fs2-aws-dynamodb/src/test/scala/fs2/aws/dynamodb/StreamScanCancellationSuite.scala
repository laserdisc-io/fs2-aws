package fs2.aws.dynamodb

import cats.effect.IO
import io.laserdisc.pure.dynamodb.tagless.DynamoDbAsyncClientOp
import munit.CatsEffectSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doAnswer, mock, verify, when}
import org.reactivestreams.{Subscriber, Subscription}
import software.amazon.awssdk.services.dynamodb.model.{ScanRequest, ScanResponse}
import software.amazon.awssdk.services.dynamodb.paginators.ScanPublisher

import java.util.concurrent.atomic.AtomicBoolean

class StreamScanCancellationSuite extends CatsEffectSuite {
  test("invalid page sizes fail through the stream error channel") {
    val ddb = mock(classOf[DynamoDbAsyncClientOp[IO]])
    StreamScan[IO](ddb)
      .scanDynamoDB(ScanRequest.builder().build(), 0)
      .compile
      .drain
      .attempt
      .map(result => assert(result.left.exists(_.isInstanceOf[IllegalArgumentException])))
  }

  test("early termination cancels the publisher subscription") {
    val ddb          = mock(classOf[DynamoDbAsyncClientOp[IO]])
    val publisher    = mock(classOf[ScanPublisher])
    val subscription = mock(classOf[Subscription])
    val emitted      = new AtomicBoolean(false)
    when(ddb.scanPaginator(any(classOf[ScanRequest]))).thenReturn(IO.pure(publisher))
    doAnswer { invocation =>
      val subscriber = invocation.getArgument[Subscriber[ScanResponse]](0)
      doAnswer { _ =>
        if (emitted.compareAndSet(false, true)) {
          val thread = new Thread(() => subscriber.onNext(ScanResponse.builder().build()))
          thread.setDaemon(true)
          thread.start()
        }
        null
      }.when(subscription).request(1L)
      subscriber.onSubscribe(subscription)
      null
    }.when(publisher).subscribe(any(classOf[Subscriber[ScanResponse]]))

    StreamScan[IO](ddb)
      .scanDynamoDB(ScanRequest.builder().build(), 3)
      .take(1)
      .compile
      .drain
      .map(_ => verify(subscription).cancel())
  }
}
