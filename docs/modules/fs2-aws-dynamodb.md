# fs2-aws-dynamodb

Stream DynamoDB table scans as fs2 streams with back-pressure.

```sbt
libraryDependencies += "io.laserdisc" %% "fs2-aws-dynamodb" % "@VERSION@"
```

The module exposes a single algebra:

```scala
trait StreamScan[F[_]] {
  def scanDynamoDB(scanRequest: ScanRequest, pageSize: Int): Stream[F, Chunk[JMap[String, AttributeValue]]]
}
```

`scanDynamoDB` wraps the SDK's scan paginator in a bounded fs2 stream: at most `pageSize`
records are requested (and buffered) at a time, so a large table scan can't exhaust memory.
The stream emits raw DynamoDB items and terminates once the scan is exhausted.

## Usage

Create a `StreamScan[F]` from a `DynamoDbAsyncClientOp` (provided by
[`pure-dynamodb-tagless`](pure-aws.md), a dependency of this module):

```scala
import cats.effect.*
import fs2.aws.dynamodb.StreamScan
import io.laserdisc.pure.dynamodb.tagless.{Interpreter as DdbInterpreter, DynamoDbAsyncClientOp}
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.ScanRequest

val ddbResource: Resource[IO, DynamoDbAsyncClientOp[IO]] =
  DdbInterpreter[IO].DynamoDbAsyncClientOpResource(DynamoDbAsyncClient.builder()) // configure credentials/region/endpoint as needed

ddbResource.use { ddb =>
  StreamScan[IO](ddb)
    .scanDynamoDB(ScanRequest.builder().tableName("my-table").build(), pageSize = 100)
    .unchunks
    .evalMap(item => IO.println(item))
    .compile
    .drain
}
```

For a parallel scan across segments, see
[`DynamoParallelScan`](https://github.com/laserdisc-io/fs2-aws/blob/main/fs2-aws-examples/src/main/scala/fs2/aws/examples/DynamoParallelScan.scala).

