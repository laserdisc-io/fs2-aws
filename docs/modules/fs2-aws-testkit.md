# fs2-aws-testkit

Test doubles for the [fs2-aws-kinesis](fs2-aws-kinesis.md) consumer and producer,
letting you exercise stream logic without a real KCL scheduler or KPL daemon.

```sbt
libraryDependencies += "io.laserdisc" %% "fs2-aws-testkit" % "@VERSION@" % Test
```

## Testing a consumer: `SchedulerFactoryTestContext`

`Kinesis.create` accepts any `ShardRecordProcessorFactory => F[Scheduler]`.
`SchedulerFactoryTestContext` is such a factory backed by a mock scheduler: it captures the
record processors the consumer registers, so your test can push records through them directly:

```scala
import cats.effect.IO
import fs2.aws.kinesis.Kinesis
import fs2.aws.testkit.SchedulerFactoryTestContext

val testContext = new SchedulerFactoryTestContext[IO](shards = 1)
val kinesis     = Kinesis.create[IO](testContext)

val consume = kinesis
  .readFromKinesisStream("test-app", "test-stream")
  .take(1)
  .compile
  .toList

// in parallel with running `consume`, feed records through the captured processor:
val processor = testContext.getShardProcessors.head
// processor.initialize(...); processor.processRecords(...)
```

See [`NewKinesisConsumerSpec`](https://github.com/laserdisc-io/fs2-aws/blob/main/fs2-aws-kinesis/src/test/scala/fs2/aws/kinesis/NewKinesisConsumerSpec.scala)
for full patterns, including checkpointing assertions.

## Testing a producer: `TestKinesisProducerClient`

A `KinesisProducerClient[F]` that decodes each published `ByteBuffer` as JSON (via a circe
`Decoder`) and accumulates the results in a `Ref`, instead of talking to Kinesis:

```scala
import cats.effect.{IO, Ref}
import fs2.aws.kinesis.publisher.writeToKinesis
import fs2.aws.testkit.TestKinesisProducerClient
import io.circe.generic.auto.*
import io.circe.syntax.*
import java.nio.ByteBuffer
import scala.concurrent.ExecutionContext.Implicits.global

case class Quote(symbol: String, price: BigDecimal)

for {
  state <- Ref.of[IO, List[Quote]](Nil)
  producer = TestKinesisProducerClient[IO, Quote](state)
  _ <- fs2.Stream(Quote("AAPL", 42))
    .map(q => (q.symbol, ByteBuffer.wrap(q.asJson.noSpaces.getBytes)))
    .through(writeToKinesis[IO]("test-stream", producer = producer))
    .compile
    .drain
  published <- state.get // List(Quote("AAPL", 42))
} yield published
```

