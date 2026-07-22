# Migrating from 6.x to 7.x

`fs2-aws` 7.x is a major release with breaking changes: 

* Full migration to AWS SDK v2 and removal of the v1 SDK from the dependency tree.
* An upgrade to the Kinesis Client Library (KCL) 3.x, which changes how consumers coordinate leases and rebalance load.
* Removal of the DynamoDB Streams consumer, which was built on the v1-only `dynamodb-streams-kinesis-adapter`.
* Renaming of the `pure-aws` interpreters and resource constructors to be more descriptive and avoid name clashes.

## AWS SDK v1 → v2

All [pure-aws](modules/pure-aws.md) modules now wrap the v2 SDK clients.  The AWS SDK v1 is [end-of-life](https://aws.amazon.com/blogs/developer/announcing-end-of-support-for-aws-sdk-for-java-v1-x-on-december-31-2025/) and has been removed as a dependency.  

If you were using the v1 SDK directly in your application, you should migrate to v2.  See the [AWS SDK v2 migration guide](https://docs.aws.amazon.com/sdk-for-java/latest/migration-guide/index.html) for details.

## Kinesis Client Library (KCL) 2.x → 3.x

[`fs2-aws-kinesis`](modules/fs2-aws-kinesis.md) now builds on KCL 3.x, which changes how workers coordinate leases and
rebalance load across consumers.

Upgrading to the KCL 3.x for an existing application requires you to deploy your consumer nodes in a sequence of 'compatibility modes' 
to ensure that you switch to the new protocol in a safe manner.

To do this, consumers must be configured with a particular [ClientVersionConfig](https://github.com/awslabs/amazon-kinesis-client/blob/a32fa5845f9bd79b651ef855af607cf58787642e/amazon-kinesis-client/src/main/java/software/amazon/kinesis/coordinator/CoordinatorConfig.java#L130) constant.
* **There is deliberately no default value**, you must select an appropriate mode for each consumer.
* If this is a new application, simply pass `CLIENT_VERSION_CONFIG_3X` to the scheduler and you are done.
* Read the official [KCL 2.x-to-3.x migration guide](https://docs.aws.amazon.com/streams/latest/dev/kcl-migration-from-2-3.html) to understand the implications of each mode.
* The KCL 3.x also needs additional DynamoDB and CloudWatch IAM permissions. 

Once you have determined your upgrade path, instantiate the [kinesis client](modules/fs2-aws-kinesis.md) as follows (for example, using the final `CLIENT_VERSION_CONFIG_3X`):

```scala
import software.amazon.kinesis.coordinator.CoordinatorConfig.ClientVersionConfig.*

Kinesis.create[IO](kc, ddb, cw, CLIENT_VERSION_CONFIG_3X)

// or
DefaultKinesisStreamBuilder[IO]()
  .withAppName(appName)
  // ...clients, stream tracker, etc...
  .withDefaultScheduler(CLIENT_VERSION_CONFIG_3X)
```

### KCL 3.x graceful shutdown

Releasing the consumer resource now calls the scheduler's `startGracefulShutdown()` and waits up
to 30 seconds for the run loop to complete before the underlying AWS clients are released,
instead of the previous abrupt `shutdown()`. In-flight records get a chance to checkpoint;
expect resource release to take a little longer than before.

## DynamoDB Streams consumer removed

The `dynamodb-streams-kinesis-adapter` was a v1-only library that allowed KCL 2.x to consume DynamoDB Streams. 

It has no v2 equivalent, and the `fs2-aws-dynamodb` streams consumer has been removed.  

While we may add a DynamoDB Streams API client here in the future, if you want to use fs2-aws to consume DynamoDB Streams, you must either: 

- stay on fs2-aws 6.x
- migrate to [Kinesis Data Streams for DynamoDB](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/kds.html) and consume it with [`fs2-aws-kinesis`](modules/fs2-aws-kinesis.md).

## `pure-aws`: interpreter renames

Each generated module's `Interpreter` is now named after the client it wraps, so imports no
longer need renames to disambiguate:

| Package | 6.x | 7.x |
|---|---|---|
| `io.laserdisc.pure.s3.tagless` | `Interpreter` | `S3Interpreter` |
| `io.laserdisc.pure.sqs.tagless` | `Interpreter` | `SqsInterpreter` |
| `io.laserdisc.pure.sns.tagless` | `Interpreter` | `SnsInterpreter` |
| `io.laserdisc.pure.kinesis.tagless` | `Interpreter` | `KinesisInterpreter` |
| `io.laserdisc.pure.dynamodb.tagless` | `Interpreter` | `DynamoDbInterpreter` |
| `io.laserdisc.pure.cloudwatch.tagless` | `Interpreter` | `CloudWatchInterpreter` |

The `Resource` constructors have shorter names too, and gained no-argument variants that use a
default client builder:

| 6.x | 7.x                                                         |
|---|-------------------------------------------------------------|
| `SqsAsyncClientOpResource(builder)` | `resource(builder)` or just `resource` for a default builder |
| `SqsAsyncClientResource(builder)` | `clientResource(builder)` or just `clientResource`         |

So, using SQS as an example:

```scala
// 7.x
import io.laserdisc.pure.sqs.tagless.{SqsInterpreter, SqsAsyncClientOp}

val sqs: Resource[IO, SqsAsyncClientOp[IO]] =
  SqsInterpreter[IO].resource // pass a builder only if you need to configure it
```

The old names still compile. `Interpreter` remains as a deprecated alias and the old resource
methods as deprecated forwarders.

