package io.laserdisc.pure.s3.tagless

// Library imports
import cats.data.Kleisli
import cats.effect.{ Async,  Resource }

import software.amazon.awssdk.services.s3.*
import software.amazon.awssdk.services.s3.model.*

// Types referenced
import java.nio.file.Path
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3Utilities
import software.amazon.awssdk.services.s3.paginators.ListBucketsPublisher
import software.amazon.awssdk.services.s3.paginators.ListDirectoryBucketsPublisher
import software.amazon.awssdk.services.s3.paginators.ListMultipartUploadsPublisher
import software.amazon.awssdk.services.s3.paginators.ListObjectVersionsPublisher
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Publisher
import software.amazon.awssdk.services.s3.paginators.ListPartsPublisher
import software.amazon.awssdk.services.s3.waiters.S3AsyncWaiter


object Interpreter {

  def apply[M[_]](
    implicit am: Async[M]
  ): Interpreter[M] =
    new Interpreter[M] {
      val asyncM = am
    }

}

// Family of interpreters into Kleisli arrows for some monad M.
trait Interpreter[M[_]] { outer =>

  import java.util.concurrent.CompletableFuture

  implicit val asyncM: Async[M]

  lazy val S3AsyncClientInterpreter: S3AsyncClientInterpreter = new S3AsyncClientInterpreter { }
  // Some methods are common to all interpreters and can be overridden to change behavior globally.

  def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(j => asyncM.blocking(f(j)))
  def primitive1[J, A](f: => A): M[A]              = asyncM.blocking(f)

  def eff[J, A](fut: J => CompletableFuture[A]): Kleisli[M, J, A] = Kleisli { j =>
    asyncM.fromCompletableFuture(asyncM.delay(fut(j)))
  }
  def eff1[J, A](fut: => CompletableFuture[A]): M[A] =
    asyncM.fromCompletableFuture(asyncM.delay(fut))

  // Interpreters
  trait S3AsyncClientInterpreter extends S3AsyncClientOp[Kleisli[M, S3AsyncClient, *]] {
  
