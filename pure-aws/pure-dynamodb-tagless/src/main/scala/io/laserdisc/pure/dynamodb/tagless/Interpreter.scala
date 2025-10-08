package io.laserdisc.pure.dynamodb.tagless

// Library imports
import cats.data.Kleisli
import cats.effect.{Async, Resource}

import software.amazon.awssdk.services.dynamodb.*
import software.amazon.awssdk.services.dynamodb.model.*

// Types referenced
import software.amazon.awssdk.services.dynamodb.paginators.BatchGetItemPublisher
import software.amazon.awssdk.services.dynamodb.paginators.ListContributorInsightsPublisher
import software.amazon.awssdk.services.dynamodb.paginators.ListExportsPublisher
import software.amazon.awssdk.services.dynamodb.paginators.ListImportsPublisher
import software.amazon.awssdk.services.dynamodb.paginators.ListTablesPublisher
import software.amazon.awssdk.services.dynamodb.paginators.QueryPublisher
import software.amazon.awssdk.services.dynamodb.paginators.ScanPublisher
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbAsyncWaiter

object Interpreter {

  def apply[M[_]](implicit
      am: Async[M]
  ): Interpreter[M] =
    new Interpreter[M] {
      val asyncM: Async[M] = am
    }

}

// Family of interpreters into Kleisli arrows for some monad M.
trait Interpreter[M[_]] { outer =>

  import java.util.concurrent.CompletableFuture

  implicit val asyncM: Async[M]

  lazy val DynamoDbAsyncClientInterpreter: DynamoDbAsyncClientInterpreter = new DynamoDbAsyncClientInterpreter {}
  // Some methods are common to all interpreters and can be overridden to change behavior globally.

  private def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(j => asyncM.blocking(f(j)))
  private def primitive1[J, A](f: => A): M[A]              = asyncM.blocking(f)

  private def eff[J, A](fut: J => CompletableFuture[A]): Kleisli[M, J, A] = Kleisli { j =>
    asyncM.fromCompletableFuture(asyncM.delay(fut(j)))
  }
  private def eff1[J, A](fut: => CompletableFuture[A]): M[A] =
    asyncM.fromCompletableFuture(asyncM.delay(fut))

  // Interpreters // scalafmt: off
  trait DynamoDbAsyncClientInterpreter extends DynamoDbAsyncClientOp[Kleisli[M, DynamoDbAsyncClient, *]] {

