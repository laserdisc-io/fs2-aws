# fs2-aws-sns

An FS2 Streams-based API for publishing messages to AWS SNS topics.

@:include(../_disclaimer.md)

### Import
```sbt
libraryDependencies += "io.laserdisc" %% "fs2-aws-sns" % "@VERSION@"
```

This module provides the `SNS[F]` algebra:

```scala
trait SNS[F[_]] {
    def publish(topicArn: String): Pipe[F, MsgBody, PublishResponse]
}
```

### Usage

To use `SNS[F]`, you need an instance of `SnsAsyncClientOp[F]`:
* This is a Tagless-Final wrapper around the `SnsAsyncClient`
* You get this automatically as `fs2-aws-sns` has a transitive dependency on `pure-sns-tagless` (see [pure-aws](../pure-aws.md)).

The general usage pattern is as follows:

```scala
// create the tagless-final wrapper resource (pass an SnsAsyncClient.builder()
// if you need to configure credentials, region, etc.)
val snsInterpreter = SnsInterpreter[IO].resource

// use the interpreter directly for effectful AWS SDK calls
snsInterpreter.use { snsOp =>
  SNS.create[IO](snsOp).flatMap { sns =>
    stream.through(sns.publish(topicArn))
    .. etc ..
  }
}
```

### Full Example

```scala mdoc:compile-only
import cats.effect.*
import fs2.aws.sns.sns.*
import io.laserdisc.pure.sns.tagless.SnsInterpreter
import software.amazon.awssdk.services.sns.model.ListTopicsResponse

val topicArn = "arn:aws:sns:us-east-1:123456789012:my-topic"

object SNSExample {

  // use the tagless-final wrapper directly for effectful AWS SDK calls
  def basicExample: IO[ListTopicsResponse] =
    SnsInterpreter[IO].resource.use { client =>
      client.listTopics
    }

  // or make use of the streaming API for publishing messages
  def fs2StreamingExample: IO[Unit] =
    SnsInterpreter[IO].resource.use { snsOp =>
      SNS.create[IO](snsOp).flatMap { sns =>
        fs2.Stream("hello", "world")
          .through(sns.publish(topicArn))
          .evalMap(resp => IO.println(s"published: ${resp.messageId()}"))
          .compile
          .drain
      }
    }
}

```

### Notes

Messages are published concurrently; the concurrency level is set via `SnsSettings` (default 10):

```scala
SNS.create[IO](snsOp, SnsSettings(concurrency = PosInt.unsafeFrom(20)))
```

Because publishing is concurrent, responses may be emitted in a different order than the incoming message bodies.

## Testing

The SNS algebra calls `SnsAsyncClientOp[F]`. The `pure-sns-tagless` test jar contains
`MockSnsAsyncClientOp[F]`, a stub where every method throws `NotImplementedError` until you override it.
Add the test jar as a test dependency:

```sbt
libraryDependencies += "io.laserdisc" %% "pure-sns-tagless" % "@VERSION@" % "tests"
```

Override the calls your code makes, then build the streaming algebra on top of the stub:

```scala mdoc:compile-only
import cats.effect.*
import cats.syntax.all.*
import fs2.aws.sns.sns.*
import io.laserdisc.pure.sns.tagless.MockSnsAsyncClientOp
import software.amazon.awssdk.services.sns.model.{PublishRequest, PublishResponse}

val snsOp = new MockSnsAsyncClientOp[IO] {
  override def publish(a: PublishRequest): IO[PublishResponse] =
    IO.println(s"published: ${a.message()}").as(PublishResponse.builder().build())
}

SNS.create[IO](snsOp).flatMap { sns =>
  fs2.Stream.emit("hello").covary[IO]
    .through(sns.publish("arn:aws:sns:us-east-1:123456789012:my-topic"))
    .compile
    .drain
}
```
