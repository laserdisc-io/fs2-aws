# fs2-aws-s3

An FS2 Streams-based API for common AWS S3 operations.

@:include(_disclaimer.md)

### Import
```sbt
libraryDependencies += "io.laserdisc" %% "fs2-aws-s3" % "@VERSION@"
```

This module provides the `S3[F]` algebra, e.g.:

```scala
trait S3[F[_]] {
    def uploadFile(bucket: BucketName, key: FileKey): Pipe[F, Byte, ETag]
    def readFile(bucket: BucketName, key: FileKey): Stream[F, Byte]
    // .. etc ..
}
```

### Usage

To use `S3[F]`, you need an instance of `S3AsyncClientOp[F]`:
* This is a Tagless-Final wrapper around the `S3AsyncClient`
* You get this automatically as `fs2-aws-s3` has a transitive dependency on `pure-s3-tagless` (see [pure-aws](pure-aws.md)).

The general usage pattern is as follows:

```scala
// create the tagless-final wrapper resource (pass an S3AsyncClient.builder()
// if you need to configure credentials, region, etc.)
val s3Interpreter = S3Interpreter[IO].resource

// use the interpreter directly for effectful AWS SDK calls
s3Interpreter.map(S3.create[IO]).use { s3 =>
  s3.uploadFile(bucket, key)
  .. etc ..
}
```

### Full Example

```scala mdoc:compile-only
import cats.effect.*
import fs2.aws.s3.S3
import fs2.aws.s3.models.Models.{BucketName, FileKey}
import io.laserdisc.pure.s3.tagless.S3Interpreter
import software.amazon.awssdk.services.s3.model.ListBucketsResponse

val bucket = BucketName.unsafeFrom("my-bucket")
val key    = FileKey.unsafeFrom("my-file.txt")

object S3Example {

  // use the tagless-final wrapper directly for effectful AWS SDK calls
  def basicExample: IO[ListBucketsResponse] =
    S3Interpreter[IO].resource.use { client =>
      client.listBuckets
    }
    
  // or make use of the streaming API for common S3 operations
  def fs2StreamingExample: IO[Unit] = {

    S3Interpreter[IO].resource
      .map(S3.create[IO])
      .use { s3 =>
        for {
          // upload
          etag <- fs2.Stream.emits("hello world".getBytes("UTF-8"))
            .through(s3.uploadFile(bucket, key))
            .compile
            .lastOrError

          // read
          _ <- s3
            .readFile(bucket, key)
            .through(fs2.text.utf8.decode)
            .through(fs2.text.lines)
            .evalMap(IO.println)
            .compile
            .drain
        } yield ()
      }
  }
}

```

### Notes

As a rule of thumb:

- Small files: use `readFile` and `uploadFile`.
- Big files: use `readFileMultipart` and `uploadFileMultipart` (part size is in MB and must be at least 5).


```scala
byteStream.through(s3.uploadFileMultipart(bucket, key, partSize = PartSizeMB.unsafeFrom(5)))

s3.readFileMultipart(bucket, key, partSize = PartSizeMB.unsafeFrom(5))
```

`uploadFileMultipart` optionally verifies the ETag returned by S3 (see `S3.MultipartETagValidation`) and supports request customisation via `AwsRequestModifier`.

