package fs2.aws.kinesis

import cats.effect.IO
import io.laserdisc.pure.s3.tagless.S3AsyncClientOp
import software.amazon.awssdk.core.async.{AsyncRequestBody, AsyncResponseTransformer}
import software.amazon.awssdk.services.s3.{S3ServiceClientConfiguration, S3Utilities}
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.paginators.*
import software.amazon.awssdk.services.s3.waiters.S3AsyncWaiter

import java.nio.file.Path

//noinspection NotImplementedCode
class S3OpsStub extends S3AsyncClientOp[IO] {
  override def abortMultipartUpload(a: AbortMultipartUploadRequest): IO[AbortMultipartUploadResponse] =
    IO.pure(AbortMultipartUploadResponse.builder().build())

  override def close: IO[Unit] = ???

  override def completeMultipartUpload(a: CompleteMultipartUploadRequest): IO[CompleteMultipartUploadResponse] =
    IO.pure(CompleteMultipartUploadResponse.builder().eTag("dummy").build())

  override def copyObject(a: CopyObjectRequest): IO[CopyObjectResponse] = ???

  override def createBucket(a: CreateBucketRequest): IO[CreateBucketResponse] = ???

  override def createMultipartUpload(a: CreateMultipartUploadRequest): IO[CreateMultipartUploadResponse] =
    IO.pure(CreateMultipartUploadResponse.builder().uploadId("dummy").build())

  override def deleteBucket(a: DeleteBucketRequest): IO[DeleteBucketResponse] = ???

  override def deleteBucketAnalyticsConfiguration(
      a: DeleteBucketAnalyticsConfigurationRequest
  ): IO[DeleteBucketAnalyticsConfigurationResponse] = ???

  override def deleteBucketCors(a: DeleteBucketCorsRequest): IO[DeleteBucketCorsResponse] = ???

  override def deleteBucketEncryption(a: DeleteBucketEncryptionRequest): IO[DeleteBucketEncryptionResponse] = ???

  override def deleteBucketIntelligentTieringConfiguration(
      a: DeleteBucketIntelligentTieringConfigurationRequest
  ): IO[DeleteBucketIntelligentTieringConfigurationResponse] = ???

  override def deleteBucketInventoryConfiguration(
      a: DeleteBucketInventoryConfigurationRequest
  ): IO[DeleteBucketInventoryConfigurationResponse] = ???

  override def deleteBucketLifecycle(a: DeleteBucketLifecycleRequest): IO[DeleteBucketLifecycleResponse] = ???

  override def deleteBucketMetricsConfiguration(
      a: DeleteBucketMetricsConfigurationRequest
  ): IO[DeleteBucketMetricsConfigurationResponse] = ???

  override def deleteBucketOwnershipControls(
      a: DeleteBucketOwnershipControlsRequest
  ): IO[DeleteBucketOwnershipControlsResponse] = ???

  override def deleteBucketPolicy(a: DeleteBucketPolicyRequest): IO[DeleteBucketPolicyResponse] = ???

  override def deleteBucketReplication(a: DeleteBucketReplicationRequest): IO[DeleteBucketReplicationResponse] =
    ???

  override def deleteBucketTagging(a: DeleteBucketTaggingRequest): IO[DeleteBucketTaggingResponse] = ???

  override def deleteBucketWebsite(a: DeleteBucketWebsiteRequest): IO[DeleteBucketWebsiteResponse] = ???

  override def deleteObject(a: DeleteObjectRequest): IO[DeleteObjectResponse] = ???

  override def deleteObjectTagging(a: DeleteObjectTaggingRequest): IO[DeleteObjectTaggingResponse] = ???

  override def deleteObjects(a: DeleteObjectsRequest): IO[DeleteObjectsResponse] = ???

  override def deletePublicAccessBlock(a: DeletePublicAccessBlockRequest): IO[DeletePublicAccessBlockResponse] =
    ???

  override def getBucketAccelerateConfiguration(
      a: GetBucketAccelerateConfigurationRequest
  ): IO[GetBucketAccelerateConfigurationResponse] = ???

  override def getBucketAcl(a: GetBucketAclRequest): IO[GetBucketAclResponse] = ???

  override def getBucketAnalyticsConfiguration(
      a: GetBucketAnalyticsConfigurationRequest
  ): IO[GetBucketAnalyticsConfigurationResponse] = ???

