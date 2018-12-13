package fs2
package aws
package internal

import java.io.InputStream
import java.nio.ByteBuffer

import fs2.concurrent.Queue
import cats.implicits._
import cats.effect.{Concurrent, Effect}
import cats.effect.concurrent.Ref
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model._
import com.amazonaws.services.kinesis.producer.{
  KinesisProducer,
  KinesisProducerConfiguration,
  UserRecordResult
}
import com.amazonaws.auth.{AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain}
import com.google.common.util.concurrent.ListenableFuture
import fs2.aws.kinesis.Producer

import scala.util.control.Exception
import scala.collection.JavaConverters._

object Internal {

  private[aws] trait S3Client[F[_]] {
    private lazy val client = AmazonS3ClientBuilder.defaultClient

    def getObjectContentOrError(getObjectRequest: GetObjectRequest)(
        implicit F: Effect[F]): F[Either[Throwable, InputStream]] =
      F.delay(Exception.nonFatalCatch either client.getObject(getObjectRequest).getObjectContent)

    def getObjectContent(getObjectRequest: GetObjectRequest)(
        implicit F: Effect[F]): F[InputStream] =
      F.delay(client.getObject(getObjectRequest).getObjectContent)

    def initiateMultipartUpload(initiateMultipartUploadRequest: InitiateMultipartUploadRequest)(
        implicit F: Effect[F]): F[InitiateMultipartUploadResult] =
      F.delay(client.initiateMultipartUpload(initiateMultipartUploadRequest))

    def uploadPart(uploadPartRequest: UploadPartRequest)(
        implicit F: Effect[F]): F[UploadPartResult] =
      F.delay(client.uploadPart(uploadPartRequest))

    def completeMultipartUpload(completeMultipartUploadRequest: CompleteMultipartUploadRequest)(
        implicit F: Effect[F]): F[CompleteMultipartUploadResult] =
      F.delay(client.completeMultipartUpload(completeMultipartUploadRequest))

    def s3ObjectSummaries(listObjectsV2Request: ListObjectsV2Request)(
        implicit F: Effect[F]): F[List[S3ObjectSummary]] =
      F.delay(client.listObjectsV2(listObjectsV2Request).getObjectSummaries.asScala.toList)

    def getObject(objectRequest: GetObjectRequest)(implicit F: Effect[F]): F[S3Object] = {
      F.delay(client.getObject(objectRequest))
    }

  }

  private[aws] case class MultiPartUploadInfo(uploadId: String, partETags: List[PartETag])

  class KinesisProducerClient[F[_]] extends Producer[F] {

    val credentials: AWSCredentialsProviderChain = new DefaultAWSCredentialsProviderChain()
    val region: Option[String]                   = None

    private lazy val config: KinesisProducerConfiguration = new KinesisProducerConfiguration()
      .setCredentialsProvider(credentials)

    region.map(r => config.setRegion(r))

    private lazy val client = new KinesisProducer(config)

    override def putData(streamName: String, partitionKey: String, data: ByteBuffer)(
        implicit F: Effect[F]): F[ListenableFuture[UserRecordResult]] =
      F.delay(client.addUserRecord(streamName, partitionKey, data))
  }

  /** Helper flow to group elements of a stream into K substreams.
    * Grows with the number of distinct 'K' selectors
    *
    * Start with an empty Map of keys to queues
    * On element received, invoke the selector function to yield the key denoting which queue this element belongs to
    * If we already have an existing queue for that respective key, append the element lifted in an Option to the queue
    * If a queue for that key does not exist, create a new queue, append it to the queue mapping, and then enqueue the element lifted in an Option
    * For each queue, drain the queue yielding a stream of elements
    * After the stream has been emptied, enqueue a single None to the queue so that the stream halts
    *
    *  @tparam F effect type of the fs2 stream
    *  @param selector partitioning function based on the element
    *  @return a FS2 pipe producing a new sub-stream of elements grouped by the selector
    */
  def groupBy[F[_], A, K](selector: A => F[K])(
      implicit F: Concurrent[F]): Pipe[F, A, (K, Stream[F, A])] = { in =>
    Stream.eval(Ref.of[F, Map[K, Queue[F, Option[A]]]](Map.empty)).flatMap { queueMap =>
      val cleanup = {
        import alleycats.std.all._
        queueMap.get.flatMap(_.traverse_(_.enqueue1(None)))
      }

      (in ++ Stream.eval_(cleanup))
        .evalMap { elem =>
          (selector(elem), queueMap.get).mapN { (key, queues) =>
            queues
              .get(key)
              .fold {
                for {
                  newQ <- Queue.unbounded[F, Option[A]] // Create a new queue
                  _    <- queueMap.modify(queues => (queues + (key -> newQ), queues))
                  _    <- newQ.enqueue1(elem.some) // Enqueue the element lifted into an Option to the new queue
                } yield (key -> newQ.dequeue.unNoneTerminate).some
              }(_.enqueue1(elem.some) as None)
          }.flatten
        }
        .unNone
        .onFinalize(cleanup)
    }
  }
}
