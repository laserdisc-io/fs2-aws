# fs2-aws
[![Build Status](https://travis-ci.org/laserdisc-io/fs2-aws.svg?branch=master)](https://travis-ci.org/laserdisc-io/fs2-aws)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.laserdisc/fs2-aws_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.laserdisc/fs2-aws_2.12)
[![Coverage Status](https://coveralls.io/repos/github/io.laserdisc/fs2-aws/badge.svg?branch=master)](https://coveralls.io/github/laserdisc-io/fs2-aws?branch=master)

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
### Streaming a file from S3
Creates a stream of `Byte`s; size of each part downlaoded is the `chunkSize`.

Example using IO for effects (any monad `F <: Effect` can be used):
```scala
readS3FileMultipart[IO]("testBucket", "testFile", 25)
  .through(io.file.writeAll(Paths.get("testFile.txt")))
```

### Writing to a file in S3
A Pipe and Sink allow for writing a stream of `Byte`s to S3; size of each part uploaded is the `chunkSize`.

Example using IO for effects (any monad `F <: Effect` can be used):
```scala
Stream("test data")
  .flatMap(_.getBytes)
  .uploadS3FileMultipart[IO]("testBucket", "testFile")
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