  override def getBucketCors(a: GetBucketCorsRequest): IO[GetBucketCorsResponse] = ???

  override def getBucketEncryption(a: GetBucketEncryptionRequest): IO[GetBucketEncryptionResponse] = ???

  override def getBucketIntelligentTieringConfiguration(
      a: GetBucketIntelligentTieringConfigurationRequest
  ): IO[GetBucketIntelligentTieringConfigurationResponse] = ???

  override def getBucketInventoryConfiguration(
      a: GetBucketInventoryConfigurationRequest
  ): IO[GetBucketInventoryConfigurationResponse] = ???

  override def getBucketLifecycleConfiguration(
      a: GetBucketLifecycleConfigurationRequest
  ): IO[GetBucketLifecycleConfigurationResponse] = ???

  override def getBucketLocation(a: GetBucketLocationRequest): IO[GetBucketLocationResponse] = ???

  override def getBucketLogging(a: GetBucketLoggingRequest): IO[GetBucketLoggingResponse] = ???

  override def getBucketMetricsConfiguration(
      a: GetBucketMetricsConfigurationRequest
  ): IO[GetBucketMetricsConfigurationResponse] = ???

  override def getBucketNotificationConfiguration(
      a: GetBucketNotificationConfigurationRequest
  ): IO[GetBucketNotificationConfigurationResponse] = ???

  override def getBucketOwnershipControls(
      a: GetBucketOwnershipControlsRequest
  ): IO[GetBucketOwnershipControlsResponse] = ???

  override def getBucketPolicy(a: GetBucketPolicyRequest): IO[GetBucketPolicyResponse] = ???

  override def getBucketPolicyStatus(a: GetBucketPolicyStatusRequest): IO[GetBucketPolicyStatusResponse] = ???

  override def getBucketReplication(a: GetBucketReplicationRequest): IO[GetBucketReplicationResponse] = ???

  override def getBucketRequestPayment(a: GetBucketRequestPaymentRequest): IO[GetBucketRequestPaymentResponse] =
    ???

  override def getBucketTagging(a: GetBucketTaggingRequest): IO[GetBucketTaggingResponse] = ???

  override def getBucketVersioning(a: GetBucketVersioningRequest): IO[GetBucketVersioningResponse] = ???

  override def getBucketWebsite(a: GetBucketWebsiteRequest): IO[GetBucketWebsiteResponse] = ???

  override def getObject[ReturnT](
      a: GetObjectRequest,
      b: AsyncResponseTransformer[GetObjectResponse, ReturnT]
  ): IO[ReturnT] = ???

  override def getObject(a: GetObjectRequest, b: Path): IO[GetObjectResponse] = ???

  override def getObjectAcl(a: GetObjectAclRequest): IO[GetObjectAclResponse] = ???

  override def getObjectAttributes(a: GetObjectAttributesRequest): IO[GetObjectAttributesResponse] = ???

  override def getObjectLegalHold(a: GetObjectLegalHoldRequest): IO[GetObjectLegalHoldResponse] = ???

  override def getObjectLockConfiguration(
      a: GetObjectLockConfigurationRequest
  ): IO[GetObjectLockConfigurationResponse] = ???

  override def getObjectRetention(a: GetObjectRetentionRequest): IO[GetObjectRetentionResponse] = ???

  override def getObjectTagging(a: GetObjectTaggingRequest): IO[GetObjectTaggingResponse] = ???

  override def getObjectTorrent[ReturnT](
      a: GetObjectTorrentRequest,
      b: AsyncResponseTransformer[GetObjectTorrentResponse, ReturnT]
  ): IO[ReturnT] = ???

  override def getObjectTorrent(a: GetObjectTorrentRequest, b: Path): IO[GetObjectTorrentResponse] = ???

  override def getPublicAccessBlock(a: GetPublicAccessBlockRequest): IO[GetPublicAccessBlockResponse] = ???

  override def headBucket(a: HeadBucketRequest): IO[HeadBucketResponse] = ???

  override def headObject(a: HeadObjectRequest): IO[HeadObjectResponse] = ???

  override def listBucketAnalyticsConfigurations(
      a: ListBucketAnalyticsConfigurationsRequest
  ): IO[ListBucketAnalyticsConfigurationsResponse] = ???

