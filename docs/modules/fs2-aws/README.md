# fs2-aws

FS2 streaming wrappers for the AWS SDK v2 clients. Each `fs2-aws-*` module adds a streaming API
on top of a [pure-aws](../pure-aws.md) client, so you can read, write and consume AWS services as
back-pressured `Stream`s and `Pipe`s.

@:include(../_disclaimer.md)

## How the modules are built

Every AWS service has two layers:

1. **Generated algebra** (`pure-aws`). Each SDK async client becomes a tagless-final algebra,
   e.g. `SqsAsyncClientOp[F]`. Every `CompletableFuture`-returning method becomes an `F`-returning
   method, and an interpreter manages the client as a `Resource`. The algebra is generated from the
   SDK, so it tracks each SDK release.
2. **Streaming algebra** (`fs2-aws`). A hand-written module wraps the generated algebra and exposes
   the operations that suit streams, such as a `Pipe` that publishes a stream of messages or a
   `Stream` that reads an object. The module's algebra is named after the service, e.g. `S3[F]`,
   `SQS[F]`, `SNS[F]`.

The streaming algebra is built with a `create` factory that takes the generated algebra, e.g.
`SQS.create[IO](config, sqsOp)`, so you control the client lifecycle. `fs2-aws-kinesis` is the
exception: it takes the SDK clients for the Kinesis Client Library.

## Modules

| Module | Streaming algebra | Built on |
|---|---|---|
| [`fs2-aws-s3`](fs2-aws-s3.md) | `S3[F]` | `pure-s3-tagless` |
| [`fs2-aws-sqs`](fs2-aws-sqs.md) | `SQS[F]` | `pure-sqs-tagless` |
| [`fs2-aws-sns`](fs2-aws-sns.md) | `SNS[F]` | `pure-sns-tagless`, `pure-sqs-tagless` |
| [`fs2-aws-kinesis`](fs2-aws-kinesis.md) | `Kinesis[F]` | `pure-kinesis-tagless`, `pure-dynamodb-tagless`, `pure-cloudwatch-tagless` |
| [`fs2-aws-dynamodb`](fs2-aws-dynamodb.md) | `StreamScan[F]` | `pure-dynamodb-tagless` |
| [`fs2-aws-testkit`](fs2-aws-testkit.md) | test doubles for the Kinesis consumer and producer | `fs2-aws-kinesis` |

The underlying clients are documented on the [pure-aws](../pure-aws.md) page, including the
interpreters and the `clientResource` option for passing your own SDK client builder.

### Import

Add the module you need. Each module pulls in its `pure-*` dependency transitively:

```sbt
libraryDependencies += "io.laserdisc" %% "fs2-aws-s3" % "@VERSION@"
```

## Testing

Each streaming module calls a tagless-final algebra from [pure-aws](../pure-aws.md), and each of those
pure modules publishes a `tests` jar with a `Mock…AsyncClientOp` stub. Add the stub as a test dependency
and override the calls your code makes. `fs2-aws-kinesis` is the exception: use
[fs2-aws-testkit](fs2-aws-testkit.md). See each module's page for an example.
