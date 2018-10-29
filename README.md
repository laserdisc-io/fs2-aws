# fs2-aws
[![Build Status](https://travis-ci.com/dmateusp/fs2-aws.svg?branch=master)](https://travis-ci.com/dmateusp/fs2-aws)
[![Coverage Status](https://coveralls.io/repos/github/dmateusp/fs2-aws/badge.svg?branch=master)](https://coveralls.io/github/dmateusp/fs2-aws?branch=master)

fs2 Streaming utilities for interacting with AWS

## S3
* Downloading / reading an `S3` file to `Byte`s, the size of each part downloaded is the `chunkSize`
`readS3FileMultipart[F[_]](bucket: String, key: String, chunkSize: Int, s3Client: S3Client[F] = new S3Client[F] {})(implicit F: Effect[F]): fs2.Stream[F, Byte]`

* Uploading multipart `Byte`s to `S3`, the size of each part uploaded is the `chunkSize` `uploadS3FileMultipart[F[_]](bucket: String, key: String, chunkSize: Int, s3Client: S3Client[F] = new S3Client[F] {})(implicit F: Effect[F]): fs2.Sink[F, Byte]`

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
A Pipe and Sink allow for writing a stream of bytes to a Kinesis stream.

Example:
```scala
someStream
  .to(writeToKinesis_[IO]("streamName", "partitionKey"))
```

## Kinesis Firehose
**TODO:** Stream get data, Stream send data

## SQS
**TODO:** Stream get SQS messages, Stream send SQS messages