  override def listBucketIntelligentTieringConfigurations(
      a: ListBucketIntelligentTieringConfigurationsRequest
  ): IO[ListBucketIntelligentTieringConfigurationsResponse] = ???

  override def listBucketInventoryConfigurations(
      a: ListBucketInventoryConfigurationsRequest
  ): IO[ListBucketInventoryConfigurationsResponse] = ???

  override def listBucketMetricsConfigurations(
      a: ListBucketMetricsConfigurationsRequest
  ): IO[ListBucketMetricsConfigurationsResponse] = ???

  override def listBuckets: IO[ListBucketsResponse] = ???

  override def listBuckets(a: ListBucketsRequest): IO[ListBucketsResponse] = ???

  override def listMultipartUploads(a: ListMultipartUploadsRequest): IO[ListMultipartUploadsResponse] = ???

  override def listMultipartUploadsPaginator(a: ListMultipartUploadsRequest): IO[ListMultipartUploadsPublisher] =
    ???

  override def listObjectVersions(a: ListObjectVersionsRequest): IO[ListObjectVersionsResponse] = ???

  override def listObjectVersionsPaginator(a: ListObjectVersionsRequest): IO[ListObjectVersionsPublisher] = ???

  override def listObjects(a: ListObjectsRequest): IO[ListObjectsResponse] = ???

  override def listObjectsV2(a: ListObjectsV2Request): IO[ListObjectsV2Response] = ???

  override def listObjectsV2Paginator(a: ListObjectsV2Request): IO[ListObjectsV2Publisher] = ???

  override def listParts(a: ListPartsRequest): IO[ListPartsResponse] = ???

  override def listPartsPaginator(a: ListPartsRequest): IO[ListPartsPublisher] = ???

  override def putBucketAccelerateConfiguration(
      a: PutBucketAccelerateConfigurationRequest
  ): IO[PutBucketAccelerateConfigurationResponse] = ???

  override def putBucketAcl(a: PutBucketAclRequest): IO[PutBucketAclResponse] = ???

  override def putBucketAnalyticsConfiguration(
      a: PutBucketAnalyticsConfigurationRequest
  ): IO[PutBucketAnalyticsConfigurationResponse] = ???

  override def putBucketCors(a: PutBucketCorsRequest): IO[PutBucketCorsResponse] = ???

  override def putBucketEncryption(a: PutBucketEncryptionRequest): IO[PutBucketEncryptionResponse] = ???

  override def putBucketIntelligentTieringConfiguration(
      a: PutBucketIntelligentTieringConfigurationRequest
  ): IO[PutBucketIntelligentTieringConfigurationResponse] = ???

  override def putBucketInventoryConfiguration(
      a: PutBucketInventoryConfigurationRequest
  ): IO[PutBucketInventoryConfigurationResponse] = ???

  override def putBucketLifecycleConfiguration(
      a: PutBucketLifecycleConfigurationRequest
  ): IO[PutBucketLifecycleConfigurationResponse] = ???

  override def putBucketLogging(a: PutBucketLoggingRequest): IO[PutBucketLoggingResponse] = ???

  override def putBucketMetricsConfiguration(
      a: PutBucketMetricsConfigurationRequest
  ): IO[PutBucketMetricsConfigurationResponse] = ???

  override def putBucketNotificationConfiguration(
      a: PutBucketNotificationConfigurationRequest
  ): IO[PutBucketNotificationConfigurationResponse] = ???

  override def putBucketOwnershipControls(
      a: PutBucketOwnershipControlsRequest
  ): IO[PutBucketOwnershipControlsResponse] = ???

  override def putBucketPolicy(a: PutBucketPolicyRequest): IO[PutBucketPolicyResponse] = ???

  override def putBucketReplication(a: PutBucketReplicationRequest): IO[PutBucketReplicationResponse] = ???

  override def putBucketRequestPayment(a: PutBucketRequestPaymentRequest): IO[PutBucketRequestPaymentResponse] =
    ???

  override def putBucketTagging(a: PutBucketTaggingRequest): IO[PutBucketTaggingResponse] = ???

  override def putBucketVersioning(a: PutBucketVersioningRequest): IO[PutBucketVersioningResponse] = ???

  override def putBucketWebsite(a: PutBucketWebsiteRequest): IO[PutBucketWebsiteResponse] = ???

