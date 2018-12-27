package fs2.aws.internal

import com.amazonaws.services.s3.model.PartETag

private[aws] case class MultiPartUploadInfo(uploadId: String, partETags: List[PartETag])
