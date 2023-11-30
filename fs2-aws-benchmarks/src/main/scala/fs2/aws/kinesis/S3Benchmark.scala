package fs2.aws.kinesis

import cats.effect.unsafe.IORuntime
import cats.effect.{ExitCode, IO, Resource}
import eu.timepit.refined.types.string.NonEmptyString
import fs2.Chunk
import fs2.aws.s3.S3
import fs2.aws.s3.models.Models.{BucketName, FileKey, PartSizeMB}
import io.laserdisc.pure.s3.tagless.{Interpreter as S3Interpreter, S3AsyncClientOp}
import org.openjdk.jmh.annotations.{Benchmark, Scope, State}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.{S3AsyncClient, S3Configuration}

import java.net.URI
import scala.concurrent.ExecutionContext
import scala.util.Random

/*
these are the numbers from my M2 macbook pro.

Result "fs2.aws.kinesis.S3Benchmark.S3UploadMultipartFlowBenchmark.S3MultipartUpload":
Just streaming 10MB
 29.185 ±(99.9%) 0.185 ops/s [Average]
[info]   (min, avg, max) = (28.808, 29.185, 29.534), stdev = 0.213
[info]   CI (99.9%): [29.000, 29.370] (assumes normal distribution)

Digest turned off
11.768 ±(99.9%) 0.404 ops/s [Average]
(min, avg, max) = (9.823, 11.768, 12.139), stdev = 0.466
CI (99.9%): [11.364, 12.173] (assumes normal distribution)

Digest turned on
10.089 ±(99.9%) 0.021 ops/s [Average]
(min, avg, max) = (10.047, 10.089, 10.143), stdev = 0.024
CI (99.9%): [10.068, 10.110] (assumes normal distribution)

1GB
0.274 ±(99.9%) 0.001 ops/s [Average]
[info]   (min, avg, max) = (0.273, 0.274, 0.276), stdev = 0.001
[info]   CI (99.9%): [0.274, 0.275] (assumes normal distribution)

Digest turned off
0.116 ±(99.9%) 0.001 ops/s [Average]
[info]   (min, avg, max) = (0.116, 0.116, 0.117), stdev = 0.001
[info]   CI (99.9%): [0.116, 0.116] (assumes normal distribution)

Digest turned on
0.118 ±(99.9%) 0.001 ops/s [Average]
[info]   (min, avg, max) = (0.117, 0.118, 0.119), stdev = 0.001
[info]   CI (99.9%): [0.117, 0.118] (assumes normal distribution)
[info] # Run complete. Total time: 00:08:26

Digest without array allocation -> iterate over bytes and update digest
0.119 ±(99.9%) 0.001 ops/s [Average]
[info]   (min, avg, max) = (0.118, 0.119, 0.119), stdev = 0.001
[info]   CI (99.9%): [0.119, 0.119] (assumes normal distribution)
[info] # Run complete. Total time: 00:08:26
 */

object S3Benchmark {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val runtime: IORuntime   = IORuntime.global

  @State(Scope.Benchmark)
  class ThreadState {

    val credentials: AwsBasicCredentials =
      AwsBasicCredentials
        .create("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
    val port = 9000
  }

  class S3UploadMultipartFlowBenchmark {

    def s3StreamResource1: Resource[IO, S3AsyncClientOp[IO]] =
      Resource.pure(new S3OpsStub)
    def s3StreamResource(credentials: AwsBasicCredentials, port: Int): Resource[IO, S3AsyncClientOp[IO]] =
      S3Interpreter[IO].S3AsyncClientOpResource(
        S3AsyncClient
          .builder()
          .credentialsProvider(StaticCredentialsProvider.create(credentials))
          .endpointOverride(URI.create(s"http://localhost:$port"))
          .serviceConfiguration(
            // see https://stackoverflow.com/a/61602647
            S3Configuration
              .builder()
              .pathStyleAccessEnabled(true)
              .build()
          )
          .region(Region.US_EAST_1)
      )

    def program(s3: S3[IO]): IO[Unit] =
      fs2.Stream
        .repeatEval[IO, Chunk[Byte]](IO.delay(Chunk.from(Random.nextBytes(1024))))
        .flatMap(fs2.Stream.chunk)
        .take(1L * 1024L * 1024L * 1024L) // 1GB
        .through(
          s3.uploadFileMultipart(
            BucketName(NonEmptyString.unsafeFrom("resources")),
            FileKey(NonEmptyString.unsafeFrom("foo")),
            PartSizeMB.unsafeFrom(5),
            false,
            10
//            MultipartETagValidation.create[IO].some
          )
        )
        .compile
        .drain

    @Benchmark
    def S3MultipartUpload(state: ThreadState): Unit =
      s3StreamResource1
        .map(S3.create[IO])
        .use(s3 => program(s3).as(ExitCode.Success))
        .unsafeRunSync()
  }
}
