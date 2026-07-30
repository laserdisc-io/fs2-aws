# fs2-aws-kinesis

Consume AWS Kinesis streams as fs2 streams via the [Kinesis Client Library (KCL)](https://docs.aws.amazon.com/streams/latest/dev/shared-throughput-kcl-consumers.html),
and publish to Kinesis via the [Kinesis Producer Library (KPL)](https://docs.aws.amazon.com/streams/latest/dev/developing-producers-with-kpl.html).

@:include(_disclaimer.md)

### Import
```sbt
libraryDependencies += "io.laserdisc" %% "fs2-aws-kinesis" % "@VERSION@"
```

## Consuming records

`DefaultKinesisStreamBuilder` walks you through the required configuration phase by phase
(each `with*` step also has a `withDefault*` shortcut where a sensible default exists) and
yields a `Resource` managing the KCL scheduler lifecycle:

```scala
import cats.effect.*
import fs2.Stream
import fs2.aws.kinesis.{CommittableRecord, DefaultKinesisStreamBuilder}
import fs2.aws.kinesis.models.KinesisModels.{AppName, StreamName}
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.coordinator.CoordinatorConfig.ClientVersionConfig

def records(
    appName: AppName,
    streamName: StreamName,
    kinesis: KinesisAsyncClient,
    dynamo: DynamoDbAsyncClient,
    cloudWatch: CloudWatchAsyncClient
): Resource[IO, Stream[IO, CommittableRecord]] =
  DefaultKinesisStreamBuilder[IO]()
    .withAppName(appName)
    .withKinesisClient(kinesis)
    .withDynamoDBClient(dynamo)
    .withCloudWatchClient(cloudWatch)
    .withDefaultSchedulerId
    .withSingleStreamTracker(streamName)
    .withDefaultStreamTracker
    .withDefaultSchedulerConfigs
    .withDefaultBufferSize
    .withDefaultScheduler(ClientVersionConfig.CLIENT_VERSION_CONFIG_3X)
    .build
    .map(_.flatMap(Stream.chunk)) // the stream emits Chunk[CommittableRecord]
```

Use `withMultiStreamTracker` instead of `withSingleStreamTracker` to consume several streams
with one scheduler (see
[`KinesisMultistreamExample`](https://github.com/laserdisc-io/fs2-aws/blob/main/fs2-aws-examples/src/main/scala/fs2/aws/examples/KinesisMultistreamExample.scala)).

The older `Kinesis.create(...)` factory + `readFromKinesisStream(appName, streamName)` API is
still available and takes the same three SDK clients plus a `ClientVersionConfig`.

### Choosing the `ClientVersionConfig`

KCL 3.x changed the lease-coordination protocol, so to prevent accidental misconfiguration for existing applications, the `ClientVersionConfig` must be explicitly set.

* For a new application, use `CLIENT_VERSION_CONFIG_3X`. 
* For an existing application, see the [KCL 2.x-to-3.x migration guide](https://docs.aws.amazon.com/streams/latest/dev/kcl-migration-from-2-3.html) (and also the fs2-aws [v7 migration notes](../migration.md))

### Checkpointing records

Records must be checkpointed to record consumer progress. Checkpointing a record implicitly
checkpoints everything before it on the same shard. The `Kinesis[F]` algebra provides a pipe
that batches checkpoints per shard:

```scala
import fs2.aws.kinesis.{Kinesis, KinesisCheckpointSettings, KinesisConsumerSettings}

val kinesis: Kinesis[IO] = Kinesis.create[IO](kinesisClient, dynamoClient, cloudWatchClient, ClientVersionConfig.CLIENT_VERSION_CONFIG_3X)

kinesis
  .readFromKinesisStream(KinesisConsumerSettings("streamName", "appName"))
  .through(myProcessingPipeline)
  .through(kinesis.checkpointRecords(KinesisCheckpointSettings.defaultInstance))
```

## Publishing records

`fs2.aws.kinesis.publisher` provides pipes that write `(partitionKey, ByteBuffer)` tuples via
the KPL:

```scala
import fs2.aws.internal.KinesisProducerClientImpl
import fs2.aws.kinesis.publisher.writeToKinesis
import java.nio.ByteBuffer
import scala.concurrent.ExecutionContext.Implicits.global

Stream("testData")
  .map(d => ("partitionKey", ByteBuffer.wrap(d.getBytes)))
  .through(writeToKinesis[IO]("streamName", producer = new KinesisProducerClientImpl[IO]))
```

Credentials and region are configured on the `KinesisProducerClient` (by default, the AWS
default credentials chain and the KPL's own region resolution). Variants include `writeAndForgetToKinesis` (don't wait for
results) and `writeObjectToKinesis` (takes an implicit `I => ByteBuffer` encoder).

## Use with LocalStack

Consuming from and publishing to Kinesis in [LocalStack](https://localstack.cloud/) works. 
Set the client `endpointOverride` (e.g. `http://localhost:4566`) and use `Polling` retrieval
mode (LocalStack does not support `FanOut`). 

See [`KinesisExampleNew`](https://github.com/laserdisc-io/fs2-aws/blob/main/fs2-aws-examples/src/main/scala/fs2/aws/examples/KinesisExampleNew.scala) for a complete runnable example.

## Testing

See [fs2-aws-testkit](fs2-aws-testkit.md) for test doubles for both the consumer and the producer.