    // domain-specific operations are implemented in terms of `primitive`
    override def batchExecuteStatement(a: BatchExecuteStatementRequest): Kleisli[M, DynamoDbAsyncClient, BatchExecuteStatementResponse]                                           = eff(_.batchExecuteStatement(a))
    override def batchGetItem(a: BatchGetItemRequest): Kleisli[M, DynamoDbAsyncClient, BatchGetItemResponse]                                                                      = eff(_.batchGetItem(a))
    override def batchGetItemPaginator(a: BatchGetItemRequest): Kleisli[M, DynamoDbAsyncClient, BatchGetItemPublisher]                                                            = primitive(_.batchGetItemPaginator(a))
    override def batchWriteItem(a: BatchWriteItemRequest): Kleisli[M, DynamoDbAsyncClient, BatchWriteItemResponse]                                                                = eff(_.batchWriteItem(a))
    override def close: Kleisli[M, DynamoDbAsyncClient, Unit]                                                                                                                     = primitive(_.close)
    override def createBackup(a: CreateBackupRequest): Kleisli[M, DynamoDbAsyncClient, CreateBackupResponse]                                                                      = eff(_.createBackup(a))
    override def createGlobalTable(a: CreateGlobalTableRequest): Kleisli[M, DynamoDbAsyncClient, CreateGlobalTableResponse]                                                       = eff(_.createGlobalTable(a))
    override def createTable(a: CreateTableRequest): Kleisli[M, DynamoDbAsyncClient, CreateTableResponse]                                                                         = eff(_.createTable(a))
    override def deleteBackup(a: DeleteBackupRequest): Kleisli[M, DynamoDbAsyncClient, DeleteBackupResponse]                                                                      = eff(_.deleteBackup(a))
    override def deleteItem(a: DeleteItemRequest): Kleisli[M, DynamoDbAsyncClient, DeleteItemResponse]                                                                            = eff(_.deleteItem(a))
    override def deleteResourcePolicy(a: DeleteResourcePolicyRequest): Kleisli[M, DynamoDbAsyncClient, DeleteResourcePolicyResponse]                                              = eff(_.deleteResourcePolicy(a))
    override def deleteTable(a: DeleteTableRequest): Kleisli[M, DynamoDbAsyncClient, DeleteTableResponse]                                                                         = eff(_.deleteTable(a))
    override def describeBackup(a: DescribeBackupRequest): Kleisli[M, DynamoDbAsyncClient, DescribeBackupResponse]                                                                = eff(_.describeBackup(a))
    override def describeContinuousBackups(a: DescribeContinuousBackupsRequest): Kleisli[M, DynamoDbAsyncClient, DescribeContinuousBackupsResponse]                               = eff(_.describeContinuousBackups(a))
    override def describeContributorInsights(a: DescribeContributorInsightsRequest): Kleisli[M, DynamoDbAsyncClient, DescribeContributorInsightsResponse]                         = eff(_.describeContributorInsights(a))
    override def describeEndpoints: Kleisli[M, DynamoDbAsyncClient, DescribeEndpointsResponse]                                                                                    = eff(_.describeEndpoints)
    override def describeEndpoints(a: DescribeEndpointsRequest): Kleisli[M, DynamoDbAsyncClient, DescribeEndpointsResponse]                                                       = eff(_.describeEndpoints(a))
    override def describeExport(a: DescribeExportRequest): Kleisli[M, DynamoDbAsyncClient, DescribeExportResponse]                                                                = eff(_.describeExport(a))
    override def describeGlobalTable(a: DescribeGlobalTableRequest): Kleisli[M, DynamoDbAsyncClient, DescribeGlobalTableResponse]                                                 = eff(_.describeGlobalTable(a))
    override def describeGlobalTableSettings(a: DescribeGlobalTableSettingsRequest): Kleisli[M, DynamoDbAsyncClient, DescribeGlobalTableSettingsResponse]                         = eff(_.describeGlobalTableSettings(a))
    override def describeImport(a: DescribeImportRequest): Kleisli[M, DynamoDbAsyncClient, DescribeImportResponse]                                                                = eff(_.describeImport(a))
    override def describeKinesisStreamingDestination(a: DescribeKinesisStreamingDestinationRequest): Kleisli[M, DynamoDbAsyncClient, DescribeKinesisStreamingDestinationResponse] = eff(_.describeKinesisStreamingDestination(a))
    override def describeLimits: Kleisli[M, DynamoDbAsyncClient, DescribeLimitsResponse]                                                                                          = eff(_.describeLimits)
    override def describeLimits(a: DescribeLimitsRequest): Kleisli[M, DynamoDbAsyncClient, DescribeLimitsResponse]                                                                = eff(_.describeLimits(a))
    override def describeTable(a: DescribeTableRequest): Kleisli[M, DynamoDbAsyncClient, DescribeTableResponse]                                                                   = eff(_.describeTable(a))
    override def describeTableReplicaAutoScaling(a: DescribeTableReplicaAutoScalingRequest): Kleisli[M, DynamoDbAsyncClient, DescribeTableReplicaAutoScalingResponse]             = eff(_.describeTableReplicaAutoScaling(a))
    override def describeTimeToLive(a: DescribeTimeToLiveRequest): Kleisli[M, DynamoDbAsyncClient, DescribeTimeToLiveResponse]                                                    = eff(_.describeTimeToLive(a))
    override def disableKinesisStreamingDestination(a: DisableKinesisStreamingDestinationRequest): Kleisli[M, DynamoDbAsyncClient, DisableKinesisStreamingDestinationResponse]    = eff(_.disableKinesisStreamingDestination(a))
    override def enableKinesisStreamingDestination(a: EnableKinesisStreamingDestinationRequest): Kleisli[M, DynamoDbAsyncClient, EnableKinesisStreamingDestinationResponse]       = eff(_.enableKinesisStreamingDestination(a))
    override def executeStatement(a: ExecuteStatementRequest): Kleisli[M, DynamoDbAsyncClient, ExecuteStatementResponse]                                                          = eff(_.executeStatement(a))
    override def executeTransaction(a: ExecuteTransactionRequest): Kleisli[M, DynamoDbAsyncClient, ExecuteTransactionResponse]                                                    = eff(_.executeTransaction(a))
    override def exportTableToPointInTime(a: ExportTableToPointInTimeRequest): Kleisli[M, DynamoDbAsyncClient, ExportTableToPointInTimeResponse]                                  = eff(_.exportTableToPointInTime(a))
    override def getItem(a: GetItemRequest): Kleisli[M, DynamoDbAsyncClient, GetItemResponse]                                                                                     = eff(_.getItem(a))
    override def getResourcePolicy(a: GetResourcePolicyRequest): Kleisli[M, DynamoDbAsyncClient, GetResourcePolicyResponse]                                                       = eff(_.getResourcePolicy(a))
    override def importTable(a: ImportTableRequest): Kleisli[M, DynamoDbAsyncClient, ImportTableResponse]                                                                         = eff(_.importTable(a))
    override def listBackups: Kleisli[M, DynamoDbAsyncClient, ListBackupsResponse]                                                                                                = eff(_.listBackups)
    override def listBackups(a: ListBackupsRequest): Kleisli[M, DynamoDbAsyncClient, ListBackupsResponse]                                                                         = eff(_.listBackups(a))
    override def listContributorInsights(a: ListContributorInsightsRequest): Kleisli[M, DynamoDbAsyncClient, ListContributorInsightsResponse]                                     = eff(_.listContributorInsights(a))
    override def listContributorInsightsPaginator(a: ListContributorInsightsRequest): Kleisli[M, DynamoDbAsyncClient, ListContributorInsightsPublisher]                           = primitive(_.listContributorInsightsPaginator(a))
    override def listExports(a: ListExportsRequest): Kleisli[M, DynamoDbAsyncClient, ListExportsResponse]                                                                         = eff(_.listExports(a))
    override def listExportsPaginator(a: ListExportsRequest): Kleisli[M, DynamoDbAsyncClient, ListExportsPublisher]                                                               = primitive(_.listExportsPaginator(a))
    override def listGlobalTables: Kleisli[M, DynamoDbAsyncClient, ListGlobalTablesResponse]                                                                                      = eff(_.listGlobalTables)
    override def listGlobalTables(a: ListGlobalTablesRequest): Kleisli[M, DynamoDbAsyncClient, ListGlobalTablesResponse]                                                          = eff(_.listGlobalTables(a))
    override def listImports(a: ListImportsRequest): Kleisli[M, DynamoDbAsyncClient, ListImportsResponse]                                                                         = eff(_.listImports(a))
    override def listImportsPaginator(a: ListImportsRequest): Kleisli[M, DynamoDbAsyncClient, ListImportsPublisher]                                                               = primitive(_.listImportsPaginator(a))
    override def listTables: Kleisli[M, DynamoDbAsyncClient, ListTablesResponse]                                                                                                  = eff(_.listTables)
    override def listTables(a: ListTablesRequest): Kleisli[M, DynamoDbAsyncClient, ListTablesResponse]                                                                            = eff(_.listTables(a))
    override def listTablesPaginator: Kleisli[M, DynamoDbAsyncClient, ListTablesPublisher]                                                                                        = primitive(_.listTablesPaginator)
    override def listTablesPaginator(a: ListTablesRequest): Kleisli[M, DynamoDbAsyncClient, ListTablesPublisher]                                                                  = primitive(_.listTablesPaginator(a))
    override def listTagsOfResource(a: ListTagsOfResourceRequest): Kleisli[M, DynamoDbAsyncClient, ListTagsOfResourceResponse]                                                    = eff(_.listTagsOfResource(a))
    override def putItem(a: PutItemRequest): Kleisli[M, DynamoDbAsyncClient, PutItemResponse]                                                                                     = eff(_.putItem(a))
    override def putResourcePolicy(a: PutResourcePolicyRequest): Kleisli[M, DynamoDbAsyncClient, PutResourcePolicyResponse]                                                       = eff(_.putResourcePolicy(a))
    override def query(a: QueryRequest): Kleisli[M, DynamoDbAsyncClient, QueryResponse]                                                                                           = eff(_.query(a))
    override def queryPaginator(a: QueryRequest): Kleisli[M, DynamoDbAsyncClient, QueryPublisher]                                                                                 = primitive(_.queryPaginator(a))
    override def restoreTableFromBackup(a: RestoreTableFromBackupRequest): Kleisli[M, DynamoDbAsyncClient, RestoreTableFromBackupResponse]                                        = eff(_.restoreTableFromBackup(a))
    override def restoreTableToPointInTime(a: RestoreTableToPointInTimeRequest): Kleisli[M, DynamoDbAsyncClient, RestoreTableToPointInTimeResponse]                               = eff(_.restoreTableToPointInTime(a))
    override def scan(a: ScanRequest): Kleisli[M, DynamoDbAsyncClient, ScanResponse]                                                                                              = eff(_.scan(a))
    override def scanPaginator(a: ScanRequest): Kleisli[M, DynamoDbAsyncClient, ScanPublisher]                                                                                    = primitive(_.scanPaginator(a))
    override def serviceName: Kleisli[M, DynamoDbAsyncClient, String]                                                                                                             = primitive(_.serviceName)
    override def tagResource(a: TagResourceRequest): Kleisli[M, DynamoDbAsyncClient, TagResourceResponse]                                                                         = eff(_.tagResource(a))
    override def transactGetItems(a: TransactGetItemsRequest): Kleisli[M, DynamoDbAsyncClient, TransactGetItemsResponse]                                                          = eff(_.transactGetItems(a))
    override def transactWriteItems(a: TransactWriteItemsRequest): Kleisli[M, DynamoDbAsyncClient, TransactWriteItemsResponse]                                                    = eff(_.transactWriteItems(a))
    override def untagResource(a: UntagResourceRequest): Kleisli[M, DynamoDbAsyncClient, UntagResourceResponse]                                                                   = eff(_.untagResource(a))
    override def updateContinuousBackups(a: UpdateContinuousBackupsRequest): Kleisli[M, DynamoDbAsyncClient, UpdateContinuousBackupsResponse]                                     = eff(_.updateContinuousBackups(a))
    override def updateContributorInsights(a: UpdateContributorInsightsRequest): Kleisli[M, DynamoDbAsyncClient, UpdateContributorInsightsResponse]                               = eff(_.updateContributorInsights(a))
    override def updateGlobalTable(a: UpdateGlobalTableRequest): Kleisli[M, DynamoDbAsyncClient, UpdateGlobalTableResponse]                                                       = eff(_.updateGlobalTable(a))
    override def updateGlobalTableSettings(a: UpdateGlobalTableSettingsRequest): Kleisli[M, DynamoDbAsyncClient, UpdateGlobalTableSettingsResponse]                               = eff(_.updateGlobalTableSettings(a))
    override def updateItem(a: UpdateItemRequest): Kleisli[M, DynamoDbAsyncClient, UpdateItemResponse]                                                                            = eff(_.updateItem(a))
    override def updateKinesisStreamingDestination(a: UpdateKinesisStreamingDestinationRequest): Kleisli[M, DynamoDbAsyncClient, UpdateKinesisStreamingDestinationResponse]       = eff(_.updateKinesisStreamingDestination(a))
    override def updateTable(a: UpdateTableRequest): Kleisli[M, DynamoDbAsyncClient, UpdateTableResponse]                                                                         = eff(_.updateTable(a))
    override def updateTableReplicaAutoScaling(a: UpdateTableReplicaAutoScalingRequest): Kleisli[M, DynamoDbAsyncClient, UpdateTableReplicaAutoScalingResponse]                   = eff(_.updateTableReplicaAutoScaling(a))
    override def updateTimeToLive(a: UpdateTimeToLiveRequest): Kleisli[M, DynamoDbAsyncClient, UpdateTimeToLiveResponse]                                                          = eff(_.updateTimeToLive(a))
    override def waiter: Kleisli[M, DynamoDbAsyncClient, DynamoDbAsyncWaiter]                                                                                                     = primitive(_.waiter)

