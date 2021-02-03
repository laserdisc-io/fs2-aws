package io.laserdisc.pure.s3.tagless

// Library imports
import cats.data.Kleisli
import cats.effect.{ Async, Blocker, ContextShift }
import software.amazon.awssdk.services.s3.model.{ GetObjectResponse, GetObjectTorrentResponse }

import java.util.concurrent.CompletionException

// Types referenced
import software.amazon.awssdk.core.async.{ AsyncRequestBody, AsyncResponseTransformer }
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.{
  AbortMultipartUploadRequest,
  CompleteMultipartUploadRequest,
  CopyObjectRequest,
  CreateBucketRequest,
  CreateMultipartUploadRequest,
  DeleteBucketAnalyticsConfigurationRequest,
  DeleteBucketCorsRequest,
  DeleteBucketEncryptionRequest,
  DeleteBucketIntelligentTieringConfigurationRequest,
  DeleteBucketInventoryConfigurationRequest,
  DeleteBucketLifecycleRequest,
  DeleteBucketMetricsConfigurationRequest,
  DeleteBucketOwnershipControlsRequest,
  DeleteBucketPolicyRequest,
  DeleteBucketReplicationRequest,
  DeleteBucketRequest,
  DeleteBucketTaggingRequest,
  DeleteBucketWebsiteRequest,
  DeleteObjectRequest,
  DeleteObjectTaggingRequest,
  DeleteObjectsRequest,
  DeletePublicAccessBlockRequest,
  GetBucketAccelerateConfigurationRequest,
  GetBucketAclRequest,
  GetBucketAnalyticsConfigurationRequest,
  GetBucketCorsRequest,
  GetBucketEncryptionRequest,
  GetBucketIntelligentTieringConfigurationRequest,
  GetBucketInventoryConfigurationRequest,
  GetBucketLifecycleConfigurationRequest,
  GetBucketLocationRequest,
  GetBucketLoggingRequest,
  GetBucketMetricsConfigurationRequest,
  GetBucketNotificationConfigurationRequest,
  GetBucketOwnershipControlsRequest,
  GetBucketPolicyRequest,
  GetBucketPolicyStatusRequest,
  GetBucketReplicationRequest,
  GetBucketRequestPaymentRequest,
  GetBucketTaggingRequest,
  GetBucketVersioningRequest,
  GetBucketWebsiteRequest,
  GetObjectAclRequest,
  GetObjectLegalHoldRequest,
  GetObjectLockConfigurationRequest,
  GetObjectRequest,
  GetObjectRetentionRequest,
  GetObjectTaggingRequest,
  GetObjectTorrentRequest,
  GetPublicAccessBlockRequest,
  HeadBucketRequest,
  HeadObjectRequest,
  ListBucketAnalyticsConfigurationsRequest,
  ListBucketIntelligentTieringConfigurationsRequest,
  ListBucketInventoryConfigurationsRequest,
  ListBucketMetricsConfigurationsRequest,
  ListBucketsRequest,
  ListMultipartUploadsRequest,
  ListObjectVersionsRequest,
  ListObjectsRequest,
  ListObjectsV2Request,
  ListPartsRequest,
  PutBucketAccelerateConfigurationRequest,
  PutBucketAclRequest,
  PutBucketAnalyticsConfigurationRequest,
  PutBucketCorsRequest,
  PutBucketEncryptionRequest,
  PutBucketIntelligentTieringConfigurationRequest,
  PutBucketInventoryConfigurationRequest,
  PutBucketLifecycleConfigurationRequest,
  PutBucketLoggingRequest,
  PutBucketMetricsConfigurationRequest,
  PutBucketNotificationConfigurationRequest,
  PutBucketOwnershipControlsRequest,
  PutBucketPolicyRequest,
  PutBucketReplicationRequest,
  PutBucketRequestPaymentRequest,
  PutBucketTaggingRequest,
  PutBucketVersioningRequest,
  PutBucketWebsiteRequest,
  PutObjectAclRequest,
  PutObjectLegalHoldRequest,
  PutObjectLockConfigurationRequest,
  PutObjectRequest,
  PutObjectRetentionRequest,
  PutObjectTaggingRequest,
  PutPublicAccessBlockRequest,
  RestoreObjectRequest,
  UploadPartCopyRequest,
  UploadPartRequest
}