    // domain-specific operations are implemented in terms of `primitive`
    override def abortMultipartUpload(a: AbortMultipartUploadRequest): Kleisli[M, S3AsyncClient, AbortMultipartUploadResponse] = eff(_.abortMultipartUpload(a)) // B
    override def close : Kleisli[M, S3AsyncClient, Unit] = primitive(_.close) // A
    override def completeMultipartUpload(a: CompleteMultipartUploadRequest): Kleisli[M, S3AsyncClient, CompleteMultipartUploadResponse] = eff(_.completeMultipartUpload(a)) // B
    override def copyObject(a: CopyObjectRequest): Kleisli[M, S3AsyncClient, CopyObjectResponse] = eff(_.copyObject(a)) // B
    override def createBucket(a: CreateBucketRequest): Kleisli[M, S3AsyncClient, CreateBucketResponse] = eff(_.createBucket(a)) // B
    override def createBucketMetadataConfiguration(a: CreateBucketMetadataConfigurationRequest): Kleisli[M, S3AsyncClient, CreateBucketMetadataConfigurationResponse] = eff(_.createBucketMetadataConfiguration(a)) // B
    override def createBucketMetadataTableConfiguration(a: CreateBucketMetadataTableConfigurationRequest): Kleisli[M, S3AsyncClient, CreateBucketMetadataTableConfigurationResponse] = eff(_.createBucketMetadataTableConfiguration(a)) // B
    override def createMultipartUpload(a: CreateMultipartUploadRequest): Kleisli[M, S3AsyncClient, CreateMultipartUploadResponse] = eff(_.createMultipartUpload(a)) // B
    override def createSession(a: CreateSessionRequest): Kleisli[M, S3AsyncClient, CreateSessionResponse] = eff(_.createSession(a)) // B
    override def deleteBucket(a: DeleteBucketRequest): Kleisli[M, S3AsyncClient, DeleteBucketResponse] = eff(_.deleteBucket(a)) // B
    override def deleteBucketAnalyticsConfiguration(a: DeleteBucketAnalyticsConfigurationRequest): Kleisli[M, S3AsyncClient, DeleteBucketAnalyticsConfigurationResponse] = eff(_.deleteBucketAnalyticsConfiguration(a)) // B
    override def deleteBucketCors(a: DeleteBucketCorsRequest): Kleisli[M, S3AsyncClient, DeleteBucketCorsResponse] = eff(_.deleteBucketCors(a)) // B
    override def deleteBucketEncryption(a: DeleteBucketEncryptionRequest): Kleisli[M, S3AsyncClient, DeleteBucketEncryptionResponse] = eff(_.deleteBucketEncryption(a)) // B
    override def deleteBucketIntelligentTieringConfiguration(a: DeleteBucketIntelligentTieringConfigurationRequest): Kleisli[M, S3AsyncClient, DeleteBucketIntelligentTieringConfigurationResponse] = eff(_.deleteBucketIntelligentTieringConfiguration(a)) // B
    override def deleteBucketInventoryConfiguration(a: DeleteBucketInventoryConfigurationRequest): Kleisli[M, S3AsyncClient, DeleteBucketInventoryConfigurationResponse] = eff(_.deleteBucketInventoryConfiguration(a)) // B
    override def deleteBucketLifecycle(a: DeleteBucketLifecycleRequest): Kleisli[M, S3AsyncClient, DeleteBucketLifecycleResponse] = eff(_.deleteBucketLifecycle(a)) // B
    override def deleteBucketMetadataConfiguration(a: DeleteBucketMetadataConfigurationRequest): Kleisli[M, S3AsyncClient, DeleteBucketMetadataConfigurationResponse] = eff(_.deleteBucketMetadataConfiguration(a)) // B
    override def deleteBucketMetadataTableConfiguration(a: DeleteBucketMetadataTableConfigurationRequest): Kleisli[M, S3AsyncClient, DeleteBucketMetadataTableConfigurationResponse] = eff(_.deleteBucketMetadataTableConfiguration(a)) // B
    override def deleteBucketMetricsConfiguration(a: DeleteBucketMetricsConfigurationRequest): Kleisli[M, S3AsyncClient, DeleteBucketMetricsConfigurationResponse] = eff(_.deleteBucketMetricsConfiguration(a)) // B
    override def deleteBucketOwnershipControls(a: DeleteBucketOwnershipControlsRequest): Kleisli[M, S3AsyncClient, DeleteBucketOwnershipControlsResponse] = eff(_.deleteBucketOwnershipControls(a)) // B
    override def deleteBucketPolicy(a: DeleteBucketPolicyRequest): Kleisli[M, S3AsyncClient, DeleteBucketPolicyResponse] = eff(_.deleteBucketPolicy(a)) // B
    override def deleteBucketReplication(a: DeleteBucketReplicationRequest): Kleisli[M, S3AsyncClient, DeleteBucketReplicationResponse] = eff(_.deleteBucketReplication(a)) // B
    override def deleteBucketTagging(a: DeleteBucketTaggingRequest): Kleisli[M, S3AsyncClient, DeleteBucketTaggingResponse] = eff(_.deleteBucketTagging(a)) // B
    override def deleteBucketWebsite(a: DeleteBucketWebsiteRequest): Kleisli[M, S3AsyncClient, DeleteBucketWebsiteResponse] = eff(_.deleteBucketWebsite(a)) // B
    override def deleteObject(a: DeleteObjectRequest): Kleisli[M, S3AsyncClient, DeleteObjectResponse] = eff(_.deleteObject(a)) // B
    override def deleteObjectTagging(a: DeleteObjectTaggingRequest): Kleisli[M, S3AsyncClient, DeleteObjectTaggingResponse] = eff(_.deleteObjectTagging(a)) // B
    override def deleteObjects(a: DeleteObjectsRequest): Kleisli[M, S3AsyncClient, DeleteObjectsResponse] = eff(_.deleteObjects(a)) // B
    override def deletePublicAccessBlock(a: DeletePublicAccessBlockRequest): Kleisli[M, S3AsyncClient, DeletePublicAccessBlockResponse] = eff(_.deletePublicAccessBlock(a)) // B
    override def getBucketAccelerateConfiguration(a: GetBucketAccelerateConfigurationRequest): Kleisli[M, S3AsyncClient, GetBucketAccelerateConfigurationResponse] = eff(_.getBucketAccelerateConfiguration(a)) // B
    override def getBucketAcl(a: GetBucketAclRequest): Kleisli[M, S3AsyncClient, GetBucketAclResponse] = eff(_.getBucketAcl(a)) // B
    override def getBucketAnalyticsConfiguration(a: GetBucketAnalyticsConfigurationRequest): Kleisli[M, S3AsyncClient, GetBucketAnalyticsConfigurationResponse] = eff(_.getBucketAnalyticsConfiguration(a)) // B
    override def getBucketCors(a: GetBucketCorsRequest): Kleisli[M, S3AsyncClient, GetBucketCorsResponse] = eff(_.getBucketCors(a)) // B
    override def getBucketEncryption(a: GetBucketEncryptionRequest): Kleisli[M, S3AsyncClient, GetBucketEncryptionResponse] = eff(_.getBucketEncryption(a)) // B
    override def getBucketIntelligentTieringConfiguration(a: GetBucketIntelligentTieringConfigurationRequest): Kleisli[M, S3AsyncClient, GetBucketIntelligentTieringConfigurationResponse] = eff(_.getBucketIntelligentTieringConfiguration(a)) // B
    override def getBucketInventoryConfiguration(a: GetBucketInventoryConfigurationRequest): Kleisli[M, S3AsyncClient, GetBucketInventoryConfigurationResponse] = eff(_.getBucketInventoryConfiguration(a)) // B
    override def getBucketLifecycleConfiguration(a: GetBucketLifecycleConfigurationRequest): Kleisli[M, S3AsyncClient, GetBucketLifecycleConfigurationResponse] = eff(_.getBucketLifecycleConfiguration(a)) // B
    override def getBucketLocation(a: GetBucketLocationRequest): Kleisli[M, S3AsyncClient, GetBucketLocationResponse] = eff(_.getBucketLocation(a)) // B
    override def getBucketLogging(a: GetBucketLoggingRequest): Kleisli[M, S3AsyncClient, GetBucketLoggingResponse] = eff(_.getBucketLogging(a)) // B
    override def getBucketMetadataConfiguration(a: GetBucketMetadataConfigurationRequest): Kleisli[M, S3AsyncClient, GetBucketMetadataConfigurationResponse] = eff(_.getBucketMetadataConfiguration(a)) // B
    override def getBucketMetadataTableConfiguration(a: GetBucketMetadataTableConfigurationRequest): Kleisli[M, S3AsyncClient, GetBucketMetadataTableConfigurationResponse] = eff(_.getBucketMetadataTableConfiguration(a)) // B
    override def getBucketMetricsConfiguration(a: GetBucketMetricsConfigurationRequest): Kleisli[M, S3AsyncClient, GetBucketMetricsConfigurationResponse] = eff(_.getBucketMetricsConfiguration(a)) // B
    override def getBucketNotificationConfiguration(a: GetBucketNotificationConfigurationRequest): Kleisli[M, S3AsyncClient, GetBucketNotificationConfigurationResponse] = eff(_.getBucketNotificationConfiguration(a)) // B
    override def getBucketOwnershipControls(a: GetBucketOwnershipControlsRequest): Kleisli[M, S3AsyncClient, GetBucketOwnershipControlsResponse] = eff(_.getBucketOwnershipControls(a)) // B
    override def getBucketPolicy(a: GetBucketPolicyRequest): Kleisli[M, S3AsyncClient, GetBucketPolicyResponse] = eff(_.getBucketPolicy(a)) // B
    override def getBucketPolicyStatus(a: GetBucketPolicyStatusRequest): Kleisli[M, S3AsyncClient, GetBucketPolicyStatusResponse] = eff(_.getBucketPolicyStatus(a)) // B
    override def getBucketReplication(a: GetBucketReplicationRequest): Kleisli[M, S3AsyncClient, GetBucketReplicationResponse] = eff(_.getBucketReplication(a)) // B
    override def getBucketRequestPayment(a: GetBucketRequestPaymentRequest): Kleisli[M, S3AsyncClient, GetBucketRequestPaymentResponse] = eff(_.getBucketRequestPayment(a)) // B
    override def getBucketTagging(a: GetBucketTaggingRequest): Kleisli[M, S3AsyncClient, GetBucketTaggingResponse] = eff(_.getBucketTagging(a)) // B
    override def getBucketVersioning(a: GetBucketVersioningRequest): Kleisli[M, S3AsyncClient, GetBucketVersioningResponse] = eff(_.getBucketVersioning(a)) // B
    override def getBucketWebsite(a: GetBucketWebsiteRequest): Kleisli[M, S3AsyncClient, GetBucketWebsiteResponse] = eff(_.getBucketWebsite(a)) // B
    override def getObject[ReturnT](a: GetObjectRequest, b: AsyncResponseTransformer[GetObjectResponse, ReturnT]): Kleisli[M, S3AsyncClient, ReturnT] = eff(_.getObject(a, b)) // B
    override def getObject(a: GetObjectRequest, b: Path): Kleisli[M, S3AsyncClient, GetObjectResponse] = eff(_.getObject(a, b)) // B
    override def getObjectAcl(a: GetObjectAclRequest): Kleisli[M, S3AsyncClient, GetObjectAclResponse] = eff(_.getObjectAcl(a)) // B
    override def getObjectAttributes(a: GetObjectAttributesRequest): Kleisli[M, S3AsyncClient, GetObjectAttributesResponse] = eff(_.getObjectAttributes(a)) // B
    override def getObjectLegalHold(a: GetObjectLegalHoldRequest): Kleisli[M, S3AsyncClient, GetObjectLegalHoldResponse] = eff(_.getObjectLegalHold(a)) // B
    override def getObjectLockConfiguration(a: GetObjectLockConfigurationRequest): Kleisli[M, S3AsyncClient, GetObjectLockConfigurationResponse] = eff(_.getObjectLockConfiguration(a)) // B
    override def getObjectRetention(a: GetObjectRetentionRequest): Kleisli[M, S3AsyncClient, GetObjectRetentionResponse] = eff(_.getObjectRetention(a)) // B
    override def getObjectTagging(a: GetObjectTaggingRequest): Kleisli[M, S3AsyncClient, GetObjectTaggingResponse] = eff(_.getObjectTagging(a)) // B
    override def getObjectTorrent[ReturnT](a: GetObjectTorrentRequest, b: AsyncResponseTransformer[GetObjectTorrentResponse, ReturnT]): Kleisli[M, S3AsyncClient, ReturnT] = eff(_.getObjectTorrent(a, b)) // B
    override def getObjectTorrent(a: GetObjectTorrentRequest, b: Path): Kleisli[M, S3AsyncClient, GetObjectTorrentResponse] = eff(_.getObjectTorrent(a, b)) // B
    override def getPublicAccessBlock(a: GetPublicAccessBlockRequest): Kleisli[M, S3AsyncClient, GetPublicAccessBlockResponse] = eff(_.getPublicAccessBlock(a)) // B
    override def headBucket(a: HeadBucketRequest): Kleisli[M, S3AsyncClient, HeadBucketResponse] = eff(_.headBucket(a)) // B
    override def headObject(a: HeadObjectRequest): Kleisli[M, S3AsyncClient, HeadObjectResponse] = eff(_.headObject(a)) // B
    override def listBucketAnalyticsConfigurations(a: ListBucketAnalyticsConfigurationsRequest): Kleisli[M, S3AsyncClient, ListBucketAnalyticsConfigurationsResponse] = eff(_.listBucketAnalyticsConfigurations(a)) // B
    override def listBucketIntelligentTieringConfigurations(a: ListBucketIntelligentTieringConfigurationsRequest): Kleisli[M, S3AsyncClient, ListBucketIntelligentTieringConfigurationsResponse] = eff(_.listBucketIntelligentTieringConfigurations(a)) // B
    override def listBucketInventoryConfigurations(a: ListBucketInventoryConfigurationsRequest): Kleisli[M, S3AsyncClient, ListBucketInventoryConfigurationsResponse] = eff(_.listBucketInventoryConfigurations(a)) // B
    override def listBucketMetricsConfigurations(a: ListBucketMetricsConfigurationsRequest): Kleisli[M, S3AsyncClient, ListBucketMetricsConfigurationsResponse] = eff(_.listBucketMetricsConfigurations(a)) // B
    override def listBuckets : Kleisli[M, S3AsyncClient, ListBucketsResponse] = eff(_.listBuckets) // A
    override def listBuckets(a: ListBucketsRequest): Kleisli[M, S3AsyncClient, ListBucketsResponse] = eff(_.listBuckets(a)) // B
    override def listBucketsPaginator : Kleisli[M, S3AsyncClient, ListBucketsPublisher] = primitive(_.listBucketsPaginator) // A
    override def listBucketsPaginator(a: ListBucketsRequest): Kleisli[M, S3AsyncClient, ListBucketsPublisher] = primitive(_.listBucketsPaginator(a)) // B
    override def listDirectoryBuckets(a: ListDirectoryBucketsRequest): Kleisli[M, S3AsyncClient, ListDirectoryBucketsResponse] = eff(_.listDirectoryBuckets(a)) // B
    override def listDirectoryBucketsPaginator(a: ListDirectoryBucketsRequest): Kleisli[M, S3AsyncClient, ListDirectoryBucketsPublisher] = primitive(_.listDirectoryBucketsPaginator(a)) // B
    override def listMultipartUploads(a: ListMultipartUploadsRequest): Kleisli[M, S3AsyncClient, ListMultipartUploadsResponse] = eff(_.listMultipartUploads(a)) // B
    override def listMultipartUploadsPaginator(a: ListMultipartUploadsRequest): Kleisli[M, S3AsyncClient, ListMultipartUploadsPublisher] = primitive(_.listMultipartUploadsPaginator(a)) // B
    override def listObjectVersions(a: ListObjectVersionsRequest): Kleisli[M, S3AsyncClient, ListObjectVersionsResponse] = eff(_.listObjectVersions(a)) // B
    override def listObjectVersionsPaginator(a: ListObjectVersionsRequest): Kleisli[M, S3AsyncClient, ListObjectVersionsPublisher] = primitive(_.listObjectVersionsPaginator(a)) // B
    override def listObjects(a: ListObjectsRequest): Kleisli[M, S3AsyncClient, ListObjectsResponse] = eff(_.listObjects(a)) // B
    override def listObjectsV2(a: ListObjectsV2Request): Kleisli[M, S3AsyncClient, ListObjectsV2Response] = eff(_.listObjectsV2(a)) // B
    override def listObjectsV2Paginator(a: ListObjectsV2Request): Kleisli[M, S3AsyncClient, ListObjectsV2Publisher] = primitive(_.listObjectsV2Paginator(a)) // B
    override def listParts(a: ListPartsRequest): Kleisli[M, S3AsyncClient, ListPartsResponse] = eff(_.listParts(a)) // B
    override def listPartsPaginator(a: ListPartsRequest): Kleisli[M, S3AsyncClient, ListPartsPublisher] = primitive(_.listPartsPaginator(a)) // B
    override def putBucketAccelerateConfiguration(a: PutBucketAccelerateConfigurationRequest): Kleisli[M, S3AsyncClient, PutBucketAccelerateConfigurationResponse] = eff(_.putBucketAccelerateConfiguration(a)) // B
    override def putBucketAcl(a: PutBucketAclRequest): Kleisli[M, S3AsyncClient, PutBucketAclResponse] = eff(_.putBucketAcl(a)) // B
    override def putBucketAnalyticsConfiguration(a: PutBucketAnalyticsConfigurationRequest): Kleisli[M, S3AsyncClient, PutBucketAnalyticsConfigurationResponse] = eff(_.putBucketAnalyticsConfiguration(a)) // B
    override def putBucketCors(a: PutBucketCorsRequest): Kleisli[M, S3AsyncClient, PutBucketCorsResponse] = eff(_.putBucketCors(a)) // B
    override def putBucketEncryption(a: PutBucketEncryptionRequest): Kleisli[M, S3AsyncClient, PutBucketEncryptionResponse] = eff(_.putBucketEncryption(a)) // B
    override def putBucketIntelligentTieringConfiguration(a: PutBucketIntelligentTieringConfigurationRequest): Kleisli[M, S3AsyncClient, PutBucketIntelligentTieringConfigurationResponse] = eff(_.putBucketIntelligentTieringConfiguration(a)) // B
    override def putBucketInventoryConfiguration(a: PutBucketInventoryConfigurationRequest): Kleisli[M, S3AsyncClient, PutBucketInventoryConfigurationResponse] = eff(_.putBucketInventoryConfiguration(a)) // B
    override def putBucketLifecycleConfiguration(a: PutBucketLifecycleConfigurationRequest): Kleisli[M, S3AsyncClient, PutBucketLifecycleConfigurationResponse] = eff(_.putBucketLifecycleConfiguration(a)) // B
    override def putBucketLogging(a: PutBucketLoggingRequest): Kleisli[M, S3AsyncClient, PutBucketLoggingResponse] = eff(_.putBucketLogging(a)) // B
    override def putBucketMetricsConfiguration(a: PutBucketMetricsConfigurationRequest): Kleisli[M, S3AsyncClient, PutBucketMetricsConfigurationResponse] = eff(_.putBucketMetricsConfiguration(a)) // B
    override def putBucketNotificationConfiguration(a: PutBucketNotificationConfigurationRequest): Kleisli[M, S3AsyncClient, PutBucketNotificationConfigurationResponse] = eff(_.putBucketNotificationConfiguration(a)) // B
    override def putBucketOwnershipControls(a: PutBucketOwnershipControlsRequest): Kleisli[M, S3AsyncClient, PutBucketOwnershipControlsResponse] = eff(_.putBucketOwnershipControls(a)) // B
    override def putBucketPolicy(a: PutBucketPolicyRequest): Kleisli[M, S3AsyncClient, PutBucketPolicyResponse] = eff(_.putBucketPolicy(a)) // B
    override def putBucketReplication(a: PutBucketReplicationRequest): Kleisli[M, S3AsyncClient, PutBucketReplicationResponse] = eff(_.putBucketReplication(a)) // B
    override def putBucketRequestPayment(a: PutBucketRequestPaymentRequest): Kleisli[M, S3AsyncClient, PutBucketRequestPaymentResponse] = eff(_.putBucketRequestPayment(a)) // B
    override def putBucketTagging(a: PutBucketTaggingRequest): Kleisli[M, S3AsyncClient, PutBucketTaggingResponse] = eff(_.putBucketTagging(a)) // B
    override def putBucketVersioning(a: PutBucketVersioningRequest): Kleisli[M, S3AsyncClient, PutBucketVersioningResponse] = eff(_.putBucketVersioning(a)) // B
    override def putBucketWebsite(a: PutBucketWebsiteRequest): Kleisli[M, S3AsyncClient, PutBucketWebsiteResponse] = eff(_.putBucketWebsite(a)) // B
    override def putObject(a: PutObjectRequest, b: AsyncRequestBody): Kleisli[M, S3AsyncClient, PutObjectResponse] = eff(_.putObject(a, b)) // B
    override def putObject(a: PutObjectRequest, b: Path): Kleisli[M, S3AsyncClient, PutObjectResponse] = eff(_.putObject(a, b)) // B
    override def putObjectAcl(a: PutObjectAclRequest): Kleisli[M, S3AsyncClient, PutObjectAclResponse] = eff(_.putObjectAcl(a)) // B
    override def putObjectLegalHold(a: PutObjectLegalHoldRequest): Kleisli[M, S3AsyncClient, PutObjectLegalHoldResponse] = eff(_.putObjectLegalHold(a)) // B
    override def putObjectLockConfiguration(a: PutObjectLockConfigurationRequest): Kleisli[M, S3AsyncClient, PutObjectLockConfigurationResponse] = eff(_.putObjectLockConfiguration(a)) // B
    override def putObjectRetention(a: PutObjectRetentionRequest): Kleisli[M, S3AsyncClient, PutObjectRetentionResponse] = eff(_.putObjectRetention(a)) // B
    override def putObjectTagging(a: PutObjectTaggingRequest): Kleisli[M, S3AsyncClient, PutObjectTaggingResponse] = eff(_.putObjectTagging(a)) // B
    override def putPublicAccessBlock(a: PutPublicAccessBlockRequest): Kleisli[M, S3AsyncClient, PutPublicAccessBlockResponse] = eff(_.putPublicAccessBlock(a)) // B
    override def renameObject(a: RenameObjectRequest): Kleisli[M, S3AsyncClient, RenameObjectResponse] = eff(_.renameObject(a)) // B
    override def restoreObject(a: RestoreObjectRequest): Kleisli[M, S3AsyncClient, RestoreObjectResponse] = eff(_.restoreObject(a)) // B
    override def selectObjectContent(a: SelectObjectContentRequest, b: SelectObjectContentResponseHandler): Kleisli[M, S3AsyncClient, Void] = eff(_.selectObjectContent(a, b)) // B
    override def serviceName : Kleisli[M, S3AsyncClient, String] = primitive(_.serviceName) // A
    override def updateBucketMetadataInventoryTableConfiguration(a: UpdateBucketMetadataInventoryTableConfigurationRequest): Kleisli[M, S3AsyncClient, UpdateBucketMetadataInventoryTableConfigurationResponse] = eff(_.updateBucketMetadataInventoryTableConfiguration(a)) // B
    override def updateBucketMetadataJournalTableConfiguration(a: UpdateBucketMetadataJournalTableConfigurationRequest): Kleisli[M, S3AsyncClient, UpdateBucketMetadataJournalTableConfigurationResponse] = eff(_.updateBucketMetadataJournalTableConfiguration(a)) // B
    override def uploadPart(a: UploadPartRequest, b: AsyncRequestBody): Kleisli[M, S3AsyncClient, UploadPartResponse] = eff(_.uploadPart(a, b)) // B
    override def uploadPart(a: UploadPartRequest, b: Path): Kleisli[M, S3AsyncClient, UploadPartResponse] = eff(_.uploadPart(a, b)) // B
    override def uploadPartCopy(a: UploadPartCopyRequest): Kleisli[M, S3AsyncClient, UploadPartCopyResponse] = eff(_.uploadPartCopy(a)) // B
    override def utilities : Kleisli[M, S3AsyncClient, S3Utilities] = primitive(_.utilities) // A
    override def waiter : Kleisli[M, S3AsyncClient, S3AsyncWaiter] = primitive(_.waiter) // A
    override def writeGetObjectResponse(a: WriteGetObjectResponseRequest, b: AsyncRequestBody): Kleisli[M, S3AsyncClient, WriteGetObjectResponseResponse] = eff(_.writeGetObjectResponse(a, b)) // B
    override def writeGetObjectResponse(a: WriteGetObjectResponseRequest, b: Path): Kleisli[M, S3AsyncClient, WriteGetObjectResponseResponse] = eff(_.writeGetObjectResponse(a, b)) // B
  
  
    def lens[E](f: E => S3AsyncClient): S3AsyncClientOp[Kleisli[M, E, *]] =
      new S3AsyncClientOp[Kleisli[M, E, *]] {
      override def abortMultipartUpload(a: AbortMultipartUploadRequest) = Kleisli(e => eff1(f(e).abortMultipartUpload(a)))
      override def close = Kleisli(e => primitive1(f(e).close))
      override def completeMultipartUpload(a: CompleteMultipartUploadRequest) = Kleisli(e => eff1(f(e).completeMultipartUpload(a)))
      override def copyObject(a: CopyObjectRequest) = Kleisli(e => eff1(f(e).copyObject(a)))
      override def createBucket(a: CreateBucketRequest) = Kleisli(e => eff1(f(e).createBucket(a)))
      override def createBucketMetadataConfiguration(a: CreateBucketMetadataConfigurationRequest) = Kleisli(e => eff1(f(e).createBucketMetadataConfiguration(a)))
      override def createBucketMetadataTableConfiguration(a: CreateBucketMetadataTableConfigurationRequest) = Kleisli(e => eff1(f(e).createBucketMetadataTableConfiguration(a)))
      override def createMultipartUpload(a: CreateMultipartUploadRequest) = Kleisli(e => eff1(f(e).createMultipartUpload(a)))
      override def createSession(a: CreateSessionRequest) = Kleisli(e => eff1(f(e).createSession(a)))
      override def deleteBucket(a: DeleteBucketRequest) = Kleisli(e => eff1(f(e).deleteBucket(a)))
      override def deleteBucketAnalyticsConfiguration(a: DeleteBucketAnalyticsConfigurationRequest) = Kleisli(e => eff1(f(e).deleteBucketAnalyticsConfiguration(a)))
      override def deleteBucketCors(a: DeleteBucketCorsRequest) = Kleisli(e => eff1(f(e).deleteBucketCors(a)))
      override def deleteBucketEncryption(a: DeleteBucketEncryptionRequest) = Kleisli(e => eff1(f(e).deleteBucketEncryption(a)))
      override def deleteBucketIntelligentTieringConfiguration(a: DeleteBucketIntelligentTieringConfigurationRequest) = Kleisli(e => eff1(f(e).deleteBucketIntelligentTieringConfiguration(a)))
      override def deleteBucketInventoryConfiguration(a: DeleteBucketInventoryConfigurationRequest) = Kleisli(e => eff1(f(e).deleteBucketInventoryConfiguration(a)))
      override def deleteBucketLifecycle(a: DeleteBucketLifecycleRequest) = Kleisli(e => eff1(f(e).deleteBucketLifecycle(a)))
      override def deleteBucketMetadataConfiguration(a: DeleteBucketMetadataConfigurationRequest) = Kleisli(e => eff1(f(e).deleteBucketMetadataConfiguration(a)))
      override def deleteBucketMetadataTableConfiguration(a: DeleteBucketMetadataTableConfigurationRequest) = Kleisli(e => eff1(f(e).deleteBucketMetadataTableConfiguration(a)))
      override def deleteBucketMetricsConfiguration(a: DeleteBucketMetricsConfigurationRequest) = Kleisli(e => eff1(f(e).deleteBucketMetricsConfiguration(a)))
      override def deleteBucketOwnershipControls(a: DeleteBucketOwnershipControlsRequest) = Kleisli(e => eff1(f(e).deleteBucketOwnershipControls(a)))
      override def deleteBucketPolicy(a: DeleteBucketPolicyRequest) = Kleisli(e => eff1(f(e).deleteBucketPolicy(a)))
      override def deleteBucketReplication(a: DeleteBucketReplicationRequest) = Kleisli(e => eff1(f(e).deleteBucketReplication(a)))
      override def deleteBucketTagging(a: DeleteBucketTaggingRequest) = Kleisli(e => eff1(f(e).deleteBucketTagging(a)))
      override def deleteBucketWebsite(a: DeleteBucketWebsiteRequest) = Kleisli(e => eff1(f(e).deleteBucketWebsite(a)))
      override def deleteObject(a: DeleteObjectRequest) = Kleisli(e => eff1(f(e).deleteObject(a)))
      override def deleteObjectTagging(a: DeleteObjectTaggingRequest) = Kleisli(e => eff1(f(e).deleteObjectTagging(a)))
      override def deleteObjects(a: DeleteObjectsRequest) = Kleisli(e => eff1(f(e).deleteObjects(a)))
      override def deletePublicAccessBlock(a: DeletePublicAccessBlockRequest) = Kleisli(e => eff1(f(e).deletePublicAccessBlock(a)))
      override def getBucketAccelerateConfiguration(a: GetBucketAccelerateConfigurationRequest) = Kleisli(e => eff1(f(e).getBucketAccelerateConfiguration(a)))
      override def getBucketAcl(a: GetBucketAclRequest) = Kleisli(e => eff1(f(e).getBucketAcl(a)))
      override def getBucketAnalyticsConfiguration(a: GetBucketAnalyticsConfigurationRequest) = Kleisli(e => eff1(f(e).getBucketAnalyticsConfiguration(a)))
      override def getBucketCors(a: GetBucketCorsRequest) = Kleisli(e => eff1(f(e).getBucketCors(a)))
      override def getBucketEncryption(a: GetBucketEncryptionRequest) = Kleisli(e => eff1(f(e).getBucketEncryption(a)))
      override def getBucketIntelligentTieringConfiguration(a: GetBucketIntelligentTieringConfigurationRequest) = Kleisli(e => eff1(f(e).getBucketIntelligentTieringConfiguration(a)))
      override def getBucketInventoryConfiguration(a: GetBucketInventoryConfigurationRequest) = Kleisli(e => eff1(f(e).getBucketInventoryConfiguration(a)))
      override def getBucketLifecycleConfiguration(a: GetBucketLifecycleConfigurationRequest) = Kleisli(e => eff1(f(e).getBucketLifecycleConfiguration(a)))
      override def getBucketLocation(a: GetBucketLocationRequest) = Kleisli(e => eff1(f(e).getBucketLocation(a)))
      override def getBucketLogging(a: GetBucketLoggingRequest) = Kleisli(e => eff1(f(e).getBucketLogging(a)))
      override def getBucketMetadataConfiguration(a: GetBucketMetadataConfigurationRequest) = Kleisli(e => eff1(f(e).getBucketMetadataConfiguration(a)))
      override def getBucketMetadataTableConfiguration(a: GetBucketMetadataTableConfigurationRequest) = Kleisli(e => eff1(f(e).getBucketMetadataTableConfiguration(a)))
      override def getBucketMetricsConfiguration(a: GetBucketMetricsConfigurationRequest) = Kleisli(e => eff1(f(e).getBucketMetricsConfiguration(a)))
      override def getBucketNotificationConfiguration(a: GetBucketNotificationConfigurationRequest) = Kleisli(e => eff1(f(e).getBucketNotificationConfiguration(a)))
      override def getBucketOwnershipControls(a: GetBucketOwnershipControlsRequest) = Kleisli(e => eff1(f(e).getBucketOwnershipControls(a)))
      override def getBucketPolicy(a: GetBucketPolicyRequest) = Kleisli(e => eff1(f(e).getBucketPolicy(a)))
      override def getBucketPolicyStatus(a: GetBucketPolicyStatusRequest) = Kleisli(e => eff1(f(e).getBucketPolicyStatus(a)))
      override def getBucketReplication(a: GetBucketReplicationRequest) = Kleisli(e => eff1(f(e).getBucketReplication(a)))
      override def getBucketRequestPayment(a: GetBucketRequestPaymentRequest) = Kleisli(e => eff1(f(e).getBucketRequestPayment(a)))
      override def getBucketTagging(a: GetBucketTaggingRequest) = Kleisli(e => eff1(f(e).getBucketTagging(a)))
      override def getBucketVersioning(a: GetBucketVersioningRequest) = Kleisli(e => eff1(f(e).getBucketVersioning(a)))
      override def getBucketWebsite(a: GetBucketWebsiteRequest) = Kleisli(e => eff1(f(e).getBucketWebsite(a)))
      override def getObject[ReturnT](a: GetObjectRequest, b: AsyncResponseTransformer[GetObjectResponse, ReturnT]) = Kleisli(e => eff1(f(e).getObject(a, b)))
      override def getObject(a: GetObjectRequest, b: Path) = Kleisli(e => eff1(f(e).getObject(a, b)))
      override def getObjectAcl(a: GetObjectAclRequest) = Kleisli(e => eff1(f(e).getObjectAcl(a)))
      override def getObjectAttributes(a: GetObjectAttributesRequest) = Kleisli(e => eff1(f(e).getObjectAttributes(a)))
      override def getObjectLegalHold(a: GetObjectLegalHoldRequest) = Kleisli(e => eff1(f(e).getObjectLegalHold(a)))
      override def getObjectLockConfiguration(a: GetObjectLockConfigurationRequest) = Kleisli(e => eff1(f(e).getObjectLockConfiguration(a)))
      override def getObjectRetention(a: GetObjectRetentionRequest) = Kleisli(e => eff1(f(e).getObjectRetention(a)))
      override def getObjectTagging(a: GetObjectTaggingRequest) = Kleisli(e => eff1(f(e).getObjectTagging(a)))
      override def getObjectTorrent[ReturnT](a: GetObjectTorrentRequest, b: AsyncResponseTransformer[GetObjectTorrentResponse, ReturnT]) = Kleisli(e => eff1(f(e).getObjectTorrent(a, b)))
      override def getObjectTorrent(a: GetObjectTorrentRequest, b: Path) = Kleisli(e => eff1(f(e).getObjectTorrent(a, b)))
      override def getPublicAccessBlock(a: GetPublicAccessBlockRequest) = Kleisli(e => eff1(f(e).getPublicAccessBlock(a)))
      override def headBucket(a: HeadBucketRequest) = Kleisli(e => eff1(f(e).headBucket(a)))
      override def headObject(a: HeadObjectRequest) = Kleisli(e => eff1(f(e).headObject(a)))
      override def listBucketAnalyticsConfigurations(a: ListBucketAnalyticsConfigurationsRequest) = Kleisli(e => eff1(f(e).listBucketAnalyticsConfigurations(a)))
      override def listBucketIntelligentTieringConfigurations(a: ListBucketIntelligentTieringConfigurationsRequest) = Kleisli(e => eff1(f(e).listBucketIntelligentTieringConfigurations(a)))
      override def listBucketInventoryConfigurations(a: ListBucketInventoryConfigurationsRequest) = Kleisli(e => eff1(f(e).listBucketInventoryConfigurations(a)))
      override def listBucketMetricsConfigurations(a: ListBucketMetricsConfigurationsRequest) = Kleisli(e => eff1(f(e).listBucketMetricsConfigurations(a)))
      override def listBuckets = Kleisli(e => eff1(f(e).listBuckets))
      override def listBuckets(a: ListBucketsRequest) = Kleisli(e => eff1(f(e).listBuckets(a)))
      override def listBucketsPaginator = Kleisli(e => primitive1(f(e).listBucketsPaginator))
      override def listBucketsPaginator(a: ListBucketsRequest) = Kleisli(e => primitive1(f(e).listBucketsPaginator(a)))
      override def listDirectoryBuckets(a: ListDirectoryBucketsRequest) = Kleisli(e => eff1(f(e).listDirectoryBuckets(a)))
      override def listDirectoryBucketsPaginator(a: ListDirectoryBucketsRequest) = Kleisli(e => primitive1(f(e).listDirectoryBucketsPaginator(a)))
      override def listMultipartUploads(a: ListMultipartUploadsRequest) = Kleisli(e => eff1(f(e).listMultipartUploads(a)))
      override def listMultipartUploadsPaginator(a: ListMultipartUploadsRequest) = Kleisli(e => primitive1(f(e).listMultipartUploadsPaginator(a)))
      override def listObjectVersions(a: ListObjectVersionsRequest) = Kleisli(e => eff1(f(e).listObjectVersions(a)))
      override def listObjectVersionsPaginator(a: ListObjectVersionsRequest) = Kleisli(e => primitive1(f(e).listObjectVersionsPaginator(a)))
      override def listObjects(a: ListObjectsRequest) = Kleisli(e => eff1(f(e).listObjects(a)))
      override def listObjectsV2(a: ListObjectsV2Request) = Kleisli(e => eff1(f(e).listObjectsV2(a)))
      override def listObjectsV2Paginator(a: ListObjectsV2Request) = Kleisli(e => primitive1(f(e).listObjectsV2Paginator(a)))
      override def listParts(a: ListPartsRequest) = Kleisli(e => eff1(f(e).listParts(a)))
      override def listPartsPaginator(a: ListPartsRequest) = Kleisli(e => primitive1(f(e).listPartsPaginator(a)))
      override def putBucketAccelerateConfiguration(a: PutBucketAccelerateConfigurationRequest) = Kleisli(e => eff1(f(e).putBucketAccelerateConfiguration(a)))
      override def putBucketAcl(a: PutBucketAclRequest) = Kleisli(e => eff1(f(e).putBucketAcl(a)))
      override def putBucketAnalyticsConfiguration(a: PutBucketAnalyticsConfigurationRequest) = Kleisli(e => eff1(f(e).putBucketAnalyticsConfiguration(a)))
      override def putBucketCors(a: PutBucketCorsRequest) = Kleisli(e => eff1(f(e).putBucketCors(a)))
      override def putBucketEncryption(a: PutBucketEncryptionRequest) = Kleisli(e => eff1(f(e).putBucketEncryption(a)))
      override def putBucketIntelligentTieringConfiguration(a: PutBucketIntelligentTieringConfigurationRequest) = Kleisli(e => eff1(f(e).putBucketIntelligentTieringConfiguration(a)))
      override def putBucketInventoryConfiguration(a: PutBucketInventoryConfigurationRequest) = Kleisli(e => eff1(f(e).putBucketInventoryConfiguration(a)))
      override def putBucketLifecycleConfiguration(a: PutBucketLifecycleConfigurationRequest) = Kleisli(e => eff1(f(e).putBucketLifecycleConfiguration(a)))
      override def putBucketLogging(a: PutBucketLoggingRequest) = Kleisli(e => eff1(f(e).putBucketLogging(a)))
      override def putBucketMetricsConfiguration(a: PutBucketMetricsConfigurationRequest) = Kleisli(e => eff1(f(e).putBucketMetricsConfiguration(a)))
      override def putBucketNotificationConfiguration(a: PutBucketNotificationConfigurationRequest) = Kleisli(e => eff1(f(e).putBucketNotificationConfiguration(a)))
      override def putBucketOwnershipControls(a: PutBucketOwnershipControlsRequest) = Kleisli(e => eff1(f(e).putBucketOwnershipControls(a)))
      override def putBucketPolicy(a: PutBucketPolicyRequest) = Kleisli(e => eff1(f(e).putBucketPolicy(a)))
      override def putBucketReplication(a: PutBucketReplicationRequest) = Kleisli(e => eff1(f(e).putBucketReplication(a)))
      override def putBucketRequestPayment(a: PutBucketRequestPaymentRequest) = Kleisli(e => eff1(f(e).putBucketRequestPayment(a)))
      override def putBucketTagging(a: PutBucketTaggingRequest) = Kleisli(e => eff1(f(e).putBucketTagging(a)))
      override def putBucketVersioning(a: PutBucketVersioningRequest) = Kleisli(e => eff1(f(e).putBucketVersioning(a)))
      override def putBucketWebsite(a: PutBucketWebsiteRequest) = Kleisli(e => eff1(f(e).putBucketWebsite(a)))
      override def putObject(a: PutObjectRequest, b: AsyncRequestBody) = Kleisli(e => eff1(f(e).putObject(a, b)))
      override def putObject(a: PutObjectRequest, b: Path) = Kleisli(e => eff1(f(e).putObject(a, b)))
      override def putObjectAcl(a: PutObjectAclRequest) = Kleisli(e => eff1(f(e).putObjectAcl(a)))
      override def putObjectLegalHold(a: PutObjectLegalHoldRequest) = Kleisli(e => eff1(f(e).putObjectLegalHold(a)))
      override def putObjectLockConfiguration(a: PutObjectLockConfigurationRequest) = Kleisli(e => eff1(f(e).putObjectLockConfiguration(a)))
      override def putObjectRetention(a: PutObjectRetentionRequest) = Kleisli(e => eff1(f(e).putObjectRetention(a)))
      override def putObjectTagging(a: PutObjectTaggingRequest) = Kleisli(e => eff1(f(e).putObjectTagging(a)))
      override def putPublicAccessBlock(a: PutPublicAccessBlockRequest) = Kleisli(e => eff1(f(e).putPublicAccessBlock(a)))
      override def renameObject(a: RenameObjectRequest) = Kleisli(e => eff1(f(e).renameObject(a)))
      override def restoreObject(a: RestoreObjectRequest) = Kleisli(e => eff1(f(e).restoreObject(a)))
      override def selectObjectContent(a: SelectObjectContentRequest, b: SelectObjectContentResponseHandler) = Kleisli(e => eff1(f(e).selectObjectContent(a, b)))
      override def serviceName = Kleisli(e => primitive1(f(e).serviceName))
      override def updateBucketMetadataInventoryTableConfiguration(a: UpdateBucketMetadataInventoryTableConfigurationRequest) = Kleisli(e => eff1(f(e).updateBucketMetadataInventoryTableConfiguration(a)))
      override def updateBucketMetadataJournalTableConfiguration(a: UpdateBucketMetadataJournalTableConfigurationRequest) = Kleisli(e => eff1(f(e).updateBucketMetadataJournalTableConfiguration(a)))
      override def uploadPart(a: UploadPartRequest, b: AsyncRequestBody) = Kleisli(e => eff1(f(e).uploadPart(a, b)))
      override def uploadPart(a: UploadPartRequest, b: Path) = Kleisli(e => eff1(f(e).uploadPart(a, b)))
      override def uploadPartCopy(a: UploadPartCopyRequest) = Kleisli(e => eff1(f(e).uploadPartCopy(a)))
      override def utilities = Kleisli(e => primitive1(f(e).utilities))
      override def waiter = Kleisli(e => primitive1(f(e).waiter))
      override def writeGetObjectResponse(a: WriteGetObjectResponseRequest, b: AsyncRequestBody) = Kleisli(e => eff1(f(e).writeGetObjectResponse(a, b)))
      override def writeGetObjectResponse(a: WriteGetObjectResponseRequest, b: Path) = Kleisli(e => eff1(f(e).writeGetObjectResponse(a, b)))
  
      }
    }

