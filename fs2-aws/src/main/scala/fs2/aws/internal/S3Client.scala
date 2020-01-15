package fs2.aws.internal

import java.io.InputStream

import cats.effect.Effect
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.s3.model._

import scala.collection.JavaConverters._
import scala.util.control.Exception

object S3Client {
  def apply[F[_]](amazonS3: AmazonS3): S3Client[F] = new S3Client[F] {
    override lazy val client: AmazonS3 = amazonS3
  }
}

trait S3Client[F[_]] {
  protected lazy val client: AmazonS3 = AmazonS3ClientBuilder.defaultClient

  def getObjectContentOrError(getObjectRequest: GetObjectRequest)(
      implicit F: Effect[F]): F[Either[Throwable, InputStream]] =
    F.delay(Exception.nonFatalCatch either client.getObject(getObjectRequest).getObjectContent)

  def getObjectContent(getObjectRequest: GetObjectRequest)(implicit F: Effect[F]): F[InputStream] =
    F.delay(client.getObject(getObjectRequest).getObjectContent)

  def initiateMultipartUpload(initiateMultipartUploadRequest: InitiateMultipartUploadRequest)(
      implicit F: Effect[F]): F[InitiateMultipartUploadResult] =
    F.delay(client.initiateMultipartUpload(initiateMultipartUploadRequest))

  def uploadPart(uploadPartRequest: UploadPartRequest)(implicit F: Effect[F]): F[UploadPartResult] =
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
