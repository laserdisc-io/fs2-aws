# fs2-aws
[![Build Status](https://travis-ci.org/laserdisc-io/fs2-aws.svg?branch=master)](https://travis-ci.org/laserdisc-io/fs2-aws)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.laserdisc/fs2-aws_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.laserdisc/fs2-aws_2.12)
[![Coverage Status](https://coveralls.io/repos/github/laserdisc-io/fs2-aws/badge.svg?branch=master)](https://coveralls.io/github/laserdisc-io/fs2-aws?branch=master)

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

In order to create an instance of `S3` we need to first create an `S3Client`, as well as a `cats.effect.Blocker`. Here's an example of the former:

```scala
import cats.effect._
import java.net.URI
import software.amazon.awssdk.services.s3.S3Client

val mkS3Client: Resource[IO, S3Client] =
  Resource.fromAutoCloseable(
    IO(S3Client.builder().endpointOverride(URI.create("http://localhost:9000")).build())
  )
```

A `Blocker` can be easily created using its `apply` method and then share it. You should only create a single instance. Now we can create our `S3[IO]` instance:

```scala
import fs2.aws.s3._

S3.create[IO](client, blocker).flatMap { s3 =>
  // do stuff with s3 here (or just share it with other functions)
}
```

Create it once and share it as an argument, as any other resource.

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
  .through(s3.uploadFile(BucketName("foo"), FileKey("bar"), partSize = 5))
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

For now, you can stubbed CommitableRecord and create a fs2.Stream to emit these records:
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
Records must be checkpointed in Kinesis to keep track of which messages each consumer has received. Checkpointing a record in the KCL will automatically checkpoint all records upto that record. To checkpoint records, a Pipe and Sink are available. To help distinguish whether a record has been checkpointed or not, a CommittableRecord class exists to denote a record that hasn't been checkpointed, while the base Record class denotes a commited record.

```scala
readFromKinesisStream[IO]("appName", "streamName")
  .through(someProcessingPipeline)
  .to(checkpointRecords_[IO]())
```

### Publishing records to Kinesis with KPL
A Pipe and Sink allow for writing a stream of tuple2 (paritionKey, ByteBuffer) to a Kinesis stream.

Example:
```scala
Stream("testData")
  .map { d => ("partitionKey", ByteBuffer.wrap(d.getBytes))}
  .to(writeToKinesis_[IO]("streamName"))
```

AWS credential chain and region can be configured by overriding the respective fields in the KinesisProducerClient parameter to `writeToKinesis`. Defaults to using the default AWS credentials chain and `us-east-1` for region.

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
def stream(deferedListener: Deferred[IO, MessageListener]) =
            aws.testkit
              .sqsStream[IO, Quote](deferedListener)
              .through(...)
              .take(2)
              .compile
              .toList

//create the program for testing the stream
import io.circe.syntax._
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

