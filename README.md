# fs2-aws
![Build](https://github.com/laserdisc-io/fs2-aws/workflows/Build/badge.svg)
![Release](https://github.com/laserdisc-io/fs2-aws/workflows/Release/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.laserdisc/fs2-aws_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.laserdisc/fs2-aws_2.12)
[![Coverage Status](https://coveralls.io/repos/github/laserdisc-io/fs2-aws/badge.svg?branch=main)](https://coveralls.io/github/laserdisc-io/fs2-aws?branch=main)

fs2 Streaming utilities for interacting with AWS

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
`SQS.create` wraps one queue: `sqsStream` receives messages, and the pipes send, delete and change visibility.

```scala
import cats.effect.{IO, IOApp}
import fs2.Stream
import fs2.aws.sqs.{SQS, SqsConfig}
import io.laserdisc.pure.sqs.tagless.Interpreter
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import scala.concurrent.duration.*

object SqsExample extends IOApp.Simple {
  val queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789012/my-queue"

  def run: IO[Unit] =
    Interpreter[IO].SqsAsyncClientOpResource(SqsAsyncClient.builder()).use { sqsOp =>
      for {
        // long polling: each receive waits up to 20s for messages, so there's no need to pause between receives
        sqs <- SQS.createWithReceiveRequest[IO](SqsConfig(queueUrl, pollRate = Duration.Zero), sqsOp, _.waitTimeSeconds(20))
        _   <- Stream("hello", "world").through(sqs.sendMessagePipe).compile.drain
        _   <- sqs.sqsStream
          .evalTap(msg => IO.println(msg.body()))
          .through(sqs.deleteMessagePipe)
          .take(2)
          .compile
          .drain
      } yield ()
    }
}
```

- `SqsConfig(queueUrl, pollRate = 3.seconds, fetchMessageCount = 10)`: `fetchMessageCount` must be between 1 and 10. Only use
  `pollRate = Duration.Zero` with long polling (per request, or the queue's `ReceiveMessageWaitTimeSeconds`); without it, the
  stream busy-loops on empty receives.
- `SQS.createWithReceiveRequest` also takes a function that customizes every `ReceiveMessageRequest`: `_.waitTimeSeconds(20)`
  for long polling, `_.visibilityTimeout(...)` per receive, or
  `_.messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)` to spot redeliveries. The queue URL and
  message count always come from `SqsConfig`.
- A receive hides up to `fetchMessageCount` messages at once, but `sqsStream` hands them over one at a time. If handling can
  outlast the queue's visibility timeout, use `fetchMessageCount = 1`, or consume `sqsStreamBatched` (one chunk per receive)
  and extend or delete each batch as a whole.
- `changeMessageVisibilityPipe(timeout)` sets each message's timeout once, rounded up to whole seconds. It isn't a heartbeat:
  to keep a message hidden during long processing, call it again before the timeout runs out.

## Support

![YourKit Image](https://www.yourkit.com/images/yklogo.png "YourKit")

This project is supported by YourKit with monitoring and profiling Tools. YourKit supports open source with innovative and intelligent tools for monitoring and profiling Java and .NET applications. YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/), and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).


