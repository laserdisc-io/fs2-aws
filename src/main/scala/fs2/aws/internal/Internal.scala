package fs2
package aws
package internal

import cats.effect.Effect
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.{GetObjectRequest, S3ObjectInputStream}

object Internal {
  private[aws] trait S3Client[F[_]] {
    private val client = AmazonS3ClientBuilder.defaultClient
    def getObjectContent(getObjectRequest: GetObjectRequest)(implicit F: Effect[F]) : F[S3ObjectInputStream] =
      F.delay(client.getObject(getObjectRequest).getObjectContent)
  }
}