import java.nio.file.Path
import java.util.concurrent.CompletableFuture

object Interpreter {

  def apply[M[_]](b: Blocker)(
    implicit am: Async[M],
    cs: ContextShift[M]
  ): Interpreter[M] =
    new Interpreter[M] {
      val asyncM        = am
      val contextShiftM = cs
      val blocker       = b
    }

}

// Family of interpreters into Kleisli arrows for some monad M.
trait Interpreter[M[_]] { outer =>

  implicit val asyncM: Async[M]

  // to support shifting blocking operations to another pool.
  val contextShiftM: ContextShift[M]
  val blocker: Blocker

  lazy val S3AsyncClientInterpreter: S3AsyncClientInterpreter = new S3AsyncClientInterpreter {}
  // Some methods are common to all interpreters and can be overridden to change behavior globally.

  def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli { a =>
    blocker.blockOn[M, A](try {
      asyncM.delay(f(a))
    } catch {
      case scala.util.control.NonFatal(e) => asyncM.raiseError(e)
    })(contextShiftM)
  }
  def primitive1[J, A](f: =>A): M[A] =
    blocker.blockOn[M, A](try {
      asyncM.delay(f)
    } catch {
      case scala.util.control.NonFatal(e) => asyncM.raiseError(e)
    })(contextShiftM)

  def eff[J, A](fut: J => CompletableFuture[A]): Kleisli[M, J, A] = Kleisli { a =>
    asyncM.async { cb =>
      fut(a).handle[Unit] { (a, x) =>
        if (a == null)
          x match {
            case t: CompletionException => cb(Left(t.getCause))
            case t                      => cb(Left(t))
          }
        else
          cb(Right(a))
      }
      ()
    }
  }
  def eff1[J, A](fut: =>CompletableFuture[A]): M[A] =
    asyncM.async { cb =>
      fut.handle[Unit] { (a, x) =>
        if (a == null)
          x match {
            case t: CompletionException => cb(Left(t.getCause))
            case t                      => cb(Left(t))
          }
        else
          cb(Right(a))
      }
      ()
    }

  // Interpreters
  trait S3AsyncClientInterpreter extends S3AsyncClientOp[Kleisli[M, S3AsyncClient, *]] {

