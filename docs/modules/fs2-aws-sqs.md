# fs2-aws-sqs

An FS2 Streams-based API for consuming and publishing AWS SQS messages.

@:include(_disclaimer.md)

### Import
```sbt
libraryDependencies += "io.laserdisc" %% "fs2-aws-sqs" % "@VERSION@"
```

This module provides the `SQS[F]` algebra:

```scala
trait SQS[F[_]] {
    def sqsStream: Stream[F, Message]
    def sqsStreamBatched: Stream[F, Chunk[Message]]
    def changeMessageVisibilityPipe(timeout: FiniteDuration): Pipe[F, Message, Message]
    def deleteMessagePipe: Pipe[F, Message, DeleteMessageResponse]
    def sendMessagePipe: Pipe[F, SQS.MsgBody, SendMessageResponse]
}
```

### Usage

To use `SQS[F]`, you need an instance of `SqsAsyncClientOp[F]`:
* This is a Tagless-Final wrapper around the `SqsAsyncClient`
* You get this automatically as `fs2-aws-sqs` has a transitive dependency on `pure-sqs-tagless` (see [pure-aws](pure-aws.md)).

The general usage pattern is as follows:

```scala
// create the tagless-final wrapper resource (pass an SqsAsyncClient.builder()
// if you need to configure credentials, region, etc.)
val sqsInterpreter = SqsInterpreter[IO].resource

// use the interpreter directly for effectful AWS SDK calls
sqsInterpreter.use { sqsOp =>
  SQS.create[IO](config, sqsOp).flatMap { sqs =>
    sqs.sqsStream
    .. etc ..
  }
}
```

### Full Example

```scala mdoc:compile-only
import cats.effect.*
import fs2.aws.sqs.{SQS, SqsConfig}
import io.laserdisc.pure.sqs.tagless.SqsInterpreter
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse
import scala.concurrent.duration.*

val config = SqsConfig(
  queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789012/my-queue",
  pollRate = 3.seconds,        // default
  fetchMessageCount = 10       // default; must be 1 to 10
)

object SQSExample {

  // use the tagless-final wrapper directly for effectful AWS SDK calls
  def basicExample: IO[ListQueuesResponse] =
    SqsInterpreter[IO].resource.use { client =>
      client.listQueues
    }

  // or make use of the streaming API for consuming and publishing messages
  def fs2StreamingExample: IO[Unit] =
    SqsInterpreter[IO].resource.use { sqsOp =>
      SQS.create[IO](config, sqsOp).flatMap { sqs =>
        for {
          // publish
          _ <- fs2.Stream("hello", "world")
            .through(sqs.sendMessagePipe)
            .compile
            .drain

          // consume
          _ <- sqs.sqsStream
            .evalTap(msg => IO.println(s"received: ${msg.body()}"))
            .through(sqs.deleteMessagePipe) // acknowledge by deleting
            .compile
            .drain
        } yield ()
      }
    }
}

```

### Long polling

`SQS.createWithReceiveRequest` applies a function to every `ReceiveMessageRequest`. With long polling, each receive waits up to 20 seconds for messages, so there's no need to pause between receives:

```scala mdoc:compile-only
import cats.effect.*
import fs2.aws.sqs.{SQS, SqsConfig}
import io.laserdisc.pure.sqs.tagless.SqsInterpreter
import scala.concurrent.duration.*

val longPolling: IO[Unit] =
  SqsInterpreter[IO].resource.use { sqsOp =>
    SQS
      .createWithReceiveRequest[IO](
        SqsConfig("https://sqs.us-east-1.amazonaws.com/123456789012/my-queue", pollRate = Duration.Zero),
        sqsOp,
        _.waitTimeSeconds(20)
      )
      .flatMap { sqs =>
        sqs.sqsStream
          .evalTap(msg => IO.println(msg.body()))
          .through(sqs.deleteMessagePipe)
          .compile
          .drain
      }
  }
```

### Notes

- `sqsStream` polls the queue at the configured `pollRate` (default 3 seconds) and emits raw SDK `Message`s, fetching up to `fetchMessageCount` messages per poll (default 10; must be 1 to 10). Only use `pollRate = Duration.Zero` with long polling (per request, or the queue's `ReceiveMessageWaitTimeSeconds`); without it, the stream busy-loops on empty receives.
- Besides long polling, the `createWithReceiveRequest` function can set `_.visibilityTimeout(...)` per receive, or `_.messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)` to spot redeliveries. The queue URL and message count always come from `SqsConfig`.
- A receive hides up to `fetchMessageCount` messages at once, but `sqsStream` hands them over one at a time. If handling can outlast the queue's visibility timeout, use `fetchMessageCount = 1`, or consume `sqsStreamBatched` (one chunk per receive) and extend or delete each batch as a whole.
- Use `deleteMessagePipe` to acknowledge messages by deleting them from the queue.
- `changeMessageVisibilityPipe(timeout)` sets each message's timeout once, rounded up to whole seconds. It isn't a heartbeat: to keep a message hidden during long processing, call it again before the timeout runs out.
