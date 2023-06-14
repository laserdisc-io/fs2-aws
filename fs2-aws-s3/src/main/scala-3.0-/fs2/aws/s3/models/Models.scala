package fs2.aws.s3.models

import eu.timepit.refined.W
import eu.timepit.refined.api.{Refined, RefinedTypeOps}
import eu.timepit.refined.numeric.GreaterEqual
import eu.timepit.refined.types.string.NonEmptyString

object Models {

  // Each part must be at least 5 MB in size
  // https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/S3Client.html#uploadPart-software.amazon.awssdk.services.s3.model.UploadPartRequest-software.amazon.awssdk.core.sync.RequestBody-
  type PartSizeMB = Int Refined GreaterEqual[W.`5`.T]
  type ETag       = String

  object PartSizeMB extends RefinedTypeOps.Numeric[PartSizeMB, Int]

  final case class BucketName(value: NonEmptyString)
  final case class FileKey(value: NonEmptyString)
}
