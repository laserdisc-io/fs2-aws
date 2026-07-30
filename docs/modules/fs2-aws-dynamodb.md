# fs2-aws-dynamodb

An FS2 Streams-based API for scanning AWS DynamoDB tables with back-pressure.

@:include(_disclaimer.md)

### Import
```sbt
libraryDependencies += "io.laserdisc" %% "fs2-aws-dynamodb" % "@VERSION@"
```

This module provides the `StreamScan[F]` algebra:

```scala
trait StreamScan[F[_]] {
    def scanDynamoDB(scanRequest: ScanRequest, pageSize: Int): Stream[F, Chunk[JMap[String, AttributeValue]]]
}
```

### Usage

To use `StreamScan[F]`, you need an instance of `DynamoDbAsyncClientOp[F]`:
* This is a Tagless-Final wrapper around the `DynamoDbAsyncClient`
* You get this automatically as `fs2-aws-dynamodb` has a transitive dependency on `pure-dynamodb-tagless` (see [pure-aws](pure-aws.md)).

The general usage pattern is as follows:

```scala
// create the tagless-final wrapper resource (pass a DynamoDbAsyncClient.builder()
// if you need to configure credentials, region, etc.)
val ddbInterpreter = DynamoDbInterpreter[IO].resource

// use the interpreter directly for effectful AWS SDK calls
ddbInterpreter.map(StreamScan[IO](_)).use { scanner =>
  scanner.scanDynamoDB(scanRequest, pageSize)
  .. etc ..
}
```

### Full Example

```scala mdoc:compile-only
import cats.effect.*
import fs2.aws.dynamodb.StreamScan
import io.laserdisc.pure.dynamodb.tagless.DynamoDbInterpreter
import software.amazon.awssdk.services.dynamodb.model.{ListTablesResponse, ScanRequest}

val scanRequest = ScanRequest.builder().tableName("my-table").build()

object DynamoDBExample {

  // use the tagless-final wrapper directly for effectful AWS SDK calls
  def basicExample: IO[ListTablesResponse] =
    DynamoDbInterpreter[IO].resource.use { client =>
      client.listTables
    }

  // or make use of the streaming API for scanning tables
  def fs2StreamingExample: IO[Unit] =
    DynamoDbInterpreter[IO].resource
      .map(StreamScan[IO](_))
      .use { scanner =>
        scanner
          .scanDynamoDB(scanRequest, pageSize = 100)
          .unchunks
          .evalMap(item => IO.println(item))
          .compile
          .drain
      }
}

```

### Notes

`scanDynamoDB` wraps the SDK's scan paginator in a bounded fs2 stream: at most `pageSize` records are requested (and buffered) at a time, so a large table scan can't exhaust memory. The stream emits raw DynamoDB items and terminates once the scan is exhausted.

For a parallel scan across segments, see [`DynamoParallelScan`](https://github.com/laserdisc-io/fs2-aws/blob/main/fs2-aws-examples/src/main/scala/fs2/aws/examples/DynamoParallelScan.scala).
