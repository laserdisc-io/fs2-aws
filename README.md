# fs2-aws
[![Build Status](https://travis-ci.com/dmateusp/fs2-aws.svg?branch=master)](https://travis-ci.com/dmateusp/fs2-aws)
[![Coverage Status](https://coveralls.io/repos/github/dmateusp/fs2-aws/badge.svg?branch=master)](https://coveralls.io/github/dmateusp/fs2-aws?branch=master)

fs2 Streaming utilities for interacting with AWS

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
  .uploadS3FileMultipart[IO]("testBucket", "testFile", 25)
```

## Kinesis
### Streaming records from Kinesis with KCL
Example using IO for effects (any monad `F <: ConcurrentEffect` can be used):
```scala
val stream: Stream[IO, CommittableRecord] = readFromKinesisStream[IO]("appName", "streamName")
```

There are a number of other stream constructors available where you can provide more specific configuration for the KCL worker.

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
**TODO:** Stream get SQS messages, Stream send SQS messages
