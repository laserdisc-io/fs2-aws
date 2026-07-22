# fs2-aws-sns

An FS2 Streams-based API for publishing messages to AWS SNS topics.

### Import:

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
* You get this automatically as `fs2-aws-sns` has a transitive dependency on `pure-sns-tagless` (see [pure-aws](pure-aws.md)).

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
