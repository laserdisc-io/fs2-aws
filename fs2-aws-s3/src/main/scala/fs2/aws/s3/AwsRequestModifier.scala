package fs2.aws.s3

import software.amazon.awssdk.services.s3.model.{
  AbortMultipartUploadRequest,
  CompleteMultipartUploadRequest,
  CreateMultipartUploadRequest,
  PutObjectRequest,
  UploadPartRequest
}

object AwsRequestModifier {
  trait MultipartUpload {
    def createMultipartUpload(
        b: CreateMultipartUploadRequest.Builder
    ): CreateMultipartUploadRequest.Builder

    def uploadPart(b: UploadPartRequest.Builder): UploadPartRequest.Builder

    def completeMultipartUpload(
        b: CompleteMultipartUploadRequest.Builder
    ): CompleteMultipartUploadRequest.Builder

    def abortMultipartUpload(
        b: AbortMultipartUploadRequest.Builder
    ): AbortMultipartUploadRequest.Builder
  }

  object MultipartUpload {
    def identity: MultipartUpload = new MultipartUpload {

      def createMultipartUpload(
          b: CreateMultipartUploadRequest.Builder
      ): CreateMultipartUploadRequest.Builder = b

      def uploadPart(b: UploadPartRequest.Builder): UploadPartRequest.Builder = b

      def completeMultipartUpload(
          b: CompleteMultipartUploadRequest.Builder
      ): CompleteMultipartUploadRequest.Builder = b

      def abortMultipartUpload(
          b: AbortMultipartUploadRequest.Builder
      ): AbortMultipartUploadRequest.Builder = b
    }
  }

  trait Upload1 {
    def putObject(b: PutObjectRequest.Builder): PutObjectRequest.Builder
  }

  object Upload1 {
    def identity: Upload1 = new Upload1 {
      def putObject(b: PutObjectRequest.Builder): PutObjectRequest.Builder = b
    }
  }
}
