package fs2
package aws
package internal

import cats.effect.Effect
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model._

import scala.util.control.Exception

object Internal {

  private[aws] trait S3Client[F[_]] {
    private val client = AmazonS3ClientBuilder.defaultClient

    def getObjectContent(getObjectRequest: GetObjectRequest)(
        implicit F: Effect[F]): F[Either[Throwable, S3ObjectInputStream]] =
      F.delay(Exception.nonFatalCatch either client.getObject(getObjectRequest).getObjectContent)

    def initiateMultipartUpload(initiateMultipartUploadRequest: InitiateMultipartUploadRequest)(
        implicit F: Effect[F]): F[InitiateMultipartUploadResult] =
      F.delay(client.initiateMultipartUpload(initiateMultipartUploadRequest))

    def uploadPart(uploadPartRequest: UploadPartRequest)(
        implicit F: Effect[F]): F[UploadPartResult] =
      F.delay(client.uploadPart(uploadPartRequest))

    def completeMultipartUpload(completeMultipartUploadRequest: CompleteMultipartUploadRequest)(
        implicit F: Effect[F]): F[CompleteMultipartUploadResult] =
      F.delay(client.completeMultipartUpload(completeMultipartUploadRequest))

  }

  private[aws] case class MultiPartUploadInfo(uploadId: String, partETags: List[PartETag])

}
