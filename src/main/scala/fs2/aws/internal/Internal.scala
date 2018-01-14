package fs2
package aws
package internal

import com.amazonaws.services.s3.model.{GetObjectRequest, S3ObjectInputStream}

object Internal {
  private[aws] trait S3Client[F[_]] {
    def getObjectContent(getObjectRequest: GetObjectRequest) : F[S3ObjectInputStream]
  }
}
