# fs2-aws
![Build](https://github.com/laserdisc-io/fs2-aws/workflows/Build/badge.svg)
![Release](https://github.com/laserdisc-io/fs2-aws/workflows/Release/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.laserdisc/fs2-aws_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.laserdisc/fs2-aws_2.12)
[![Coverage Status](https://coveralls.io/repos/github/laserdisc-io/fs2-aws/badge.svg?branch=main)](https://coveralls.io/github/laserdisc-io/fs2-aws?branch=main)

fs2 Streaming utilities for interacting with AWS

## :warning: What's new in 7.x (unreleased)

This branch contains the next major release. It is a breaking change from the released 6.x series
(see [`series/6.x`](https://github.com/laserdisc-io/fs2-aws/tree/series/6.x) for the currently published library).

### AWS SDK v1 removed ([#1284](https://github.com/laserdisc-io/fs2-aws/issues/1284))

All remaining dependencies on the legacy `com.amazonaws` SDK are gone:

* **The DynamoDB Streams consumer has been removed.** It was built on the SDK v1-only
  `dynamodb-streams-kinesis-adapter`, which has no SDK v2 equivalent. `fs2-aws-dynamodb` now provides
  only the `StreamScan` table-scanning API. If you consume DynamoDB Streams, stay on 6.x or migrate
  to [Kinesis Data Streams for DynamoDB](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/kds.html).
* **The Kinesis Producer Library is now the SDK v2-based release** (`software.amazon.kinesis:amazon-kinesis-producer:1.0.7`,
  previously `com.amazonaws:amazon-kinesis-producer:0.15.12`). If you reference KPL types directly
  (e.g. `KinesisProducerConfiguration`, `UserRecordResult`), update imports from
  `com.amazonaws.services.kinesis.producer.*` to `software.amazon.kinesis.producer.*`.
  `KinesisProducerClientImpl` now exposes SDK v2 credentials types (`AwsCredentialsProvider`).

### KCL 2.6.0 → 3.5.0

The Kinesis consumer is now built on KCL 3.x, which changes how workers coordinate leases and
rebalance load. Consequences:

* **You must now choose a `ClientVersionConfig`** when using the default scheduler — both
  `withDefaultScheduler(...)` (stream builder) and `Kinesis.create(kinesisClient, dynamoClient, cloudWatchClient, ...)`
  require it. There is deliberately no default: upgrading a live KCL 2.x application requires a
  coordinated migration, and silently picking a mode for you could either break lease coordination
  mid-migration (`CLIENT_VERSION_CONFIG_3X` too early) or permanently disable KCL 3's improved
  rebalancing (`CLIENT_VERSION_CONFIG_COMPATIBLE_WITH_2X` forever). Pick
  `CLIENT_VERSION_CONFIG_COMPATIBLE_WITH_2X` while workers built against KCL 2.x still share the
  lease table, then switch to `CLIENT_VERSION_CONFIG_3X` once migration completes; new applications
  should use `CLIENT_VERSION_CONFIG_3X` from the start. Read the
  [KCL 2.x-to-3.x migration guide](https://docs.aws.amazon.com/streams/latest/dev/kcl-migration-from-2-3.html)
  before upgrading — KCL 3.x also needs additional DynamoDB/CloudWatch IAM permissions for its new
  coordination tables.
* **Shutdown is now graceful.** Releasing the consumer resource calls `startGracefulShutdown()` and
  waits (up to 30 seconds) for the scheduler's run loop to complete before the underlying AWS
  clients are released, instead of the previous abrupt `shutdown()`.

## Scope of the project

fs2-aws provides an [fs2](https://github.com/functional-streams-for-scala/fs2) interface to AWS services

The design goals are the same as fs2:
> compositionality, expressiveness, resource safety, and speed

## Using:

Find [the latest release version](https://github.com/laserdisc-io/fs2-aws/releases) and add the following dependency:

```sbt
libraryDependencies +=  "io.laserdisc" %% "fs2-aws" % "VERSION"
```

## S3

The module `fs2-aws-s3` provides a purely functional API to operate with the AWS-S3 API. It defines four functions:

```scala
trait S3[F[_]] {
  def delete(bucket: BucketName, key: FileKey): F[Unit]
  def uploadFile(bucket: BucketName, key: FileKey): Pipe[F, Byte, ETag]
  def uploadFileMultipart(bucket: BucketName, key: FileKey, partSize: PartSizeMB): Pipe[F, Byte, ETag]
  def readFile(bucket: BucketName, key: FileKey): Stream[F, Byte]
  def readFileMultipart(bucket: BucketName, key: FileKey, partSize: PartSizeMB): Stream[F, Byte]
}
```

You can find out more in the scaladocs for each function, but as a rule of thumb for:

- Small files: use `readFile` and `uploadFile`.
- Big files: use `readFileMultipart` and `uploadFileMultipart`.

You can also combine them as you see fit. For example, use `uploadFileMultipart` and then read it in one shot using `readFile`.

### Getting started with the S3 module

In order to create an instance of `S3` we need to first create an `S3Client`. Here's an example of the former:

```scala
def s3StreamResource: Resource[IO, S3AsyncClientOp[IO]] =
  for {
    credentials = AwsBasicCredentials.create("accesskey", "secretkey")
    port        = 4566
    s3 <- S3Interpreter[IO](blocker).S3AsyncClientOpResource(
      S3AsyncClient
        .builder()
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .endpointOverride(URI.create(s"http://localhost:$port"))
        .region(Region.US_EAST_1)
    )
  } yield s3
```

Now we can create our `S3[IO]` instance:

```scala

s3StreamResource.map(S3.create[IO]).use { s3 =>
  // do stuff with s3 here (or just share it with other functions)
}
```

Create it once and share it as an argument, as any other resource.

For more details on how to work with S3 streams follow [link](fs2-aws-examples/src/main/scala/fs2/aws/examples/S3Example.scala)

### Reading a file from S3

The simple way:

```scala
s3.readFile(BucketName("test"), FileKey("foo"))
  .through(fs2.text.utf8Decode)
  .through(fs2.text.lines)
  .evalMap(line => IO(println(line)))
```

The streaming way in a multipart fashion (part size is indicated in MBs and must be 5 or higher):

```scala
s3.readFileMultipart(BucketName("test"), FileKey("foo"), partSize = 5)
  .through(fs2.text.utf8Decode)
  .through(fs2.text.lines)
  .evalMap(line => IO(println(line)))
```

### Writing to a file in S3

The simple way:

```scala
Stream.emits("test data".getBytes("UTF-8"))
  .through(s3.uploadFile(BucketName("foo"), FileKey("bar"))
  .evalMap(t => IO(println(s"eTag: $t")))
```

The streaming way in a multipart fashion. Again, part size is indicated in MBs and must be 5 or higher.

```scala
Stream.emits("test data".getBytes("UTF-8"))
  .through(s3.uploadFileMultipart(BucketName("foo"), FileKey("bar"), partSize = 5))
  .evalMap(t => IO(println(s"eTag: $t")))
```

### Deleting a file in S3

There is a simple function to delete a file.

```scala
s3.delete(BucketName("foo"), FileKey("bar"))
```

## Kinesis
### Streaming records from Kinesis with KCL
Example using IO for effects (any monad `F <: ConcurrentEffect` can be used):
```scala
val stream: Stream[IO, CommittableRecord] = readFromKinesisStream[IO]("appName", "streamName")
```

There are a number of other stream constructors available where you can provide more specific configuration for the KCL worker.

#### Testing
TODO: Implement better test consumer

For now, you can stub CommitableRecord and create a fs2.Stream to emit these records:
```scala
val record = new Record()
  .withApproximateArrivalTimestamp(new Date())
  .withEncryptionType("encryption")
  .withPartitionKey("partitionKey")
  .withSequenceNumber("sequenceNum")
  .withData(ByteBuffer.wrap("test".getBytes))

val testRecord = CommittableRecord(
  "shardId0",
  mock[ExtendedSequenceNumber],
  0L,
  record,
  mock[RecordProcessor],
  mock[IRecordProcessorCheckpointer])
```

#### Checkpointing records
Records must be checkpointed in Kinesis to keep track of which messages each consumer has received. Checkpointing a record in the KCL will automatically checkpoint all records upto that record. To checkpoint records, a Pipe and Sink are available. To help distinguish whether a record has been checkpointed or not, a CommittableRecord class exists to denote a record that hasn't been checkpointed, while the base Record class denotes a committed record.

```scala
readFromKinesisStream[IO]("appName", "streamName")
  .through(someProcessingPipeline)
  .to(checkpointRecords_[IO]())
```

### Publishing records to Kinesis with KPL
A Pipe and Sink allow for writing a stream of tuple2 (partitionKey, ByteBuffer) to a Kinesis stream.

Example:
```scala
Stream("testData")
  .map { d => ("partitionKey", ByteBuffer.wrap(d.getBytes))}
  .to(writeToKinesis_[IO]("streamName"))
```

AWS credential chain and region can be configured by overriding the respective fields in the KinesisProducerClient parameter to `writeToKinesis`. Defaults to using the default AWS credentials chain and `us-east-1` for region.

### Use with LocalStack

In some situations (e.g. local dev and automated tests), it may be desirable to be able to consume from and publish to Kinesis running inside LocalStack.
Ensure that the `endpoint` setting is set correctly (e.g. http://localhost:4566) and that `retrievalMode` is set to `Polling` (LocalStack doesn't support `FanOut`).

## Kinesis Firehose
**TODO:** Stream get data, Stream send data

## SQS
Example
```scala
implicit val messageDecoder: Message => Either[Throwable, Quote] = { sqs_msg =>
    io.circe.parser.decode[Quote](sqs_msg.asInstanceOf[TextMessage].getText)
}
fs2.aws
      .sqsStream[IO, Quote](
        sqsConfig,
        (config, callback) => SQSConsumerBuilder(config, callback))
      .through(...)
      .compile
      .drain
      .as(ExitCode.Success)
```

Testing
```scala
//create stream for testing
def stream(deferredListener: Deferred[IO, MessageListener]) =
            aws.testkit
              .sqsStream[IO, Quote](deferredListener)
              .through(...)
              .take(2)
              .compile
              .toList

//create the program for testing the stream
import io.circe.fs2.aws.examples.syntax._
import io.circe.generic.auto._
val quote = Quote(...)
val program : IO[List[(Quote, MessageListener)]] = for {
            d <- Deferred[IO, MessageListener]
            r <- IO.racePair(stream(d), d.get).flatMap {
              case Right((streamFiber, listener)) =>
                //simulate SQS stream fan-in here
                listener.onMessage(new SQSTextMessage(Printer.noSpaces.pretty(quote.asJson)))
                streamFiber.join
              case _ => IO(Nil)
            }
          } yield r

//Assert results
val result = program
            .unsafeRunSync()
result should be(...)
```
**TODO:** Stream send SQS messages

## Support

![YourKit Image](https://www.yourkit.com/images/yklogo.png "YourKit")

This project is supported by YourKit with monitoring and profiling Tools. YourKit supports open source with innovative and intelligent tools for monitoring and profiling Java and .NET applications. YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/), and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).