    // domain-specific operations are implemented in terms of `primitive`
    override def abortMultipartUpload(a: AbortMultipartUploadRequest) =
      eff(_.abortMultipartUpload(a))
    override def close = primitive(_.close)
    override def completeMultipartUpload(a: CompleteMultipartUploadRequest) =
      eff(_.completeMultipartUpload(a))
    override def copyObject(a: CopyObjectRequest)     = eff(_.copyObject(a))
    override def createBucket(a: CreateBucketRequest) = eff(_.createBucket(a))
    override def createMultipartUpload(a: CreateMultipartUploadRequest) =
      eff(_.createMultipartUpload(a))
    override def deleteBucket(a: DeleteBucketRequest) = eff(_.deleteBucket(a))
    override def deleteBucketAnalyticsConfiguration(a: DeleteBucketAnalyticsConfigurationRequest) =
      eff(_.deleteBucketAnalyticsConfiguration(a))
    override def deleteBucketCors(a: DeleteBucketCorsRequest) = eff(_.deleteBucketCors(a))
    override def deleteBucketEncryption(a: DeleteBucketEncryptionRequest) =
      eff(_.deleteBucketEncryption(a))
    override def deleteBucketIntelligentTieringConfiguration(
      a: DeleteBucketIntelligentTieringConfigurationRequest
    ) = eff(_.deleteBucketIntelligentTieringConfiguration(a))
    override def deleteBucketInventoryConfiguration(a: DeleteBucketInventoryConfigurationRequest) =
      eff(_.deleteBucketInventoryConfiguration(a))
    override def deleteBucketLifecycle(a: DeleteBucketLifecycleRequest) =
      eff(_.deleteBucketLifecycle(a))
    override def deleteBucketMetricsConfiguration(a: DeleteBucketMetricsConfigurationRequest) =
      eff(_.deleteBucketMetricsConfiguration(a))
    override def deleteBucketOwnershipControls(a: DeleteBucketOwnershipControlsRequest) =
      eff(_.deleteBucketOwnershipControls(a))
    override def deleteBucketPolicy(a: DeleteBucketPolicyRequest) = eff(_.deleteBucketPolicy(a))
    override def deleteBucketReplication(a: DeleteBucketReplicationRequest) =
      eff(_.deleteBucketReplication(a))
    override def deleteBucketTagging(a: DeleteBucketTaggingRequest) = eff(_.deleteBucketTagging(a))
    override def deleteBucketWebsite(a: DeleteBucketWebsiteRequest) = eff(_.deleteBucketWebsite(a))
    override def deleteObject(a: DeleteObjectRequest)               = eff(_.deleteObject(a))
    override def deleteObjectTagging(a: DeleteObjectTaggingRequest) = eff(_.deleteObjectTagging(a))
    override def deleteObjects(a: DeleteObjectsRequest)             = eff(_.deleteObjects(a))
    override def deletePublicAccessBlock(a: DeletePublicAccessBlockRequest) =
      eff(_.deletePublicAccessBlock(a))
    override def getBucketAccelerateConfiguration(a: GetBucketAccelerateConfigurationRequest) =
      eff(_.getBucketAccelerateConfiguration(a))
    override def getBucketAcl(a: GetBucketAclRequest) = eff(_.getBucketAcl(a))
    override def getBucketAnalyticsConfiguration(a: GetBucketAnalyticsConfigurationRequest) =
      eff(_.getBucketAnalyticsConfiguration(a))
    override def getBucketCors(a: GetBucketCorsRequest)             = eff(_.getBucketCors(a))
    override def getBucketEncryption(a: GetBucketEncryptionRequest) = eff(_.getBucketEncryption(a))
    override def getBucketIntelligentTieringConfiguration(
      a: GetBucketIntelligentTieringConfigurationRequest
    ) = eff(_.getBucketIntelligentTieringConfiguration(a))
    override def getBucketInventoryConfiguration(a: GetBucketInventoryConfigurationRequest) =
      eff(_.getBucketInventoryConfiguration(a))
    override def getBucketLifecycleConfiguration(a: GetBucketLifecycleConfigurationRequest) =
      eff(_.getBucketLifecycleConfiguration(a))
    override def getBucketLocation(a: GetBucketLocationRequest) = eff(_.getBucketLocation(a))
    override def getBucketLogging(a: GetBucketLoggingRequest)   = eff(_.getBucketLogging(a))
    override def getBucketMetricsConfiguration(a: GetBucketMetricsConfigurationRequest) =
      eff(_.getBucketMetricsConfiguration(a))
    override def getBucketNotificationConfiguration(a: GetBucketNotificationConfigurationRequest) =
      eff(_.getBucketNotificationConfiguration(a))
    override def getBucketOwnershipControls(a: GetBucketOwnershipControlsRequest) =
      eff(_.getBucketOwnershipControls(a))
    override def getBucketPolicy(a: GetBucketPolicyRequest) = eff(_.getBucketPolicy(a))
    override def getBucketPolicyStatus(a: GetBucketPolicyStatusRequest) =
      eff(_.getBucketPolicyStatus(a))
    override def getBucketReplication(a: GetBucketReplicationRequest) =
      eff(_.getBucketReplication(a))
    override def getBucketRequestPayment(a: GetBucketRequestPaymentRequest) =
      eff(_.getBucketRequestPayment(a))
    override def getBucketTagging(a: GetBucketTaggingRequest)       = eff(_.getBucketTagging(a))
    override def getBucketVersioning(a: GetBucketVersioningRequest) = eff(_.getBucketVersioning(a))
    override def getBucketWebsite(a: GetBucketWebsiteRequest)       = eff(_.getBucketWebsite(a))
    override def getObject[ReturnT](
      a: GetObjectRequest,
      b: AsyncResponseTransformer[GetObjectResponse, ReturnT]
    )                                                             = eff(_.getObject(a, b))
    override def getObject(a: GetObjectRequest, b: Path)          = eff(_.getObject(a, b))
    override def getObjectAcl(a: GetObjectAclRequest)             = eff(_.getObjectAcl(a))
    override def getObjectLegalHold(a: GetObjectLegalHoldRequest) = eff(_.getObjectLegalHold(a))
    override def getObjectLockConfiguration(a: GetObjectLockConfigurationRequest) =
      eff(_.getObjectLockConfiguration(a))
    override def getObjectRetention(a: GetObjectRetentionRequest) = eff(_.getObjectRetention(a))
    override def getObjectTagging(a: GetObjectTaggingRequest)     = eff(_.getObjectTagging(a))
    override def getObjectTorrent[ReturnT](
      a: GetObjectTorrentRequest,
      b: AsyncResponseTransformer[GetObjectTorrentResponse, ReturnT]
    ) = eff(_.getObjectTorrent(a, b))
    override def getObjectTorrent(a: GetObjectTorrentRequest, b: Path) =
      eff(_.getObjectTorrent(a, b))
    override def getPublicAccessBlock(a: GetPublicAccessBlockRequest) =
      eff(_.getPublicAccessBlock(a))
    override def headBucket(a: HeadBucketRequest) = eff(_.headBucket(a))
    override def headObject(a: HeadObjectRequest) = eff(_.headObject(a))
    override def listBucketAnalyticsConfigurations(a: ListBucketAnalyticsConfigurationsRequest) =
      eff(_.listBucketAnalyticsConfigurations(a))
    override def listBucketIntelligentTieringConfigurations(
      a: ListBucketIntelligentTieringConfigurationsRequest
    ) = eff(_.listBucketIntelligentTieringConfigurations(a))
    override def listBucketInventoryConfigurations(a: ListBucketInventoryConfigurationsRequest) =
      eff(_.listBucketInventoryConfigurations(a))
    override def listBucketMetricsConfigurations(a: ListBucketMetricsConfigurationsRequest) =
      eff(_.listBucketMetricsConfigurations(a))
    override def listBuckets                        = eff(_.listBuckets)
    override def listBuckets(a: ListBucketsRequest) = eff(_.listBuckets(a))
    override def listMultipartUploads(a: ListMultipartUploadsRequest) =
      eff(_.listMultipartUploads(a))
    override def listMultipartUploadsPaginator(a: ListMultipartUploadsRequest) =
      primitive(_.listMultipartUploadsPaginator(a))
    override def listObjectVersions(a: ListObjectVersionsRequest) = eff(_.listObjectVersions(a))
    override def listObjectVersionsPaginator(a: ListObjectVersionsRequest) =
      primitive(_.listObjectVersionsPaginator(a))
    override def listObjects(a: ListObjectsRequest)     = eff(_.listObjects(a))
    override def listObjectsV2(a: ListObjectsV2Request) = eff(_.listObjectsV2(a))
    override def listObjectsV2Paginator(a: ListObjectsV2Request) =
      primitive(_.listObjectsV2Paginator(a))
    override def listParts(a: ListPartsRequest)          = eff(_.listParts(a))
    override def listPartsPaginator(a: ListPartsRequest) = primitive(_.listPartsPaginator(a))
    override def putBucketAccelerateConfiguration(a: PutBucketAccelerateConfigurationRequest) =
      eff(_.putBucketAccelerateConfiguration(a))
    override def putBucketAcl(a: PutBucketAclRequest) = eff(_.putBucketAcl(a))
    override def putBucketAnalyticsConfiguration(a: PutBucketAnalyticsConfigurationRequest) =
      eff(_.putBucketAnalyticsConfiguration(a))
    override def putBucketCors(a: PutBucketCorsRequest)             = eff(_.putBucketCors(a))
    override def putBucketEncryption(a: PutBucketEncryptionRequest) = eff(_.putBucketEncryption(a))
    override def putBucketIntelligentTieringConfiguration(
      a: PutBucketIntelligentTieringConfigurationRequest
    ) = eff(_.putBucketIntelligentTieringConfiguration(a))
    override def putBucketInventoryConfiguration(a: PutBucketInventoryConfigurationRequest) =
      eff(_.putBucketInventoryConfiguration(a))
    override def putBucketLifecycleConfiguration(a: PutBucketLifecycleConfigurationRequest) =
      eff(_.putBucketLifecycleConfiguration(a))
    override def putBucketLogging(a: PutBucketLoggingRequest) = eff(_.putBucketLogging(a))
    override def putBucketMetricsConfiguration(a: PutBucketMetricsConfigurationRequest) =
      eff(_.putBucketMetricsConfiguration(a))
    override def putBucketNotificationConfiguration(a: PutBucketNotificationConfigurationRequest) =
      eff(_.putBucketNotificationConfiguration(a))
    override def putBucketOwnershipControls(a: PutBucketOwnershipControlsRequest) =
      eff(_.putBucketOwnershipControls(a))
    override def putBucketPolicy(a: PutBucketPolicyRequest) = eff(_.putBucketPolicy(a))
    override def putBucketReplication(a: PutBucketReplicationRequest) =
      eff(_.putBucketReplication(a))
    override def putBucketRequestPayment(a: PutBucketRequestPaymentRequest) =
      eff(_.putBucketRequestPayment(a))
    override def putBucketTagging(a: PutBucketTaggingRequest)        = eff(_.putBucketTagging(a))
    override def putBucketVersioning(a: PutBucketVersioningRequest)  = eff(_.putBucketVersioning(a))
    override def putBucketWebsite(a: PutBucketWebsiteRequest)        = eff(_.putBucketWebsite(a))
    override def putObject(a: PutObjectRequest, b: AsyncRequestBody) = eff(_.putObject(a, b))
    override def putObject(a: PutObjectRequest, b: Path)             = eff(_.putObject(a, b))
    override def putObjectAcl(a: PutObjectAclRequest)                = eff(_.putObjectAcl(a))
    override def putObjectLegalHold(a: PutObjectLegalHoldRequest)    = eff(_.putObjectLegalHold(a))
    override def putObjectLockConfiguration(a: PutObjectLockConfigurationRequest) =
      eff(_.putObjectLockConfiguration(a))
    override def putObjectRetention(a: PutObjectRetentionRequest) = eff(_.putObjectRetention(a))
    override def putObjectTagging(a: PutObjectTaggingRequest)     = eff(_.putObjectTagging(a))
    override def putPublicAccessBlock(a: PutPublicAccessBlockRequest) =
      eff(_.putPublicAccessBlock(a))
    override def restoreObject(a: RestoreObjectRequest)                = eff(_.restoreObject(a))
    override def serviceName                                           = primitive(_.serviceName)
    override def uploadPart(a: UploadPartRequest, b: AsyncRequestBody) = eff(_.uploadPart(a, b))
    override def uploadPart(a: UploadPartRequest, b: Path)             = eff(_.uploadPart(a, b))
    override def uploadPartCopy(a: UploadPartCopyRequest)              = eff(_.uploadPartCopy(a))
    override def utilities                                             = primitive(_.utilities)
    override def waiter                                                = primitive(_.waiter)

  }

