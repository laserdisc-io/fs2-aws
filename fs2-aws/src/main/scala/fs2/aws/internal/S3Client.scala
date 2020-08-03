package fs2.aws.internal

import java.io.InputStream

import cats.effect.Effect
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model._

import scala.jdk.CollectionConverters._
import scala.util.control.Exception

private[aws] object S3Client {
  def apply[F[_]](s3: AmazonS3) = new S3ClientImpl[F](s3)
}

private[aws] class S3ClientImpl[F[_]](c: AmazonS3) extends S3Client[F] {
  override def client: AmazonS3 = c
}

private[aws] trait S3Client[F[_]] {

  def client: AmazonS3

  def getObjectContentOrError(
    getObjectRequest: GetObjectRequest
  )(implicit F: Effect[F]): F[Either[Throwable, InputStream]] =
    F.delay(Exception.nonFatalCatch either client.getObject(getObjectRequest).getObjectContent)

  def getObjectContent(getObjectRequest: GetObjectRequest)(implicit F: Effect[F]): F[InputStream] =
    F.delay(client.getObject(getObjectRequest).getObjectContent)

  def initiateMultipartUpload(
    initiateMultipartUploadRequest: InitiateMultipartUploadRequest
  )(implicit F: Effect[F]): F[InitiateMultipartUploadResult] =
    F.delay(client.initiateMultipartUpload(initiateMultipartUploadRequest))

  def uploadPart(uploadPartRequest: UploadPartRequest)(implicit F: Effect[F]): F[UploadPartResult] =
    F.delay(client.uploadPart(uploadPartRequest))

  def completeMultipartUpload(
    completeMultipartUploadRequest: CompleteMultipartUploadRequest
  )(implicit F: Effect[F]): F[CompleteMultipartUploadResult] =
    F.delay(client.completeMultipartUpload(completeMultipartUploadRequest))

  def s3ObjectSummaries(
    listObjectsV2Request: ListObjectsV2Request
  )(implicit F: Effect[F]): F[List[S3ObjectSummary]] =
    F.delay(client.listObjectsV2(listObjectsV2Request).getObjectSummaries.asScala.toList)

  def getObject(objectRequest: GetObjectRequest)(implicit F: Effect[F]): F[S3Object] =
    F.delay(client.getObject(objectRequest))
}