    def lens[E](f: E => DynamoDbAsyncClient): DynamoDbAsyncClientOp[Kleisli[M, E, *]] =
      new DynamoDbAsyncClientOp[Kleisli[M, E, *]] {
        override def batchExecuteStatement(a: BatchExecuteStatementRequest): Kleisli[M, E, BatchExecuteStatementResponse]                                           = Kleisli(e => eff1(f(e).batchExecuteStatement(a)))
        override def batchGetItem(a: BatchGetItemRequest): Kleisli[M, E, BatchGetItemResponse]                                                                      = Kleisli(e => eff1(f(e).batchGetItem(a)))
        override def batchGetItemPaginator(a: BatchGetItemRequest): Kleisli[M, E, BatchGetItemPublisher]                                                            = Kleisli(e => primitive1(f(e).batchGetItemPaginator(a)))
        override def batchWriteItem(a: BatchWriteItemRequest): Kleisli[M, E, BatchWriteItemResponse]                                                                = Kleisli(e => eff1(f(e).batchWriteItem(a)))
        override def close: Kleisli[M, E, Unit]                                                                                                                     = Kleisli(e => primitive1(f(e).close))
        override def createBackup(a: CreateBackupRequest): Kleisli[M, E, CreateBackupResponse]                                                                      = Kleisli(e => eff1(f(e).createBackup(a)))
        override def createGlobalTable(a: CreateGlobalTableRequest): Kleisli[M, E, CreateGlobalTableResponse]                                                       = Kleisli(e => eff1(f(e).createGlobalTable(a)))
        override def createTable(a: CreateTableRequest): Kleisli[M, E, CreateTableResponse]                                                                         = Kleisli(e => eff1(f(e).createTable(a)))
        override def deleteBackup(a: DeleteBackupRequest): Kleisli[M, E, DeleteBackupResponse]                                                                      = Kleisli(e => eff1(f(e).deleteBackup(a)))
        override def deleteItem(a: DeleteItemRequest): Kleisli[M, E, DeleteItemResponse]                                                                            = Kleisli(e => eff1(f(e).deleteItem(a)))
        override def deleteResourcePolicy(a: DeleteResourcePolicyRequest): Kleisli[M, E, DeleteResourcePolicyResponse]                                              = Kleisli(e => eff1(f(e).deleteResourcePolicy(a)))
        override def deleteTable(a: DeleteTableRequest): Kleisli[M, E, DeleteTableResponse]                                                                         = Kleisli(e => eff1(f(e).deleteTable(a)))
        override def describeBackup(a: DescribeBackupRequest): Kleisli[M, E, DescribeBackupResponse]                                                                = Kleisli(e => eff1(f(e).describeBackup(a)))
        override def describeContinuousBackups(a: DescribeContinuousBackupsRequest): Kleisli[M, E, DescribeContinuousBackupsResponse]                               = Kleisli(e => eff1(f(e).describeContinuousBackups(a)))
        override def describeContributorInsights(a: DescribeContributorInsightsRequest): Kleisli[M, E, DescribeContributorInsightsResponse]                         = Kleisli(e => eff1(f(e).describeContributorInsights(a)))
        override def describeEndpoints: Kleisli[M, E, DescribeEndpointsResponse]                                                                                    = Kleisli(e => eff1(f(e).describeEndpoints))
        override def describeEndpoints(a: DescribeEndpointsRequest): Kleisli[M, E, DescribeEndpointsResponse]                                                       = Kleisli(e => eff1(f(e).describeEndpoints(a)))
        override def describeExport(a: DescribeExportRequest): Kleisli[M, E, DescribeExportResponse]                                                                = Kleisli(e => eff1(f(e).describeExport(a)))
        override def describeGlobalTable(a: DescribeGlobalTableRequest): Kleisli[M, E, DescribeGlobalTableResponse]                                                 = Kleisli(e => eff1(f(e).describeGlobalTable(a)))
        override def describeGlobalTableSettings(a: DescribeGlobalTableSettingsRequest): Kleisli[M, E, DescribeGlobalTableSettingsResponse]                         = Kleisli(e => eff1(f(e).describeGlobalTableSettings(a)))
        override def describeImport(a: DescribeImportRequest): Kleisli[M, E, DescribeImportResponse]                                                                = Kleisli(e => eff1(f(e).describeImport(a)))
        override def describeKinesisStreamingDestination(a: DescribeKinesisStreamingDestinationRequest): Kleisli[M, E, DescribeKinesisStreamingDestinationResponse] = Kleisli(e => eff1(f(e).describeKinesisStreamingDestination(a)))
        override def describeLimits: Kleisli[M, E, DescribeLimitsResponse]                                                                                          = Kleisli(e => eff1(f(e).describeLimits))
        override def describeLimits(a: DescribeLimitsRequest): Kleisli[M, E, DescribeLimitsResponse]                                                                = Kleisli(e => eff1(f(e).describeLimits(a)))
        override def describeTable(a: DescribeTableRequest): Kleisli[M, E, DescribeTableResponse]                                                                   = Kleisli(e => eff1(f(e).describeTable(a)))
        override def describeTableReplicaAutoScaling(a: DescribeTableReplicaAutoScalingRequest): Kleisli[M, E, DescribeTableReplicaAutoScalingResponse]             = Kleisli(e => eff1(f(e).describeTableReplicaAutoScaling(a)))
        override def describeTimeToLive(a: DescribeTimeToLiveRequest): Kleisli[M, E, DescribeTimeToLiveResponse]                                                    = Kleisli(e => eff1(f(e).describeTimeToLive(a)))
        override def disableKinesisStreamingDestination(a: DisableKinesisStreamingDestinationRequest): Kleisli[M, E, DisableKinesisStreamingDestinationResponse]    = Kleisli(e => eff1(f(e).disableKinesisStreamingDestination(a)))
        override def enableKinesisStreamingDestination(a: EnableKinesisStreamingDestinationRequest): Kleisli[M, E, EnableKinesisStreamingDestinationResponse]       = Kleisli(e => eff1(f(e).enableKinesisStreamingDestination(a)))
        override def executeStatement(a: ExecuteStatementRequest): Kleisli[M, E, ExecuteStatementResponse]                                                          = Kleisli(e => eff1(f(e).executeStatement(a)))
        override def executeTransaction(a: ExecuteTransactionRequest): Kleisli[M, E, ExecuteTransactionResponse]                                                    = Kleisli(e => eff1(f(e).executeTransaction(a)))
        override def exportTableToPointInTime(a: ExportTableToPointInTimeRequest): Kleisli[M, E, ExportTableToPointInTimeResponse]                                  = Kleisli(e => eff1(f(e).exportTableToPointInTime(a)))
        override def getItem(a: GetItemRequest): Kleisli[M, E, GetItemResponse]                                                                                     = Kleisli(e => eff1(f(e).getItem(a)))
        override def getResourcePolicy(a: GetResourcePolicyRequest): Kleisli[M, E, GetResourcePolicyResponse]                                                       = Kleisli(e => eff1(f(e).getResourcePolicy(a)))
        override def importTable(a: ImportTableRequest): Kleisli[M, E, ImportTableResponse]                                                                         = Kleisli(e => eff1(f(e).importTable(a)))
        override def listBackups: Kleisli[M, E, ListBackupsResponse]                                                                                                = Kleisli(e => eff1(f(e).listBackups))
        override def listBackups(a: ListBackupsRequest): Kleisli[M, E, ListBackupsResponse]                                                                         = Kleisli(e => eff1(f(e).listBackups(a)))
        override def listContributorInsights(a: ListContributorInsightsRequest): Kleisli[M, E, ListContributorInsightsResponse]                                     = Kleisli(e => eff1(f(e).listContributorInsights(a)))
        override def listContributorInsightsPaginator(a: ListContributorInsightsRequest): Kleisli[M, E, ListContributorInsightsPublisher]                           = Kleisli(e => primitive1(f(e).listContributorInsightsPaginator(a)))
        override def listExports(a: ListExportsRequest): Kleisli[M, E, ListExportsResponse]                                                                         = Kleisli(e => eff1(f(e).listExports(a)))
        override def listExportsPaginator(a: ListExportsRequest): Kleisli[M, E, ListExportsPublisher]                                                               = Kleisli(e => primitive1(f(e).listExportsPaginator(a)))
        override def listGlobalTables: Kleisli[M, E, ListGlobalTablesResponse]                                                                                      = Kleisli(e => eff1(f(e).listGlobalTables))
        override def listGlobalTables(a: ListGlobalTablesRequest): Kleisli[M, E, ListGlobalTablesResponse]                                                          = Kleisli(e => eff1(f(e).listGlobalTables(a)))
        override def listImports(a: ListImportsRequest): Kleisli[M, E, ListImportsResponse]                                                                         = Kleisli(e => eff1(f(e).listImports(a)))
        override def listImportsPaginator(a: ListImportsRequest): Kleisli[M, E, ListImportsPublisher]                                                               = Kleisli(e => primitive1(f(e).listImportsPaginator(a)))
        override def listTables: Kleisli[M, E, ListTablesResponse]                                                                                                  = Kleisli(e => eff1(f(e).listTables))
        override def listTables(a: ListTablesRequest): Kleisli[M, E, ListTablesResponse]                                                                            = Kleisli(e => eff1(f(e).listTables(a)))
        override def listTablesPaginator: Kleisli[M, E, ListTablesPublisher]                                                                                        = Kleisli(e => primitive1(f(e).listTablesPaginator))
        override def listTablesPaginator(a: ListTablesRequest): Kleisli[M, E, ListTablesPublisher]                                                                  = Kleisli(e => primitive1(f(e).listTablesPaginator(a)))
        override def listTagsOfResource(a: ListTagsOfResourceRequest): Kleisli[M, E, ListTagsOfResourceResponse]                                                    = Kleisli(e => eff1(f(e).listTagsOfResource(a)))
        override def putItem(a: PutItemRequest): Kleisli[M, E, PutItemResponse]                                                                                     = Kleisli(e => eff1(f(e).putItem(a)))
        override def putResourcePolicy(a: PutResourcePolicyRequest): Kleisli[M, E, PutResourcePolicyResponse]                                                       = Kleisli(e => eff1(f(e).putResourcePolicy(a)))
        override def query(a: QueryRequest): Kleisli[M, E, QueryResponse]                                                                                           = Kleisli(e => eff1(f(e).query(a)))
        override def queryPaginator(a: QueryRequest): Kleisli[M, E, QueryPublisher]                                                                                 = Kleisli(e => primitive1(f(e).queryPaginator(a)))
        override def restoreTableFromBackup(a: RestoreTableFromBackupRequest): Kleisli[M, E, RestoreTableFromBackupResponse]                                        = Kleisli(e => eff1(f(e).restoreTableFromBackup(a)))
        override def restoreTableToPointInTime(a: RestoreTableToPointInTimeRequest): Kleisli[M, E, RestoreTableToPointInTimeResponse]                               = Kleisli(e => eff1(f(e).restoreTableToPointInTime(a)))
        override def scan(a: ScanRequest): Kleisli[M, E, ScanResponse]                                                                                              = Kleisli(e => eff1(f(e).scan(a)))
        override def scanPaginator(a: ScanRequest): Kleisli[M, E, ScanPublisher]                                                                                    = Kleisli(e => primitive1(f(e).scanPaginator(a)))
        override def serviceName: Kleisli[M, E, String]                                                                                                             = Kleisli(e => primitive1(f(e).serviceName))
        override def tagResource(a: TagResourceRequest): Kleisli[M, E, TagResourceResponse]                                                                         = Kleisli(e => eff1(f(e).tagResource(a)))
        override def transactGetItems(a: TransactGetItemsRequest): Kleisli[M, E, TransactGetItemsResponse]                                                          = Kleisli(e => eff1(f(e).transactGetItems(a)))
        override def transactWriteItems(a: TransactWriteItemsRequest): Kleisli[M, E, TransactWriteItemsResponse]                                                    = Kleisli(e => eff1(f(e).transactWriteItems(a)))
        override def untagResource(a: UntagResourceRequest): Kleisli[M, E, UntagResourceResponse]                                                                   = Kleisli(e => eff1(f(e).untagResource(a)))
        override def updateContinuousBackups(a: UpdateContinuousBackupsRequest): Kleisli[M, E, UpdateContinuousBackupsResponse]                                     = Kleisli(e => eff1(f(e).updateContinuousBackups(a)))
        override def updateContributorInsights(a: UpdateContributorInsightsRequest): Kleisli[M, E, UpdateContributorInsightsResponse]                               = Kleisli(e => eff1(f(e).updateContributorInsights(a)))
        override def updateGlobalTable(a: UpdateGlobalTableRequest): Kleisli[M, E, UpdateGlobalTableResponse]                                                       = Kleisli(e => eff1(f(e).updateGlobalTable(a)))
        override def updateGlobalTableSettings(a: UpdateGlobalTableSettingsRequest): Kleisli[M, E, UpdateGlobalTableSettingsResponse]                               = Kleisli(e => eff1(f(e).updateGlobalTableSettings(a)))
        override def updateItem(a: UpdateItemRequest): Kleisli[M, E, UpdateItemResponse]                                                                            = Kleisli(e => eff1(f(e).updateItem(a)))
        override def updateKinesisStreamingDestination(a: UpdateKinesisStreamingDestinationRequest): Kleisli[M, E, UpdateKinesisStreamingDestinationResponse]       = Kleisli(e => eff1(f(e).updateKinesisStreamingDestination(a)))
        override def updateTable(a: UpdateTableRequest): Kleisli[M, E, UpdateTableResponse]                                                                         = Kleisli(e => eff1(f(e).updateTable(a)))
        override def updateTableReplicaAutoScaling(a: UpdateTableReplicaAutoScalingRequest): Kleisli[M, E, UpdateTableReplicaAutoScalingResponse]                   = Kleisli(e => eff1(f(e).updateTableReplicaAutoScaling(a)))
        override def updateTimeToLive(a: UpdateTimeToLiveRequest): Kleisli[M, E, UpdateTimeToLiveResponse]                                                          = Kleisli(e => eff1(f(e).updateTimeToLive(a)))
        override def waiter: Kleisli[M, E, DynamoDbAsyncWaiter]                                                                                                     = Kleisli(e => primitive1(f(e).waiter))
      }
  }
  // end interpreters

