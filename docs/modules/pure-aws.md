# pure-aws

Machine-generated [tagless-final](https://okmij.org/ftp/tagless-final/index.html) wrappers for the AWS SDK v2 **async** clients. 

@:include(_disclaimer.md)

Each module exposes the full client API as an algebra in an
arbitrary effect `F[_]: Async` — every `CompletableFuture`-returning SDK method becomes an
`F`-returning method — plus an interpreter that builds instances (and manages the underlying
client as a `cats.effect.Resource`).

| Module | Wraps | Algebra | Interpreter |
|---|---|---|---|
| `pure-s3-tagless` | `S3AsyncClient` | `S3AsyncClientOp[F]` | `S3Interpreter` |
| `pure-sqs-tagless` | `SqsAsyncClient` | `SqsAsyncClientOp[F]` | `SqsInterpreter` |
| `pure-sns-tagless` | `SnsAsyncClient` | `SnsAsyncClientOp[F]` | `SnsInterpreter` |
| `pure-kinesis-tagless` | `KinesisAsyncClient` | `KinesisAsyncClientOp[F]` | `KinesisInterpreter` |
| `pure-dynamodb-tagless` | `DynamoDbAsyncClient` | `DynamoDbAsyncClientOp[F]` | `DynamoDbInterpreter` |
| `pure-cloudwatch-tagless` | `CloudWatchAsyncClient` | `CloudWatchAsyncClientOp[F]` | `CloudWatchInterpreter` |

### Import
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
import io.laserdisc.pure.sqs.tagless.{SqsInterpreter, SqsAsyncClientOp}
import software.amazon.awssdk.services.sqs.model.{CreateQueueRequest, SendMessageRequest}

val sqsResource: Resource[IO, SqsAsyncClientOp[IO]] =
  SqsInterpreter[IO].resource

sqsResource.use { sqs =>
  for {
    queue <- sqs.createQueue(CreateQueueRequest.builder().queueName("my-queue").build())
    _     <- sqs.sendMessage(
      SendMessageRequest.builder().queueUrl(queue.queueUrl()).messageBody("hello").build()
    )
  } yield ()
}
```

To configure credentials/region/endpoint, pass a builder:
`SqsInterpreter[IO].resource(SqsAsyncClient.builder().region(...))`. When you want the raw SDK
client rather than the algebra (e.g. to hand it to the KCL), use `clientResource` instead of
`resource`.

The interpreters also support `Kleisli`-based environment passing — see
[`PureAWS.scala`](https://github.com/laserdisc-io/fs2-aws/blob/main/fs2-aws-examples/src/main/scala/fs2/aws/examples/PureAWS.scala)
for an example combining SQS and SNS both ways.

## Code generation

The sources are generated from the SDK client interfaces by the `taglessGen` sbt task
(see `project/TaglessGen.scala`). To regenerate after an SDK bump:

```sh
make generate-pure-aws
```

