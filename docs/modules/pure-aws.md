# pure-aws

Machine-generated [tagless-final](https://okmij.org/ftp/tagless-final/index.html) wrappers for
the AWS SDK v2 **async** clients. Each module exposes the full client API as an algebra in an
arbitrary effect `F[_]: Async` — every `CompletableFuture`-returning SDK method becomes an
`F`-returning method — plus an `Interpreter` that builds instances (and manages the underlying
client as a `cats.effect.Resource`).

| Module | Wraps | Algebra |
|---|---|---|
| `pure-s3-tagless` | `S3AsyncClient` | `S3AsyncClientOp[F]` |
| `pure-sqs-tagless` | `SqsAsyncClient` | `SqsAsyncClientOp[F]` |
| `pure-sns-tagless` | `SnsAsyncClient` | `SnsAsyncClientOp[F]` |
| `pure-kinesis-tagless` | `KinesisAsyncClient` | `KinesisAsyncClientOp[F]` |
| `pure-dynamodb-tagless` | `DynamoDbAsyncClient` | `DynamoDbAsyncClientOp[F]` |
| `pure-cloudwatch-tagless` | `CloudWatchAsyncClient` | `CloudWatchAsyncClientOp[F]` |

```sbt
libraryDependencies += "io.laserdisc" %% "pure-sqs-tagless" % "@VERSION@"
```

These modules are the foundation the higher-level `fs2-aws-*` modules are built on, but they
are also useful on their own whenever you need a purely functional version of an SDK call that
has no streaming wrapper.

## Usage

Acquire the algebra as a `Resource` from the SDK client builder, then call SDK operations as
`F` effects:

```scala
import cats.effect.*
import io.laserdisc.pure.sqs.tagless.{Interpreter as SqsInterpreter, SqsAsyncClientOp}
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{CreateQueueRequest, SendMessageRequest}

val sqsResource: Resource[IO, SqsAsyncClientOp[IO]] =
  SqsInterpreter[IO].SqsAsyncClientOpResource(
    SqsAsyncClient.builder() // configure credentials/region/endpoint as needed
  )

sqsResource.use { sqs =>
  for {
    queue <- sqs.createQueue(CreateQueueRequest.builder().queueName("my-queue").build())
    _     <- sqs.sendMessage(
      SendMessageRequest.builder().queueUrl(queue.queueUrl()).messageBody("hello").build()
    )
  } yield ()
}
```

The interpreters also support `Kleisli`-based environment passing — see
[`PureAWS.scala`](https://github.com/laserdisc-io/fs2-aws/blob/main/fs2-aws-examples/src/main/scala/fs2/aws/examples/PureAWS.scala)
for an example combining SQS and SNS both ways.

## Code generation

The sources are generated from the SDK client interfaces by the `taglessGen` sbt task
(see `project/TaglessGen.scala`). To regenerate after an SDK bump:

```sh
make generate-pure-aws
```