  def create(client: S3AsyncClient): S3AsyncClientOp[M] = new S3AsyncClientOp[M] {

    // domain-specific operations are implemented in terms of `primitive`
    override def abortMultipartUpload(a: AbortMultipartUploadRequest) =
      eff1(client.abortMultipartUpload(a))
    override def close = primitive1(client.close)
    override def completeMultipartUpload(a: CompleteMultipartUploadRequest) =
      eff1(client.completeMultipartUpload(a))
    override def copyObject(a: CopyObjectRequest)     = eff1(client.copyObject(a))
    override def createBucket(a: CreateBucketRequest) = eff1(client.createBucket(a))
    override def createMultipartUpload(a: CreateMultipartUploadRequest) =
      eff1(client.createMultipartUpload(a))
    override def deleteBucket(a: DeleteBucketRequest) = eff1(client.deleteBucket(a))
    override def deleteBucketAnalyticsConfiguration(a: DeleteBucketAnalyticsConfigurationRequest) =
      eff1(client.deleteBucketAnalyticsConfiguration(a))
    override def deleteBucketCors(a: DeleteBucketCorsRequest) = eff1(client.deleteBucketCors(a))
    override def deleteBucketEncryption(a: DeleteBucketEncryptionRequest) =
      eff1(client.deleteBucketEncryption(a))
    override def deleteBucketIntelligentTieringConfiguration(
      a: DeleteBucketIntelligentTieringConfigurationRequest
    ) = eff1(client.deleteBucketIntelligentTieringConfiguration(a))
    override def deleteBucketInventoryConfiguration(a: DeleteBucketInventoryConfigurationRequest) =
      eff1(client.deleteBucketInventoryConfiguration(a))
    override def deleteBucketLifecycle(a: DeleteBucketLifecycleRequest) =
      eff1(client.deleteBucketLifecycle(a))
    override def deleteBucketMetricsConfiguration(a: DeleteBucketMetricsConfigurationRequest) =
      eff1(client.deleteBucketMetricsConfiguration(a))
    override def deleteBucketOwnershipControls(a: DeleteBucketOwnershipControlsRequest) =
      eff1(client.deleteBucketOwnershipControls(a))
    override def deleteBucketPolicy(a: DeleteBucketPolicyRequest) =
      eff1(client.deleteBucketPolicy(a))
    override def deleteBucketReplication(a: DeleteBucketReplicationRequest) =
      eff1(client.deleteBucketReplication(a))
    override def deleteBucketTagging(a: DeleteBucketTaggingRequest) =
      eff1(client.deleteBucketTagging(a))
    override def deleteBucketWebsite(a: DeleteBucketWebsiteRequest) =
      eff1(client.deleteBucketWebsite(a))
    override def deleteObject(a: DeleteObjectRequest) = eff1(client.deleteObject(a))
    override def deleteObjectTagging(a: DeleteObjectTaggingRequest) =
      eff1(client.deleteObjectTagging(a))
    override def deleteObjects(a: DeleteObjectsRequest) = eff1(client.deleteObjects(a))
    override def deletePublicAccessBlock(a: DeletePublicAccessBlockRequest) =
      eff1(client.deletePublicAccessBlock(a))
    override def getBucketAccelerateConfiguration(a: GetBucketAccelerateConfigurationRequest) =
      eff1(client.getBucketAccelerateConfiguration(a))
    override def getBucketAcl(a: GetBucketAclRequest) = eff1(client.getBucketAcl(a))
    override def getBucketAnalyticsConfiguration(a: GetBucketAnalyticsConfigurationRequest) =
      eff1(client.getBucketAnalyticsConfiguration(a))
    override def getBucketCors(a: GetBucketCorsRequest) = eff1(client.getBucketCors(a))
    override def getBucketEncryption(a: GetBucketEncryptionRequest) =
      eff1(client.getBucketEncryption(a))
    override def getBucketIntelligentTieringConfiguration(
      a: GetBucketIntelligentTieringConfigurationRequest
    ) = eff1(client.getBucketIntelligentTieringConfiguration(a))
    override def getBucketInventoryConfiguration(a: GetBucketInventoryConfigurationRequest) =
      eff1(client.getBucketInventoryConfiguration(a))
    override def getBucketLifecycleConfiguration(a: GetBucketLifecycleConfigurationRequest) =
      eff1(client.getBucketLifecycleConfiguration(a))
    override def getBucketLocation(a: GetBucketLocationRequest) = eff1(client.getBucketLocation(a))
    override def getBucketLogging(a: GetBucketLoggingRequest)   = eff1(client.getBucketLogging(a))
    override def getBucketMetricsConfiguration(a: GetBucketMetricsConfigurationRequest) =
      eff1(client.getBucketMetricsConfiguration(a))
    override def getBucketNotificationConfiguration(a: GetBucketNotificationConfigurationRequest) =
      eff1(client.getBucketNotificationConfiguration(a))
    override def getBucketOwnershipControls(a: GetBucketOwnershipControlsRequest) =
      eff1(client.getBucketOwnershipControls(a))
    override def getBucketPolicy(a: GetBucketPolicyRequest) = eff1(client.getBucketPolicy(a))
    override def getBucketPolicyStatus(a: GetBucketPolicyStatusRequest) =
      eff1(client.getBucketPolicyStatus(a))
    override def getBucketReplication(a: GetBucketReplicationRequest) =
      eff1(client.getBucketReplication(a))
    override def getBucketRequestPayment(a: GetBucketRequestPaymentRequest) =
      eff1(client.getBucketRequestPayment(a))
    override def getBucketTagging(a: GetBucketTaggingRequest) = eff1(client.getBucketTagging(a))
    override def getBucketVersioning(a: GetBucketVersioningRequest) =
      eff1(client.getBucketVersioning(a))
    override def getBucketWebsite(a: GetBucketWebsiteRequest) = eff1(client.getBucketWebsite(a))
    override def getObject[ReturnT](
      a: GetObjectRequest,
      b: AsyncResponseTransformer[GetObjectResponse, ReturnT]
    )                                                    = eff1(client.getObject(a, b))
    override def getObject(a: GetObjectRequest, b: Path) = eff1(client.getObject(a, b))
    override def getObjectAcl(a: GetObjectAclRequest)    = eff1(client.getObjectAcl(a))
    override def getObjectLegalHold(a: GetObjectLegalHoldRequest) =
      eff1(client.getObjectLegalHold(a))
    override def getObjectLockConfiguration(a: GetObjectLockConfigurationRequest) =
      eff1(client.getObjectLockConfiguration(a))
    override def getObjectRetention(a: GetObjectRetentionRequest) =
      eff1(client.getObjectRetention(a))
    override def getObjectTagging(a: GetObjectTaggingRequest) = eff1(client.getObjectTagging(a))
    override def getObjectTorrent[ReturnT](
      a: GetObjectTorrentRequest,
      b: AsyncResponseTransformer[GetObjectTorrentResponse, ReturnT]
    ) = eff1(client.getObjectTorrent(a, b))
    override def getObjectTorrent(a: GetObjectTorrentRequest, b: Path) =
      eff1(client.getObjectTorrent(a, b))
    override def getPublicAccessBlock(a: GetPublicAccessBlockRequest) =
      eff1(client.getPublicAccessBlock(a))
    override def headBucket(a: HeadBucketRequest) = eff1(client.headBucket(a))
    override def headObject(a: HeadObjectRequest) = eff1(client.headObject(a))
    override def listBucketAnalyticsConfigurations(a: ListBucketAnalyticsConfigurationsRequest) =
      eff1(client.listBucketAnalyticsConfigurations(a))
    override def listBucketIntelligentTieringConfigurations(
      a: ListBucketIntelligentTieringConfigurationsRequest
    ) = eff1(client.listBucketIntelligentTieringConfigurations(a))
    override def listBucketInventoryConfigurations(a: ListBucketInventoryConfigurationsRequest) =
      eff1(client.listBucketInventoryConfigurations(a))
    override def listBucketMetricsConfigurations(a: ListBucketMetricsConfigurationsRequest) =
      eff1(client.listBucketMetricsConfigurations(a))
    override def listBuckets                        = eff1(client.listBuckets)
    override def listBuckets(a: ListBucketsRequest) = eff1(client.listBuckets(a))
    override def listMultipartUploads(a: ListMultipartUploadsRequest) =
      eff1(client.listMultipartUploads(a))
    override def listMultipartUploadsPaginator(a: ListMultipartUploadsRequest) =
      primitive1(client.listMultipartUploadsPaginator(a))
    override def listObjectVersions(a: ListObjectVersionsRequest) =
      eff1(client.listObjectVersions(a))
    override def listObjectVersionsPaginator(a: ListObjectVersionsRequest) =
      primitive1(client.listObjectVersionsPaginator(a))
    override def listObjects(a: ListObjectsRequest)     = eff1(client.listObjects(a))
    override def listObjectsV2(a: ListObjectsV2Request) = eff1(client.listObjectsV2(a))
    override def listObjectsV2Paginator(a: ListObjectsV2Request) =
      primitive1(client.listObjectsV2Paginator(a))
    override def listParts(a: ListPartsRequest)          = eff1(client.listParts(a))
    override def listPartsPaginator(a: ListPartsRequest) = primitive1(client.listPartsPaginator(a))
    override def putBucketAccelerateConfiguration(a: PutBucketAccelerateConfigurationRequest) =
      eff1(client.putBucketAccelerateConfiguration(a))
    override def putBucketAcl(a: PutBucketAclRequest) = eff1(client.putBucketAcl(a))
    override def putBucketAnalyticsConfiguration(a: PutBucketAnalyticsConfigurationRequest) =
      eff1(client.putBucketAnalyticsConfiguration(a))
    override def putBucketCors(a: PutBucketCorsRequest) = eff1(client.putBucketCors(a))
    override def putBucketEncryption(a: PutBucketEncryptionRequest) =
      eff1(client.putBucketEncryption(a))
    override def putBucketIntelligentTieringConfiguration(
      a: PutBucketIntelligentTieringConfigurationRequest
    ) = eff1(client.putBucketIntelligentTieringConfiguration(a))
    override def putBucketInventoryConfiguration(a: PutBucketInventoryConfigurationRequest) =
      eff1(client.putBucketInventoryConfiguration(a))
    override def putBucketLifecycleConfiguration(a: PutBucketLifecycleConfigurationRequest) =
      eff1(client.putBucketLifecycleConfiguration(a))
    override def putBucketLogging(a: PutBucketLoggingRequest) = eff1(client.putBucketLogging(a))
    override def putBucketMetricsConfiguration(a: PutBucketMetricsConfigurationRequest) =
      eff1(client.putBucketMetricsConfiguration(a))
    override def putBucketNotificationConfiguration(a: PutBucketNotificationConfigurationRequest) =
      eff1(client.putBucketNotificationConfiguration(a))
    override def putBucketOwnershipControls(a: PutBucketOwnershipControlsRequest) =
      eff1(client.putBucketOwnershipControls(a))
    override def putBucketPolicy(a: PutBucketPolicyRequest) = eff1(client.putBucketPolicy(a))
    override def putBucketReplication(a: PutBucketReplicationRequest) =
      eff1(client.putBucketReplication(a))
    override def putBucketRequestPayment(a: PutBucketRequestPaymentRequest) =
      eff1(client.putBucketRequestPayment(a))
    override def putBucketTagging(a: PutBucketTaggingRequest) = eff1(client.putBucketTagging(a))
    override def putBucketVersioning(a: PutBucketVersioningRequest) =
      eff1(client.putBucketVersioning(a))
    override def putBucketWebsite(a: PutBucketWebsiteRequest)        = eff1(client.putBucketWebsite(a))
    override def putObject(a: PutObjectRequest, b: AsyncRequestBody) = eff1(client.putObject(a, b))
    override def putObject(a: PutObjectRequest, b: Path)             = eff1(client.putObject(a, b))
    override def putObjectAcl(a: PutObjectAclRequest)                = eff1(client.putObjectAcl(a))
    override def putObjectLegalHold(a: PutObjectLegalHoldRequest) =
      eff1(client.putObjectLegalHold(a))
    override def putObjectLockConfiguration(a: PutObjectLockConfigurationRequest) =
      eff1(client.putObjectLockConfiguration(a))
    override def putObjectRetention(a: PutObjectRetentionRequest) =
      eff1(client.putObjectRetention(a))
    override def putObjectTagging(a: PutObjectTaggingRequest) = eff1(client.putObjectTagging(a))
    override def putPublicAccessBlock(a: PutPublicAccessBlockRequest) =
      eff1(client.putPublicAccessBlock(a))
    override def restoreObject(a: RestoreObjectRequest) = eff1(client.restoreObject(a))
    override def serviceName                            = primitive1(client.serviceName)
    override def uploadPart(a: UploadPartRequest, b: AsyncRequestBody) =
      eff1(client.uploadPart(a, b))
    override def uploadPart(a: UploadPartRequest, b: Path) = eff1(client.uploadPart(a, b))
    override def uploadPartCopy(a: UploadPartCopyRequest)  = eff1(client.uploadPartCopy(a))
    override def utilities                                 = primitive1(client.utilities)
    override def waiter                                    = primitive1(client.waiter)

  }

}
