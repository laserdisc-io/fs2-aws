# fs2-aws-sns

Publish messages to AWS SNS topics through an fs2 pipe.

```sbt
libraryDependencies += "io.laserdisc" %% "fs2-aws-sns" % "@VERSION@"
```

The module exposes a single algebra:

```scala
trait SNS[F[_]] {
  def publish(topicArn: String): Pipe[F, MsgBody, PublishResponse]
}
```

Messages are published concurrently; the concurrency level is set via `SnsSettings`
(default 10).

## Usage

Create an `SNS[F]` from an `SnsAsyncClientOp` (provided by
[`pure-sns-tagless`](pure-aws.md), a dependency of this module):

```scala
import cats.effect.*
import fs2.aws.sns.sns.*
import io.laserdisc.pure.sns.tagless.{Interpreter as SnsInterpreter, SnsAsyncClientOp}
import software.amazon.awssdk.services.sns.SnsAsyncClient

val snsResource: Resource[IO, SnsAsyncClientOp[IO]] =
  SnsInterpreter[IO].SnsAsyncClientOpResource(SnsAsyncClient.builder()) // configure credentials/region/endpoint as needed

snsResource.use { snsOp =>
  SNS.create[IO](snsOp).flatMap { sns =>
    fs2.Stream("hello", "world")
      .through(sns.publish("arn:aws:sns:us-east-1:123456789012:my-topic"))
      .evalTap(resp => IO.println(s"published: ${resp.messageId()}"))
      .compile
      .drain
  }
}
```

