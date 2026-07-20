# fs2-aws-s3

A purely functional API for processing S3 objects as FS2 byte streams.

```sbt
libraryDependencies += "io.laserdisc" %% "fs2-aws-s3" % "@VERSION@"
```

The module exposes a single algebra:

```scala
trait S3[F[_]] {
  def delete(bucket: BucketName, key: FileKey): F[Unit]
  def uploadFile(bucket: BucketName, key: FileKey, awsRequestModifier: AwsRequestModifier.Upload1 = ...): Pipe[F, Byte, ETag]
  def uploadFileMultipart(bucket: BucketName, key: FileKey, partSize: PartSizeMB, ...): Pipe[F, Byte, Option[ETag]]
  def readFile(bucket: BucketName, key: FileKey): Stream[F, Byte]
  def readFileMultipart(bucket: BucketName, key: FileKey, partSize: PartSizeMB): Stream[F, Byte]
}
```

As a rule of thumb:

- Small files: use `readFile` and `uploadFile`.
- Big files: use `readFileMultipart` and `uploadFileMultipart` (part size is in MB and must be at least 5).

## Usage

Create an `S3[F]` from an `S3AsyncClientOp[F]`:

```scala mdoc:compile-only
import cats.effect.*
import fs2.aws.s3.S3
import fs2.aws.s3.models.Models.{BucketName, FileKey}
import io.laserdisc.pure.s3.tagless.{Interpreter as S3Interpreter, S3AsyncClientOp}
import software.amazon.awssdk.services.s3.S3AsyncClient

val bucket = BucketName.unsafeFrom("my-bucket")
val key = FileKey.unsafeFrom("my-file.txt")

S3Interpreter[IO].S3AsyncClientOpResource(S3AsyncClient.builder())
  .map(S3.create[IO]).use { s3 =>
    for {
      // upload
      etag <- fs2.Stream
        .emits("hello world".getBytes("UTF-8"))
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

      // delete
      _ <- s3.delete(bucket, key)
    } yield ()
  }
```

For large files, swap in the multipart variants (`partSize` in MB, minimum 5):

```scala
byteStream.through(s3.uploadFileMultipart(bucket, key, partSize = PartSizeMB.unsafeFrom(5)))

s3.readFileMultipart(bucket, key, partSize = PartSizeMB.unsafeFrom(5))
```

`uploadFileMultipart` optionally verifies the ETag returned by S3 (see
`S3.MultipartETagValidation`) and supports request customisation via `AwsRequestModifier`.