  def DynamoDbAsyncClientResource(builder: DynamoDbAsyncClientBuilder): Resource[M, DynamoDbAsyncClient]        = Resource.fromAutoCloseable(asyncM.delay(builder.build()))
  def DynamoDbAsyncClientOpResource(builder: DynamoDbAsyncClientBuilder): Resource[M, DynamoDbAsyncClientOp[M]] = DynamoDbAsyncClientResource(builder).map(create)

  def create(client: DynamoDbAsyncClient): DynamoDbAsyncClientOp[M] = new DynamoDbAsyncClientOp[M] {

    // domain-specific operations are implemented in terms of `primitive`
    override def batchExecuteStatement(a: BatchExecuteStatementRequest): M[BatchExecuteStatementResponse]                                           = eff1(client.batchExecuteStatement(a))
    override def batchGetItem(a: BatchGetItemRequest): M[BatchGetItemResponse]                                                                      = eff1(client.batchGetItem(a))
    override def batchGetItemPaginator(a: BatchGetItemRequest): M[BatchGetItemPublisher]                                                            = primitive1(client.batchGetItemPaginator(a))
    override def batchWriteItem(a: BatchWriteItemRequest): M[BatchWriteItemResponse]                                                                = eff1(client.batchWriteItem(a))
    override def close: M[Unit]                                                                                                                     = primitive1(client.close)
    override def createBackup(a: CreateBackupRequest): M[CreateBackupResponse]                                                                      = eff1(client.createBackup(a))
    override def createGlobalTable(a: CreateGlobalTableRequest): M[CreateGlobalTableResponse]                                                       = eff1(client.createGlobalTable(a))
    override def createTable(a: CreateTableRequest): M[CreateTableResponse]                                                                         = eff1(client.createTable(a))
    override def deleteBackup(a: DeleteBackupRequest): M[DeleteBackupResponse]                                                                      = eff1(client.deleteBackup(a))
    override def deleteItem(a: DeleteItemRequest): M[DeleteItemResponse]                                                                            = eff1(client.deleteItem(a))
    override def deleteResourcePolicy(a: DeleteResourcePolicyRequest): M[DeleteResourcePolicyResponse]                                              = eff1(client.deleteResourcePolicy(a))
    override def deleteTable(a: DeleteTableRequest): M[DeleteTableResponse]                                                                         = eff1(client.deleteTable(a))
    override def describeBackup(a: DescribeBackupRequest): M[DescribeBackupResponse]                                                                = eff1(client.describeBackup(a))
    override def describeContinuousBackups(a: DescribeContinuousBackupsRequest): M[DescribeContinuousBackupsResponse]                               = eff1(client.describeContinuousBackups(a))
    override def describeContributorInsights(a: DescribeContributorInsightsRequest): M[DescribeContributorInsightsResponse]                         = eff1(client.describeContributorInsights(a))
    override def describeEndpoints: M[DescribeEndpointsResponse]                                                                                    = eff1(client.describeEndpoints)
    override def describeEndpoints(a: DescribeEndpointsRequest): M[DescribeEndpointsResponse]                                                       = eff1(client.describeEndpoints(a))
    override def describeExport(a: DescribeExportRequest): M[DescribeExportResponse]                                                                = eff1(client.describeExport(a))
    override def describeGlobalTable(a: DescribeGlobalTableRequest): M[DescribeGlobalTableResponse]                                                 = eff1(client.describeGlobalTable(a))
    override def describeGlobalTableSettings(a: DescribeGlobalTableSettingsRequest): M[DescribeGlobalTableSettingsResponse]                         = eff1(client.describeGlobalTableSettings(a))
    override def describeImport(a: DescribeImportRequest): M[DescribeImportResponse]                                                                = eff1(client.describeImport(a))
    override def describeKinesisStreamingDestination(a: DescribeKinesisStreamingDestinationRequest): M[DescribeKinesisStreamingDestinationResponse] = eff1(client.describeKinesisStreamingDestination(a))
    override def describeLimits: M[DescribeLimitsResponse]                                                                                          = eff1(client.describeLimits)
    override def describeLimits(a: DescribeLimitsRequest): M[DescribeLimitsResponse]                                                                = eff1(client.describeLimits(a))
    override def describeTable(a: DescribeTableRequest): M[DescribeTableResponse]                                                                   = eff1(client.describeTable(a))
    override def describeTableReplicaAutoScaling(a: DescribeTableReplicaAutoScalingRequest): M[DescribeTableReplicaAutoScalingResponse]             = eff1(client.describeTableReplicaAutoScaling(a))
    override def describeTimeToLive(a: DescribeTimeToLiveRequest): M[DescribeTimeToLiveResponse]                                                    = eff1(client.describeTimeToLive(a))
    override def disableKinesisStreamingDestination(a: DisableKinesisStreamingDestinationRequest): M[DisableKinesisStreamingDestinationResponse]    = eff1(client.disableKinesisStreamingDestination(a))
    override def enableKinesisStreamingDestination(a: EnableKinesisStreamingDestinationRequest): M[EnableKinesisStreamingDestinationResponse]       = eff1(client.enableKinesisStreamingDestination(a))
    override def executeStatement(a: ExecuteStatementRequest): M[ExecuteStatementResponse]                                                          = eff1(client.executeStatement(a))
    override def executeTransaction(a: ExecuteTransactionRequest): M[ExecuteTransactionResponse]                                                    = eff1(client.executeTransaction(a))
    override def exportTableToPointInTime(a: ExportTableToPointInTimeRequest): M[ExportTableToPointInTimeResponse]                                  = eff1(client.exportTableToPointInTime(a))
    override def getItem(a: GetItemRequest): M[GetItemResponse]                                                                                     = eff1(client.getItem(a))
    override def getResourcePolicy(a: GetResourcePolicyRequest): M[GetResourcePolicyResponse]                                                       = eff1(client.getResourcePolicy(a))
    override def importTable(a: ImportTableRequest): M[ImportTableResponse]                                                                         = eff1(client.importTable(a))
    override def listBackups: M[ListBackupsResponse]                                                                                                = eff1(client.listBackups)
    override def listBackups(a: ListBackupsRequest): M[ListBackupsResponse]                                                                         = eff1(client.listBackups(a))
    override def listContributorInsights(a: ListContributorInsightsRequest): M[ListContributorInsightsResponse]                                     = eff1(client.listContributorInsights(a))
    override def listContributorInsightsPaginator(a: ListContributorInsightsRequest): M[ListContributorInsightsPublisher]                           = primitive1(client.listContributorInsightsPaginator(a))
    override def listExports(a: ListExportsRequest): M[ListExportsResponse]                                                                         = eff1(client.listExports(a))
    override def listExportsPaginator(a: ListExportsRequest): M[ListExportsPublisher]                                                               = primitive1(client.listExportsPaginator(a))
    override def listGlobalTables: M[ListGlobalTablesResponse]                                                                                      = eff1(client.listGlobalTables)
    override def listGlobalTables(a: ListGlobalTablesRequest): M[ListGlobalTablesResponse]                                                          = eff1(client.listGlobalTables(a))
    override def listImports(a: ListImportsRequest): M[ListImportsResponse]                                                                         = eff1(client.listImports(a))
    override def listImportsPaginator(a: ListImportsRequest): M[ListImportsPublisher]                                                               = primitive1(client.listImportsPaginator(a))
    override def listTables: M[ListTablesResponse]                                                                                                  = eff1(client.listTables)
    override def listTables(a: ListTablesRequest): M[ListTablesResponse]                                                                            = eff1(client.listTables(a))
    override def listTablesPaginator: M[ListTablesPublisher]                                                                                        = primitive1(client.listTablesPaginator)
    override def listTablesPaginator(a: ListTablesRequest): M[ListTablesPublisher]                                                                  = primitive1(client.listTablesPaginator(a))
    override def listTagsOfResource(a: ListTagsOfResourceRequest): M[ListTagsOfResourceResponse]                                                    = eff1(client.listTagsOfResource(a))
    override def putItem(a: PutItemRequest): M[PutItemResponse]                                                                                     = eff1(client.putItem(a))
    override def putResourcePolicy(a: PutResourcePolicyRequest): M[PutResourcePolicyResponse]                                                       = eff1(client.putResourcePolicy(a))
    override def query(a: QueryRequest): M[QueryResponse]                                                                                           = eff1(client.query(a))
    override def queryPaginator(a: QueryRequest): M[QueryPublisher]                                                                                 = primitive1(client.queryPaginator(a))
    override def restoreTableFromBackup(a: RestoreTableFromBackupRequest): M[RestoreTableFromBackupResponse]                                        = eff1(client.restoreTableFromBackup(a))
    override def restoreTableToPointInTime(a: RestoreTableToPointInTimeRequest): M[RestoreTableToPointInTimeResponse]                               = eff1(client.restoreTableToPointInTime(a))
    override def scan(a: ScanRequest): M[ScanResponse]                                                                                              = eff1(client.scan(a))
    override def scanPaginator(a: ScanRequest): M[ScanPublisher]                                                                                    = primitive1(client.scanPaginator(a))
    override def serviceName: M[String]                                                                                                             = primitive1(client.serviceName)
    override def tagResource(a: TagResourceRequest): M[TagResourceResponse]                                                                         = eff1(client.tagResource(a))
    override def transactGetItems(a: TransactGetItemsRequest): M[TransactGetItemsResponse]                                                          = eff1(client.transactGetItems(a))
    override def transactWriteItems(a: TransactWriteItemsRequest): M[TransactWriteItemsResponse]                                                    = eff1(client.transactWriteItems(a))
    override def untagResource(a: UntagResourceRequest): M[UntagResourceResponse]                                                                   = eff1(client.untagResource(a))
    override def updateContinuousBackups(a: UpdateContinuousBackupsRequest): M[UpdateContinuousBackupsResponse]                                     = eff1(client.updateContinuousBackups(a))
    override def updateContributorInsights(a: UpdateContributorInsightsRequest): M[UpdateContributorInsightsResponse]                               = eff1(client.updateContributorInsights(a))
    override def updateGlobalTable(a: UpdateGlobalTableRequest): M[UpdateGlobalTableResponse]                                                       = eff1(client.updateGlobalTable(a))
    override def updateGlobalTableSettings(a: UpdateGlobalTableSettingsRequest): M[UpdateGlobalTableSettingsResponse]                               = eff1(client.updateGlobalTableSettings(a))
    override def updateItem(a: UpdateItemRequest): M[UpdateItemResponse]                                                                            = eff1(client.updateItem(a))
    override def updateKinesisStreamingDestination(a: UpdateKinesisStreamingDestinationRequest): M[UpdateKinesisStreamingDestinationResponse]       = eff1(client.updateKinesisStreamingDestination(a))
    override def updateTable(a: UpdateTableRequest): M[UpdateTableResponse]                                                                         = eff1(client.updateTable(a))
    override def updateTableReplicaAutoScaling(a: UpdateTableReplicaAutoScalingRequest): M[UpdateTableReplicaAutoScalingResponse]                   = eff1(client.updateTableReplicaAutoScaling(a))
    override def updateTimeToLive(a: UpdateTimeToLiveRequest): M[UpdateTimeToLiveResponse]                                                          = eff1(client.updateTimeToLive(a))
    override def waiter: M[DynamoDbAsyncWaiter]                                                                                                     = primitive1(client.waiter)

  }

}