  // end interpreters

  def S3AsyncClientResource(builder : S3AsyncClientBuilder) : Resource[M, S3AsyncClient] = Resource.fromAutoCloseable(asyncM.delay(builder.build()))
  def S3AsyncClientOpResource(builder: S3AsyncClientBuilder) = S3AsyncClientResource(builder).map(create)

  def create(client : S3AsyncClient) : S3AsyncClientOp[M] = new S3AsyncClientOp[M] {

    // domain-specific operations are implemented in terms of `primitive`
    override def abortMultipartUpload(a: AbortMultipartUploadRequest) = eff1(client.abortMultipartUpload(a))
    override def close = primitive1(client.close)
    override def completeMultipartUpload(a: CompleteMultipartUploadRequest) = eff1(client.completeMultipartUpload(a))
    override def copyObject(a: CopyObjectRequest) = eff1(client.copyObject(a))
    override def createBucket(a: CreateBucketRequest) = eff1(client.createBucket(a))
    override def createBucketMetadataConfiguration(a: CreateBucketMetadataConfigurationRequest) = eff1(client.createBucketMetadataConfiguration(a))
    override def createBucketMetadataTableConfiguration(a: CreateBucketMetadataTableConfigurationRequest) = eff1(client.createBucketMetadataTableConfiguration(a))
    override def createMultipartUpload(a: CreateMultipartUploadRequest) = eff1(client.createMultipartUpload(a))
    override def createSession(a: CreateSessionRequest) = eff1(client.createSession(a))
    override def deleteBucket(a: DeleteBucketRequest) = eff1(client.deleteBucket(a))
    override def deleteBucketAnalyticsConfiguration(a: DeleteBucketAnalyticsConfigurationRequest) = eff1(client.deleteBucketAnalyticsConfiguration(a))
    override def deleteBucketCors(a: DeleteBucketCorsRequest) = eff1(client.deleteBucketCors(a))
    override def deleteBucketEncryption(a: DeleteBucketEncryptionRequest) = eff1(client.deleteBucketEncryption(a))
    override def deleteBucketIntelligentTieringConfiguration(a: DeleteBucketIntelligentTieringConfigurationRequest) = eff1(client.deleteBucketIntelligentTieringConfiguration(a))
    override def deleteBucketInventoryConfiguration(a: DeleteBucketInventoryConfigurationRequest) = eff1(client.deleteBucketInventoryConfiguration(a))
    override def deleteBucketLifecycle(a: DeleteBucketLifecycleRequest) = eff1(client.deleteBucketLifecycle(a))
    override def deleteBucketMetadataConfiguration(a: DeleteBucketMetadataConfigurationRequest) = eff1(client.deleteBucketMetadataConfiguration(a))
    override def deleteBucketMetadataTableConfiguration(a: DeleteBucketMetadataTableConfigurationRequest) = eff1(client.deleteBucketMetadataTableConfiguration(a))
    override def deleteBucketMetricsConfiguration(a: DeleteBucketMetricsConfigurationRequest) = eff1(client.deleteBucketMetricsConfiguration(a))
    override def deleteBucketOwnershipControls(a: DeleteBucketOwnershipControlsRequest) = eff1(client.deleteBucketOwnershipControls(a))
    override def deleteBucketPolicy(a: DeleteBucketPolicyRequest) = eff1(client.deleteBucketPolicy(a))
    override def deleteBucketReplication(a: DeleteBucketReplicationRequest) = eff1(client.deleteBucketReplication(a))
    override def deleteBucketTagging(a: DeleteBucketTaggingRequest) = eff1(client.deleteBucketTagging(a))
    override def deleteBucketWebsite(a: DeleteBucketWebsiteRequest) = eff1(client.deleteBucketWebsite(a))
    override def deleteObject(a: DeleteObjectRequest) = eff1(client.deleteObject(a))
    override def deleteObjectTagging(a: DeleteObjectTaggingRequest) = eff1(client.deleteObjectTagging(a))
    override def deleteObjects(a: DeleteObjectsRequest) = eff1(client.deleteObjects(a))
    override def deletePublicAccessBlock(a: DeletePublicAccessBlockRequest) = eff1(client.deletePublicAccessBlock(a))
    override def getBucketAccelerateConfiguration(a: GetBucketAccelerateConfigurationRequest) = eff1(client.getBucketAccelerateConfiguration(a))
    override def getBucketAcl(a: GetBucketAclRequest) = eff1(client.getBucketAcl(a))
    override def getBucketAnalyticsConfiguration(a: GetBucketAnalyticsConfigurationRequest) = eff1(client.getBucketAnalyticsConfiguration(a))
    override def getBucketCors(a: GetBucketCorsRequest) = eff1(client.getBucketCors(a))
    override def getBucketEncryption(a: GetBucketEncryptionRequest) = eff1(client.getBucketEncryption(a))
    override def getBucketIntelligentTieringConfiguration(a: GetBucketIntelligentTieringConfigurationRequest) = eff1(client.getBucketIntelligentTieringConfiguration(a))
    override def getBucketInventoryConfiguration(a: GetBucketInventoryConfigurationRequest) = eff1(client.getBucketInventoryConfiguration(a))
    override def getBucketLifecycleConfiguration(a: GetBucketLifecycleConfigurationRequest) = eff1(client.getBucketLifecycleConfiguration(a))
    override def getBucketLocation(a: GetBucketLocationRequest) = eff1(client.getBucketLocation(a))
    override def getBucketLogging(a: GetBucketLoggingRequest) = eff1(client.getBucketLogging(a))
    override def getBucketMetadataConfiguration(a: GetBucketMetadataConfigurationRequest) = eff1(client.getBucketMetadataConfiguration(a))
    override def getBucketMetadataTableConfiguration(a: GetBucketMetadataTableConfigurationRequest) = eff1(client.getBucketMetadataTableConfiguration(a))
    override def getBucketMetricsConfiguration(a: GetBucketMetricsConfigurationRequest) = eff1(client.getBucketMetricsConfiguration(a))
    override def getBucketNotificationConfiguration(a: GetBucketNotificationConfigurationRequest) = eff1(client.getBucketNotificationConfiguration(a))
    override def getBucketOwnershipControls(a: GetBucketOwnershipControlsRequest) = eff1(client.getBucketOwnershipControls(a))
    override def getBucketPolicy(a: GetBucketPolicyRequest) = eff1(client.getBucketPolicy(a))
    override def getBucketPolicyStatus(a: GetBucketPolicyStatusRequest) = eff1(client.getBucketPolicyStatus(a))
    override def getBucketReplication(a: GetBucketReplicationRequest) = eff1(client.getBucketReplication(a))
    override def getBucketRequestPayment(a: GetBucketRequestPaymentRequest) = eff1(client.getBucketRequestPayment(a))
    override def getBucketTagging(a: GetBucketTaggingRequest) = eff1(client.getBucketTagging(a))
    override def getBucketVersioning(a: GetBucketVersioningRequest) = eff1(client.getBucketVersioning(a))
    override def getBucketWebsite(a: GetBucketWebsiteRequest) = eff1(client.getBucketWebsite(a))
    override def getObject[ReturnT](a: GetObjectRequest, b: AsyncResponseTransformer[GetObjectResponse, ReturnT]) = eff1(client.getObject(a, b))
    override def getObject(a: GetObjectRequest, b: Path) = eff1(client.getObject(a, b))
    override def getObjectAcl(a: GetObjectAclRequest) = eff1(client.getObjectAcl(a))
    override def getObjectAttributes(a: GetObjectAttributesRequest) = eff1(client.getObjectAttributes(a))
    override def getObjectLegalHold(a: GetObjectLegalHoldRequest) = eff1(client.getObjectLegalHold(a))
    override def getObjectLockConfiguration(a: GetObjectLockConfigurationRequest) = eff1(client.getObjectLockConfiguration(a))
    override def getObjectRetention(a: GetObjectRetentionRequest) = eff1(client.getObjectRetention(a))
    override def getObjectTagging(a: GetObjectTaggingRequest) = eff1(client.getObjectTagging(a))
    override def getObjectTorrent[ReturnT](a: GetObjectTorrentRequest, b: AsyncResponseTransformer[GetObjectTorrentResponse, ReturnT]) = eff1(client.getObjectTorrent(a, b))
    override def getObjectTorrent(a: GetObjectTorrentRequest, b: Path) = eff1(client.getObjectTorrent(a, b))
    override def getPublicAccessBlock(a: GetPublicAccessBlockRequest) = eff1(client.getPublicAccessBlock(a))
    override def headBucket(a: HeadBucketRequest) = eff1(client.headBucket(a))
    override def headObject(a: HeadObjectRequest) = eff1(client.headObject(a))
    override def listBucketAnalyticsConfigurations(a: ListBucketAnalyticsConfigurationsRequest) = eff1(client.listBucketAnalyticsConfigurations(a))
    override def listBucketIntelligentTieringConfigurations(a: ListBucketIntelligentTieringConfigurationsRequest) = eff1(client.listBucketIntelligentTieringConfigurations(a))
    override def listBucketInventoryConfigurations(a: ListBucketInventoryConfigurationsRequest) = eff1(client.listBucketInventoryConfigurations(a))
    override def listBucketMetricsConfigurations(a: ListBucketMetricsConfigurationsRequest) = eff1(client.listBucketMetricsConfigurations(a))
    override def listBuckets = eff1(client.listBuckets)
    override def listBuckets(a: ListBucketsRequest) = eff1(client.listBuckets(a))
    override def listBucketsPaginator = primitive1(client.listBucketsPaginator)
    override def listBucketsPaginator(a: ListBucketsRequest) = primitive1(client.listBucketsPaginator(a))
    override def listDirectoryBuckets(a: ListDirectoryBucketsRequest) = eff1(client.listDirectoryBuckets(a))
    override def listDirectoryBucketsPaginator(a: ListDirectoryBucketsRequest) = primitive1(client.listDirectoryBucketsPaginator(a))
    override def listMultipartUploads(a: ListMultipartUploadsRequest) = eff1(client.listMultipartUploads(a))
    override def listMultipartUploadsPaginator(a: ListMultipartUploadsRequest) = primitive1(client.listMultipartUploadsPaginator(a))
    override def listObjectVersions(a: ListObjectVersionsRequest) = eff1(client.listObjectVersions(a))
    override def listObjectVersionsPaginator(a: ListObjectVersionsRequest) = primitive1(client.listObjectVersionsPaginator(a))
    override def listObjects(a: ListObjectsRequest) = eff1(client.listObjects(a))
    override def listObjectsV2(a: ListObjectsV2Request) = eff1(client.listObjectsV2(a))
    override def listObjectsV2Paginator(a: ListObjectsV2Request) = primitive1(client.listObjectsV2Paginator(a))
    override def listParts(a: ListPartsRequest) = eff1(client.listParts(a))
    override def listPartsPaginator(a: ListPartsRequest) = primitive1(client.listPartsPaginator(a))
    override def putBucketAccelerateConfiguration(a: PutBucketAccelerateConfigurationRequest) = eff1(client.putBucketAccelerateConfiguration(a))
    override def putBucketAcl(a: PutBucketAclRequest) = eff1(client.putBucketAcl(a))
    override def putBucketAnalyticsConfiguration(a: PutBucketAnalyticsConfigurationRequest) = eff1(client.putBucketAnalyticsConfiguration(a))
    override def putBucketCors(a: PutBucketCorsRequest) = eff1(client.putBucketCors(a))
    override def putBucketEncryption(a: PutBucketEncryptionRequest) = eff1(client.putBucketEncryption(a))
    override def putBucketIntelligentTieringConfiguration(a: PutBucketIntelligentTieringConfigurationRequest) = eff1(client.putBucketIntelligentTieringConfiguration(a))
    override def putBucketInventoryConfiguration(a: PutBucketInventoryConfigurationRequest) = eff1(client.putBucketInventoryConfiguration(a))
    override def putBucketLifecycleConfiguration(a: PutBucketLifecycleConfigurationRequest) = eff1(client.putBucketLifecycleConfiguration(a))
    override def putBucketLogging(a: PutBucketLoggingRequest) = eff1(client.putBucketLogging(a))
    override def putBucketMetricsConfiguration(a: PutBucketMetricsConfigurationRequest) = eff1(client.putBucketMetricsConfiguration(a))
    override def putBucketNotificationConfiguration(a: PutBucketNotificationConfigurationRequest) = eff1(client.putBucketNotificationConfiguration(a))
    override def putBucketOwnershipControls(a: PutBucketOwnershipControlsRequest) = eff1(client.putBucketOwnershipControls(a))
    override def putBucketPolicy(a: PutBucketPolicyRequest) = eff1(client.putBucketPolicy(a))
    override def putBucketReplication(a: PutBucketReplicationRequest) = eff1(client.putBucketReplication(a))
    override def putBucketRequestPayment(a: PutBucketRequestPaymentRequest) = eff1(client.putBucketRequestPayment(a))
    override def putBucketTagging(a: PutBucketTaggingRequest) = eff1(client.putBucketTagging(a))
    override def putBucketVersioning(a: PutBucketVersioningRequest) = eff1(client.putBucketVersioning(a))
    override def putBucketWebsite(a: PutBucketWebsiteRequest) = eff1(client.putBucketWebsite(a))
    override def putObject(a: PutObjectRequest, b: AsyncRequestBody) = eff1(client.putObject(a, b))
    override def putObject(a: PutObjectRequest, b: Path) = eff1(client.putObject(a, b))
    override def putObjectAcl(a: PutObjectAclRequest) = eff1(client.putObjectAcl(a))
    override def putObjectLegalHold(a: PutObjectLegalHoldRequest) = eff1(client.putObjectLegalHold(a))
    override def putObjectLockConfiguration(a: PutObjectLockConfigurationRequest) = eff1(client.putObjectLockConfiguration(a))
    override def putObjectRetention(a: PutObjectRetentionRequest) = eff1(client.putObjectRetention(a))
    override def putObjectTagging(a: PutObjectTaggingRequest) = eff1(client.putObjectTagging(a))
    override def putPublicAccessBlock(a: PutPublicAccessBlockRequest) = eff1(client.putPublicAccessBlock(a))
    override def renameObject(a: RenameObjectRequest) = eff1(client.renameObject(a))
    override def restoreObject(a: RestoreObjectRequest) = eff1(client.restoreObject(a))
    override def selectObjectContent(a: SelectObjectContentRequest, b: SelectObjectContentResponseHandler) = eff1(client.selectObjectContent(a, b))
    override def serviceName = primitive1(client.serviceName)
    override def updateBucketMetadataInventoryTableConfiguration(a: UpdateBucketMetadataInventoryTableConfigurationRequest) = eff1(client.updateBucketMetadataInventoryTableConfiguration(a))
    override def updateBucketMetadataJournalTableConfiguration(a: UpdateBucketMetadataJournalTableConfigurationRequest) = eff1(client.updateBucketMetadataJournalTableConfiguration(a))
    override def uploadPart(a: UploadPartRequest, b: AsyncRequestBody) = eff1(client.uploadPart(a, b))
    override def uploadPart(a: UploadPartRequest, b: Path) = eff1(client.uploadPart(a, b))
    override def uploadPartCopy(a: UploadPartCopyRequest) = eff1(client.uploadPartCopy(a))
    override def utilities = primitive1(client.utilities)
    override def waiter = primitive1(client.waiter)
    override def writeGetObjectResponse(a: WriteGetObjectResponseRequest, b: AsyncRequestBody) = eff1(client.writeGetObjectResponse(a, b))
    override def writeGetObjectResponse(a: WriteGetObjectResponseRequest, b: Path) = eff1(client.writeGetObjectResponse(a, b))


  }


}

