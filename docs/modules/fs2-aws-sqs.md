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

### Notes

- `sqsStream` polls the queue at the configured `pollRate` (default 3 seconds) and emits raw SDK `Message`s, fetching up to `fetchMessageCount` messages per poll (default 10; must be 1 to 10).
- Use `deleteMessagePipe` to acknowledge messages by deleting them from the queue.
- Use `changeMessageVisibilityPipe(timeout)` to extend the visibility timeout of in-flight messages while you process them.