  override def putObject(a: PutObjectRequest, b: AsyncRequestBody): IO[PutObjectResponse] = ???

  override def putObject(a: PutObjectRequest, b: Path): IO[PutObjectResponse] = ???

  override def putObjectAcl(a: PutObjectAclRequest): IO[PutObjectAclResponse] = ???

  override def putObjectLegalHold(a: PutObjectLegalHoldRequest): IO[PutObjectLegalHoldResponse] = ???

  override def putObjectLockConfiguration(
      a: PutObjectLockConfigurationRequest
  ): IO[PutObjectLockConfigurationResponse] = ???

  override def putObjectRetention(a: PutObjectRetentionRequest): IO[PutObjectRetentionResponse] = ???

  override def putObjectTagging(a: PutObjectTaggingRequest): IO[PutObjectTaggingResponse] = ???

  override def putPublicAccessBlock(a: PutPublicAccessBlockRequest): IO[PutPublicAccessBlockResponse] = ???

  override def restoreObject(a: RestoreObjectRequest): IO[RestoreObjectResponse] = ???

  override def selectObjectContent(
      a: SelectObjectContentRequest,
      b: SelectObjectContentResponseHandler
  ): IO[Void] = ???

  override def serviceName: IO[String] = ???

  override def uploadPart(a: UploadPartRequest, b: AsyncRequestBody): IO[UploadPartResponse] =
    IO.pure(UploadPartResponse.builder().eTag("dummy").build())

  override def uploadPart(a: UploadPartRequest, b: Path): IO[UploadPartResponse] = ???

  override def uploadPartCopy(a: UploadPartCopyRequest): IO[UploadPartCopyResponse] = ???

  override def utilities: IO[S3Utilities] = ???

  override def waiter: IO[S3AsyncWaiter] = ???

  override def writeGetObjectResponse(
      a: WriteGetObjectResponseRequest,
      b: AsyncRequestBody
  ): IO[WriteGetObjectResponseResponse] = ???

  override def writeGetObjectResponse(
      a: WriteGetObjectResponseRequest,
      b: Path
  ): IO[WriteGetObjectResponseResponse] = ???

  override def createSession(a: CreateSessionRequest): IO[CreateSessionResponse] = ???

  override def listDirectoryBuckets(a: ListDirectoryBucketsRequest): IO[ListDirectoryBucketsResponse] = ???

  override def listDirectoryBucketsPaginator(a: ListDirectoryBucketsRequest): IO[ListDirectoryBucketsPublisher] = ???

  override def createBucketMetadataTableConfiguration(
      a: CreateBucketMetadataTableConfigurationRequest
  ): IO[CreateBucketMetadataTableConfigurationResponse] = ???

  override def deleteBucketMetadataTableConfiguration(
      a: DeleteBucketMetadataTableConfigurationRequest
  ): IO[DeleteBucketMetadataTableConfigurationResponse] = ???

  override def getBucketMetadataTableConfiguration(
      a: GetBucketMetadataTableConfigurationRequest
  ): IO[GetBucketMetadataTableConfigurationResponse] = ???

  override def listBucketsPaginator: IO[ListBucketsPublisher] = ???

  override def listBucketsPaginator(a: ListBucketsRequest): IO[ListBucketsPublisher] = ???

  override def createBucketMetadataConfiguration(
      a: CreateBucketMetadataConfigurationRequest
  ): IO[CreateBucketMetadataConfigurationResponse] = ???

  override def deleteBucketMetadataConfiguration(
      a: DeleteBucketMetadataConfigurationRequest
  ): IO[DeleteBucketMetadataConfigurationResponse] = ???

  override def getBucketMetadataConfiguration(
      a: GetBucketMetadataConfigurationRequest
  ): IO[GetBucketMetadataConfigurationResponse] = ???

  override def renameObject(a: RenameObjectRequest): IO[RenameObjectResponse] = ???

  override def updateBucketMetadataInventoryTableConfiguration(
      a: UpdateBucketMetadataInventoryTableConfigurationRequest
  ): IO[UpdateBucketMetadataInventoryTableConfigurationResponse] = ???

  override def updateBucketMetadataJournalTableConfiguration(
      a: UpdateBucketMetadataJournalTableConfigurationRequest
  ): IO[UpdateBucketMetadataJournalTableConfigurationResponse] = ???
}
