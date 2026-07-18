# fs2-aws-sqs

Consume and publish AWS SQS messages as fs2 streams and pipes.

```sbt
libraryDependencies += "io.laserdisc" %% "fs2-aws-sqs" % "@VERSION@"
```

The module exposes a single algebra:

```scala
trait SQS[F[_]] {
  def sqsStream: Stream[F, Message]
  def changeMessageVisibilityPipe(timeout: FiniteDuration): Pipe[F, Message, Message]
  def deleteMessagePipe: Pipe[F, Message, DeleteMessageResponse]
  def sendMessagePipe: Pipe[F, SQS.MsgBody, SendMessageResponse]
}
```

`sqsStream` polls the queue at the configured `pollRate` and emits raw SDK `Message`s.

## Usage

Create an `SQS[F]` from an `SqsConfig` and an `SqsAsyncClientOp` (provided by
[`pure-sqs-tagless`](pure-aws.md), a dependency of this module):

```scala
import cats.effect.*
import fs2.aws.sqs.{SQS, SqsConfig}
import io.laserdisc.pure.sqs.tagless.{Interpreter as SqsInterpreter, SqsAsyncClientOp}
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import scala.concurrent.duration.*

val sqsResource: Resource[IO, SqsAsyncClientOp[IO]] =
  SqsInterpreter[IO].SqsAsyncClientOpResource(SqsAsyncClient.builder()) // configure credentials/region/endpoint as needed

val config = SqsConfig(
  queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789012/my-queue",
  pollRate = 3.seconds,        // default
  fetchMessageCount = 10       // default; must be 1 to 10
)

sqsResource.use { sqsOp =>
  SQS.create[IO](config, sqsOp).flatMap { sqs =>
    sqs.sqsStream
      .evalTap(msg => IO.println(s"received: ${msg.body()}"))
      .through(sqs.deleteMessagePipe) // acknowledge by deleting
      .compile
      .drain
  }
}
```

Publishing is a pipe from message bodies:

```scala
fs2.Stream("hello", "world")
  .through(sqs.sendMessagePipe)
```

Use `changeMessageVisibilityPipe(timeout)` to extend the visibility timeout of in-flight
messages while you process them.

