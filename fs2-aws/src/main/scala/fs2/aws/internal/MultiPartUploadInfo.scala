package fs2.aws.internal

import software.amazon.awssdk.services.s3.model.Part

private[aws] case class MultiPartUploadInfo(uploadId: String, partETags: List[Part])
