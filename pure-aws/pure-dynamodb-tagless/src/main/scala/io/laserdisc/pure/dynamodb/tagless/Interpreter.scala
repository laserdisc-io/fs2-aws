package io.laserdisc.pure.dynamodb.tagless

// Library imports
import cats.data.Kleisli
import cats.effect.{ Async,  Resource }

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

  lazy val DynamoDbAsyncClientInterpreter: DynamoDbAsyncClientInterpreter = new DynamoDbAsyncClientInterpreter { }
  // Some methods are common to all interpreters and can be overridden to change behavior globally.

  def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(j => asyncM.blocking(f(j)))
  def primitive1[J, A](f: => A): M[A]              = asyncM.blocking(f)

  def eff[J, A](fut: J => CompletableFuture[A]): Kleisli[M, J, A] = Kleisli { j =>
    asyncM.fromCompletableFuture(asyncM.delay(fut(j)))
  }
  def eff1[J, A](fut: => CompletableFuture[A]): M[A] =
    asyncM.fromCompletableFuture(asyncM.delay(fut))

  // Interpreters
  trait DynamoDbAsyncClientInterpreter extends DynamoDbAsyncClientOp[Kleisli[M, DynamoDbAsyncClient, *]] {
  
    // domain-specific operations are implemented in terms of `primitive`
    override def batchExecuteStatement(a: BatchExecuteStatementRequest): Kleisli[M, DynamoDbAsyncClient, BatchExecuteStatementResponse] = eff(_.batchExecuteStatement(a)) // B
    override def batchGetItem(a: BatchGetItemRequest): Kleisli[M, DynamoDbAsyncClient, BatchGetItemResponse] = eff(_.batchGetItem(a)) // B
    override def batchGetItemPaginator(a: BatchGetItemRequest): Kleisli[M, DynamoDbAsyncClient, BatchGetItemPublisher] = primitive(_.batchGetItemPaginator(a)) // B
    override def batchWriteItem(a: BatchWriteItemRequest): Kleisli[M, DynamoDbAsyncClient, BatchWriteItemResponse] = eff(_.batchWriteItem(a)) // B
    override def close : Kleisli[M, DynamoDbAsyncClient, Unit] = primitive(_.close) // A
    override def createBackup(a: CreateBackupRequest): Kleisli[M, DynamoDbAsyncClient, CreateBackupResponse] = eff(_.createBackup(a)) // B
    override def createGlobalTable(a: CreateGlobalTableRequest): Kleisli[M, DynamoDbAsyncClient, CreateGlobalTableResponse] = eff(_.createGlobalTable(a)) // B
    override def createTable(a: CreateTableRequest): Kleisli[M, DynamoDbAsyncClient, CreateTableResponse] = eff(_.createTable(a)) // B
    override def deleteBackup(a: DeleteBackupRequest): Kleisli[M, DynamoDbAsyncClient, DeleteBackupResponse] = eff(_.deleteBackup(a)) // B
    override def deleteItem(a: DeleteItemRequest): Kleisli[M, DynamoDbAsyncClient, DeleteItemResponse] = eff(_.deleteItem(a)) // B
    override def deleteResourcePolicy(a: DeleteResourcePolicyRequest): Kleisli[M, DynamoDbAsyncClient, DeleteResourcePolicyResponse] = eff(_.deleteResourcePolicy(a)) // B
    override def deleteTable(a: DeleteTableRequest): Kleisli[M, DynamoDbAsyncClient, DeleteTableResponse] = eff(_.deleteTable(a)) // B
    override def describeBackup(a: DescribeBackupRequest): Kleisli[M, DynamoDbAsyncClient, DescribeBackupResponse] = eff(_.describeBackup(a)) // B
    override def describeContinuousBackups(a: DescribeContinuousBackupsRequest): Kleisli[M, DynamoDbAsyncClient, DescribeContinuousBackupsResponse] = eff(_.describeContinuousBackups(a)) // B
    override def describeContributorInsights(a: DescribeContributorInsightsRequest): Kleisli[M, DynamoDbAsyncClient, DescribeContributorInsightsResponse] = eff(_.describeContributorInsights(a)) // B
    override def describeEndpoints : Kleisli[M, DynamoDbAsyncClient, DescribeEndpointsResponse] = eff(_.describeEndpoints) // A
    override def describeEndpoints(a: DescribeEndpointsRequest): Kleisli[M, DynamoDbAsyncClient, DescribeEndpointsResponse] = eff(_.describeEndpoints(a)) // B
    override def describeExport(a: DescribeExportRequest): Kleisli[M, DynamoDbAsyncClient, DescribeExportResponse] = eff(_.describeExport(a)) // B
    override def describeGlobalTable(a: DescribeGlobalTableRequest): Kleisli[M, DynamoDbAsyncClient, DescribeGlobalTableResponse] = eff(_.describeGlobalTable(a)) // B
    override def describeGlobalTableSettings(a: DescribeGlobalTableSettingsRequest): Kleisli[M, DynamoDbAsyncClient, DescribeGlobalTableSettingsResponse] = eff(_.describeGlobalTableSettings(a)) // B
    override def describeImport(a: DescribeImportRequest): Kleisli[M, DynamoDbAsyncClient, DescribeImportResponse] = eff(_.describeImport(a)) // B
    override def describeKinesisStreamingDestination(a: DescribeKinesisStreamingDestinationRequest): Kleisli[M, DynamoDbAsyncClient, DescribeKinesisStreamingDestinationResponse] = eff(_.describeKinesisStreamingDestination(a)) // B
    override def describeLimits : Kleisli[M, DynamoDbAsyncClient, DescribeLimitsResponse] = eff(_.describeLimits) // A
    override def describeLimits(a: DescribeLimitsRequest): Kleisli[M, DynamoDbAsyncClient, DescribeLimitsResponse] = eff(_.describeLimits(a)) // B
    override def describeTable(a: DescribeTableRequest): Kleisli[M, DynamoDbAsyncClient, DescribeTableResponse] = eff(_.describeTable(a)) // B
    override def describeTableReplicaAutoScaling(a: DescribeTableReplicaAutoScalingRequest): Kleisli[M, DynamoDbAsyncClient, DescribeTableReplicaAutoScalingResponse] = eff(_.describeTableReplicaAutoScaling(a)) // B
    override def describeTimeToLive(a: DescribeTimeToLiveRequest): Kleisli[M, DynamoDbAsyncClient, DescribeTimeToLiveResponse] = eff(_.describeTimeToLive(a)) // B
    override def disableKinesisStreamingDestination(a: DisableKinesisStreamingDestinationRequest): Kleisli[M, DynamoDbAsyncClient, DisableKinesisStreamingDestinationResponse] = eff(_.disableKinesisStreamingDestination(a)) // B
    override def enableKinesisStreamingDestination(a: EnableKinesisStreamingDestinationRequest): Kleisli[M, DynamoDbAsyncClient, EnableKinesisStreamingDestinationResponse] = eff(_.enableKinesisStreamingDestination(a)) // B
    override def executeStatement(a: ExecuteStatementRequest): Kleisli[M, DynamoDbAsyncClient, ExecuteStatementResponse] = eff(_.executeStatement(a)) // B
    override def executeTransaction(a: ExecuteTransactionRequest): Kleisli[M, DynamoDbAsyncClient, ExecuteTransactionResponse] = eff(_.executeTransaction(a)) // B
    override def exportTableToPointInTime(a: ExportTableToPointInTimeRequest): Kleisli[M, DynamoDbAsyncClient, ExportTableToPointInTimeResponse] = eff(_.exportTableToPointInTime(a)) // B
    override def getItem(a: GetItemRequest): Kleisli[M, DynamoDbAsyncClient, GetItemResponse] = eff(_.getItem(a)) // B
    override def getResourcePolicy(a: GetResourcePolicyRequest): Kleisli[M, DynamoDbAsyncClient, GetResourcePolicyResponse] = eff(_.getResourcePolicy(a)) // B
    override def importTable(a: ImportTableRequest): Kleisli[M, DynamoDbAsyncClient, ImportTableResponse] = eff(_.importTable(a)) // B
    override def listBackups : Kleisli[M, DynamoDbAsyncClient, ListBackupsResponse] = eff(_.listBackups) // A
    override def listBackups(a: ListBackupsRequest): Kleisli[M, DynamoDbAsyncClient, ListBackupsResponse] = eff(_.listBackups(a)) // B
    override def listContributorInsights(a: ListContributorInsightsRequest): Kleisli[M, DynamoDbAsyncClient, ListContributorInsightsResponse] = eff(_.listContributorInsights(a)) // B
    override def listContributorInsightsPaginator(a: ListContributorInsightsRequest): Kleisli[M, DynamoDbAsyncClient, ListContributorInsightsPublisher] = primitive(_.listContributorInsightsPaginator(a)) // B
    override def listExports(a: ListExportsRequest): Kleisli[M, DynamoDbAsyncClient, ListExportsResponse] = eff(_.listExports(a)) // B
    override def listExportsPaginator(a: ListExportsRequest): Kleisli[M, DynamoDbAsyncClient, ListExportsPublisher] = primitive(_.listExportsPaginator(a)) // B
    override def listGlobalTables : Kleisli[M, DynamoDbAsyncClient, ListGlobalTablesResponse] = eff(_.listGlobalTables) // A
    override def listGlobalTables(a: ListGlobalTablesRequest): Kleisli[M, DynamoDbAsyncClient, ListGlobalTablesResponse] = eff(_.listGlobalTables(a)) // B
    override def listImports(a: ListImportsRequest): Kleisli[M, DynamoDbAsyncClient, ListImportsResponse] = eff(_.listImports(a)) // B
    override def listImportsPaginator(a: ListImportsRequest): Kleisli[M, DynamoDbAsyncClient, ListImportsPublisher] = primitive(_.listImportsPaginator(a)) // B
    override def listTables : Kleisli[M, DynamoDbAsyncClient, ListTablesResponse] = eff(_.listTables) // A
    override def listTables(a: ListTablesRequest): Kleisli[M, DynamoDbAsyncClient, ListTablesResponse] = eff(_.listTables(a)) // B
    override def listTablesPaginator : Kleisli[M, DynamoDbAsyncClient, ListTablesPublisher] = primitive(_.listTablesPaginator) // A
    override def listTablesPaginator(a: ListTablesRequest): Kleisli[M, DynamoDbAsyncClient, ListTablesPublisher] = primitive(_.listTablesPaginator(a)) // B
    override def listTagsOfResource(a: ListTagsOfResourceRequest): Kleisli[M, DynamoDbAsyncClient, ListTagsOfResourceResponse] = eff(_.listTagsOfResource(a)) // B
    override def putItem(a: PutItemRequest): Kleisli[M, DynamoDbAsyncClient, PutItemResponse] = eff(_.putItem(a)) // B
    override def putResourcePolicy(a: PutResourcePolicyRequest): Kleisli[M, DynamoDbAsyncClient, PutResourcePolicyResponse] = eff(_.putResourcePolicy(a)) // B
    override def query(a: QueryRequest): Kleisli[M, DynamoDbAsyncClient, QueryResponse] = eff(_.query(a)) // B
    override def queryPaginator(a: QueryRequest): Kleisli[M, DynamoDbAsyncClient, QueryPublisher] = primitive(_.queryPaginator(a)) // B
    override def restoreTableFromBackup(a: RestoreTableFromBackupRequest): Kleisli[M, DynamoDbAsyncClient, RestoreTableFromBackupResponse] = eff(_.restoreTableFromBackup(a)) // B
    override def restoreTableToPointInTime(a: RestoreTableToPointInTimeRequest): Kleisli[M, DynamoDbAsyncClient, RestoreTableToPointInTimeResponse] = eff(_.restoreTableToPointInTime(a)) // B
    override def scan(a: ScanRequest): Kleisli[M, DynamoDbAsyncClient, ScanResponse] = eff(_.scan(a)) // B
    override def scanPaginator(a: ScanRequest): Kleisli[M, DynamoDbAsyncClient, ScanPublisher] = primitive(_.scanPaginator(a)) // B
    override def serviceName : Kleisli[M, DynamoDbAsyncClient, String] = primitive(_.serviceName) // A
    override def tagResource(a: TagResourceRequest): Kleisli[M, DynamoDbAsyncClient, TagResourceResponse] = eff(_.tagResource(a)) // B
    override def transactGetItems(a: TransactGetItemsRequest): Kleisli[M, DynamoDbAsyncClient, TransactGetItemsResponse] = eff(_.transactGetItems(a)) // B
    override def transactWriteItems(a: TransactWriteItemsRequest): Kleisli[M, DynamoDbAsyncClient, TransactWriteItemsResponse] = eff(_.transactWriteItems(a)) // B
    override def untagResource(a: UntagResourceRequest): Kleisli[M, DynamoDbAsyncClient, UntagResourceResponse] = eff(_.untagResource(a)) // B
    override def updateContinuousBackups(a: UpdateContinuousBackupsRequest): Kleisli[M, DynamoDbAsyncClient, UpdateContinuousBackupsResponse] = eff(_.updateContinuousBackups(a)) // B
    override def updateContributorInsights(a: UpdateContributorInsightsRequest): Kleisli[M, DynamoDbAsyncClient, UpdateContributorInsightsResponse] = eff(_.updateContributorInsights(a)) // B
    override def updateGlobalTable(a: UpdateGlobalTableRequest): Kleisli[M, DynamoDbAsyncClient, UpdateGlobalTableResponse] = eff(_.updateGlobalTable(a)) // B
    override def updateGlobalTableSettings(a: UpdateGlobalTableSettingsRequest): Kleisli[M, DynamoDbAsyncClient, UpdateGlobalTableSettingsResponse] = eff(_.updateGlobalTableSettings(a)) // B
    override def updateItem(a: UpdateItemRequest): Kleisli[M, DynamoDbAsyncClient, UpdateItemResponse] = eff(_.updateItem(a)) // B
    override def updateKinesisStreamingDestination(a: UpdateKinesisStreamingDestinationRequest): Kleisli[M, DynamoDbAsyncClient, UpdateKinesisStreamingDestinationResponse] = eff(_.updateKinesisStreamingDestination(a)) // B
    override def updateTable(a: UpdateTableRequest): Kleisli[M, DynamoDbAsyncClient, UpdateTableResponse] = eff(_.updateTable(a)) // B
    override def updateTableReplicaAutoScaling(a: UpdateTableReplicaAutoScalingRequest): Kleisli[M, DynamoDbAsyncClient, UpdateTableReplicaAutoScalingResponse] = eff(_.updateTableReplicaAutoScaling(a)) // B
    override def updateTimeToLive(a: UpdateTimeToLiveRequest): Kleisli[M, DynamoDbAsyncClient, UpdateTimeToLiveResponse] = eff(_.updateTimeToLive(a)) // B
    override def waiter : Kleisli[M, DynamoDbAsyncClient, DynamoDbAsyncWaiter] = primitive(_.waiter) // A
  
  
    def lens[E](f: E => DynamoDbAsyncClient): DynamoDbAsyncClientOp[Kleisli[M, E, *]] =
      new DynamoDbAsyncClientOp[Kleisli[M, E, *]] {
      override def batchExecuteStatement(a: BatchExecuteStatementRequest) = Kleisli(e => eff1(f(e).batchExecuteStatement(a)))
      override def batchGetItem(a: BatchGetItemRequest) = Kleisli(e => eff1(f(e).batchGetItem(a)))
      override def batchGetItemPaginator(a: BatchGetItemRequest) = Kleisli(e => primitive1(f(e).batchGetItemPaginator(a)))
      override def batchWriteItem(a: BatchWriteItemRequest) = Kleisli(e => eff1(f(e).batchWriteItem(a)))
      override def close = Kleisli(e => primitive1(f(e).close))
      override def createBackup(a: CreateBackupRequest) = Kleisli(e => eff1(f(e).createBackup(a)))
      override def createGlobalTable(a: CreateGlobalTableRequest) = Kleisli(e => eff1(f(e).createGlobalTable(a)))
      override def createTable(a: CreateTableRequest) = Kleisli(e => eff1(f(e).createTable(a)))
      override def deleteBackup(a: DeleteBackupRequest) = Kleisli(e => eff1(f(e).deleteBackup(a)))
      override def deleteItem(a: DeleteItemRequest) = Kleisli(e => eff1(f(e).deleteItem(a)))
      override def deleteResourcePolicy(a: DeleteResourcePolicyRequest) = Kleisli(e => eff1(f(e).deleteResourcePolicy(a)))
      override def deleteTable(a: DeleteTableRequest) = Kleisli(e => eff1(f(e).deleteTable(a)))
      override def describeBackup(a: DescribeBackupRequest) = Kleisli(e => eff1(f(e).describeBackup(a)))
      override def describeContinuousBackups(a: DescribeContinuousBackupsRequest) = Kleisli(e => eff1(f(e).describeContinuousBackups(a)))
      override def describeContributorInsights(a: DescribeContributorInsightsRequest) = Kleisli(e => eff1(f(e).describeContributorInsights(a)))
      override def describeEndpoints = Kleisli(e => eff1(f(e).describeEndpoints))
      override def describeEndpoints(a: DescribeEndpointsRequest) = Kleisli(e => eff1(f(e).describeEndpoints(a)))
      override def describeExport(a: DescribeExportRequest) = Kleisli(e => eff1(f(e).describeExport(a)))
      override def describeGlobalTable(a: DescribeGlobalTableRequest) = Kleisli(e => eff1(f(e).describeGlobalTable(a)))
      override def describeGlobalTableSettings(a: DescribeGlobalTableSettingsRequest) = Kleisli(e => eff1(f(e).describeGlobalTableSettings(a)))
      override def describeImport(a: DescribeImportRequest) = Kleisli(e => eff1(f(e).describeImport(a)))
      override def describeKinesisStreamingDestination(a: DescribeKinesisStreamingDestinationRequest) = Kleisli(e => eff1(f(e).describeKinesisStreamingDestination(a)))
      override def describeLimits = Kleisli(e => eff1(f(e).describeLimits))
      override def describeLimits(a: DescribeLimitsRequest) = Kleisli(e => eff1(f(e).describeLimits(a)))
      override def describeTable(a: DescribeTableRequest) = Kleisli(e => eff1(f(e).describeTable(a)))
      override def describeTableReplicaAutoScaling(a: DescribeTableReplicaAutoScalingRequest) = Kleisli(e => eff1(f(e).describeTableReplicaAutoScaling(a)))
      override def describeTimeToLive(a: DescribeTimeToLiveRequest) = Kleisli(e => eff1(f(e).describeTimeToLive(a)))
      override def disableKinesisStreamingDestination(a: DisableKinesisStreamingDestinationRequest) = Kleisli(e => eff1(f(e).disableKinesisStreamingDestination(a)))
      override def enableKinesisStreamingDestination(a: EnableKinesisStreamingDestinationRequest) = Kleisli(e => eff1(f(e).enableKinesisStreamingDestination(a)))
      override def executeStatement(a: ExecuteStatementRequest) = Kleisli(e => eff1(f(e).executeStatement(a)))
      override def executeTransaction(a: ExecuteTransactionRequest) = Kleisli(e => eff1(f(e).executeTransaction(a)))
      override def exportTableToPointInTime(a: ExportTableToPointInTimeRequest) = Kleisli(e => eff1(f(e).exportTableToPointInTime(a)))
      override def getItem(a: GetItemRequest) = Kleisli(e => eff1(f(e).getItem(a)))
      override def getResourcePolicy(a: GetResourcePolicyRequest) = Kleisli(e => eff1(f(e).getResourcePolicy(a)))
      override def importTable(a: ImportTableRequest) = Kleisli(e => eff1(f(e).importTable(a)))
      override def listBackups = Kleisli(e => eff1(f(e).listBackups))
      override def listBackups(a: ListBackupsRequest) = Kleisli(e => eff1(f(e).listBackups(a)))
      override def listContributorInsights(a: ListContributorInsightsRequest) = Kleisli(e => eff1(f(e).listContributorInsights(a)))
      override def listContributorInsightsPaginator(a: ListContributorInsightsRequest) = Kleisli(e => primitive1(f(e).listContributorInsightsPaginator(a)))
      override def listExports(a: ListExportsRequest) = Kleisli(e => eff1(f(e).listExports(a)))
      override def listExportsPaginator(a: ListExportsRequest) = Kleisli(e => primitive1(f(e).listExportsPaginator(a)))
      override def listGlobalTables = Kleisli(e => eff1(f(e).listGlobalTables))
      override def listGlobalTables(a: ListGlobalTablesRequest) = Kleisli(e => eff1(f(e).listGlobalTables(a)))
      override def listImports(a: ListImportsRequest) = Kleisli(e => eff1(f(e).listImports(a)))
      override def listImportsPaginator(a: ListImportsRequest) = Kleisli(e => primitive1(f(e).listImportsPaginator(a)))
      override def listTables = Kleisli(e => eff1(f(e).listTables))
      override def listTables(a: ListTablesRequest) = Kleisli(e => eff1(f(e).listTables(a)))
      override def listTablesPaginator = Kleisli(e => primitive1(f(e).listTablesPaginator))
      override def listTablesPaginator(a: ListTablesRequest) = Kleisli(e => primitive1(f(e).listTablesPaginator(a)))
      override def listTagsOfResource(a: ListTagsOfResourceRequest) = Kleisli(e => eff1(f(e).listTagsOfResource(a)))
      override def putItem(a: PutItemRequest) = Kleisli(e => eff1(f(e).putItem(a)))
      override def putResourcePolicy(a: PutResourcePolicyRequest) = Kleisli(e => eff1(f(e).putResourcePolicy(a)))
      override def query(a: QueryRequest) = Kleisli(e => eff1(f(e).query(a)))
      override def queryPaginator(a: QueryRequest) = Kleisli(e => primitive1(f(e).queryPaginator(a)))
      override def restoreTableFromBackup(a: RestoreTableFromBackupRequest) = Kleisli(e => eff1(f(e).restoreTableFromBackup(a)))
      override def restoreTableToPointInTime(a: RestoreTableToPointInTimeRequest) = Kleisli(e => eff1(f(e).restoreTableToPointInTime(a)))
      override def scan(a: ScanRequest) = Kleisli(e => eff1(f(e).scan(a)))
      override def scanPaginator(a: ScanRequest) = Kleisli(e => primitive1(f(e).scanPaginator(a)))
      override def serviceName = Kleisli(e => primitive1(f(e).serviceName))
      override def tagResource(a: TagResourceRequest) = Kleisli(e => eff1(f(e).tagResource(a)))
      override def transactGetItems(a: TransactGetItemsRequest) = Kleisli(e => eff1(f(e).transactGetItems(a)))
      override def transactWriteItems(a: TransactWriteItemsRequest) = Kleisli(e => eff1(f(e).transactWriteItems(a)))
      override def untagResource(a: UntagResourceRequest) = Kleisli(e => eff1(f(e).untagResource(a)))
      override def updateContinuousBackups(a: UpdateContinuousBackupsRequest) = Kleisli(e => eff1(f(e).updateContinuousBackups(a)))
      override def updateContributorInsights(a: UpdateContributorInsightsRequest) = Kleisli(e => eff1(f(e).updateContributorInsights(a)))
      override def updateGlobalTable(a: UpdateGlobalTableRequest) = Kleisli(e => eff1(f(e).updateGlobalTable(a)))
      override def updateGlobalTableSettings(a: UpdateGlobalTableSettingsRequest) = Kleisli(e => eff1(f(e).updateGlobalTableSettings(a)))
      override def updateItem(a: UpdateItemRequest) = Kleisli(e => eff1(f(e).updateItem(a)))
      override def updateKinesisStreamingDestination(a: UpdateKinesisStreamingDestinationRequest) = Kleisli(e => eff1(f(e).updateKinesisStreamingDestination(a)))
      override def updateTable(a: UpdateTableRequest) = Kleisli(e => eff1(f(e).updateTable(a)))
      override def updateTableReplicaAutoScaling(a: UpdateTableReplicaAutoScalingRequest) = Kleisli(e => eff1(f(e).updateTableReplicaAutoScaling(a)))
      override def updateTimeToLive(a: UpdateTimeToLiveRequest) = Kleisli(e => eff1(f(e).updateTimeToLive(a)))
      override def waiter = Kleisli(e => primitive1(f(e).waiter))
  
      }
    }

  // end interpreters

  def DynamoDbAsyncClientResource(builder : DynamoDbAsyncClientBuilder) : Resource[M, DynamoDbAsyncClient] = Resource.fromAutoCloseable(asyncM.delay(builder.build()))
  def DynamoDbAsyncClientOpResource(builder: DynamoDbAsyncClientBuilder) = DynamoDbAsyncClientResource(builder).map(create)

  def create(client : DynamoDbAsyncClient) : DynamoDbAsyncClientOp[M] = new DynamoDbAsyncClientOp[M] {

    // domain-specific operations are implemented in terms of `primitive`
    override def batchExecuteStatement(a: BatchExecuteStatementRequest) = eff1(client.batchExecuteStatement(a))
    override def batchGetItem(a: BatchGetItemRequest) = eff1(client.batchGetItem(a))
    override def batchGetItemPaginator(a: BatchGetItemRequest) = primitive1(client.batchGetItemPaginator(a))
    override def batchWriteItem(a: BatchWriteItemRequest) = eff1(client.batchWriteItem(a))
    override def close = primitive1(client.close)
    override def createBackup(a: CreateBackupRequest) = eff1(client.createBackup(a))
    override def createGlobalTable(a: CreateGlobalTableRequest) = eff1(client.createGlobalTable(a))
    override def createTable(a: CreateTableRequest) = eff1(client.createTable(a))
    override def deleteBackup(a: DeleteBackupRequest) = eff1(client.deleteBackup(a))
    override def deleteItem(a: DeleteItemRequest) = eff1(client.deleteItem(a))
    override def deleteResourcePolicy(a: DeleteResourcePolicyRequest) = eff1(client.deleteResourcePolicy(a))
    override def deleteTable(a: DeleteTableRequest) = eff1(client.deleteTable(a))
    override def describeBackup(a: DescribeBackupRequest) = eff1(client.describeBackup(a))
    override def describeContinuousBackups(a: DescribeContinuousBackupsRequest) = eff1(client.describeContinuousBackups(a))
    override def describeContributorInsights(a: DescribeContributorInsightsRequest) = eff1(client.describeContributorInsights(a))
    override def describeEndpoints = eff1(client.describeEndpoints)
    override def describeEndpoints(a: DescribeEndpointsRequest) = eff1(client.describeEndpoints(a))
    override def describeExport(a: DescribeExportRequest) = eff1(client.describeExport(a))
    override def describeGlobalTable(a: DescribeGlobalTableRequest) = eff1(client.describeGlobalTable(a))
    override def describeGlobalTableSettings(a: DescribeGlobalTableSettingsRequest) = eff1(client.describeGlobalTableSettings(a))
    override def describeImport(a: DescribeImportRequest) = eff1(client.describeImport(a))
    override def describeKinesisStreamingDestination(a: DescribeKinesisStreamingDestinationRequest) = eff1(client.describeKinesisStreamingDestination(a))
    override def describeLimits = eff1(client.describeLimits)
    override def describeLimits(a: DescribeLimitsRequest) = eff1(client.describeLimits(a))
    override def describeTable(a: DescribeTableRequest) = eff1(client.describeTable(a))
    override def describeTableReplicaAutoScaling(a: DescribeTableReplicaAutoScalingRequest) = eff1(client.describeTableReplicaAutoScaling(a))
    override def describeTimeToLive(a: DescribeTimeToLiveRequest) = eff1(client.describeTimeToLive(a))
    override def disableKinesisStreamingDestination(a: DisableKinesisStreamingDestinationRequest) = eff1(client.disableKinesisStreamingDestination(a))
    override def enableKinesisStreamingDestination(a: EnableKinesisStreamingDestinationRequest) = eff1(client.enableKinesisStreamingDestination(a))
    override def executeStatement(a: ExecuteStatementRequest) = eff1(client.executeStatement(a))
    override def executeTransaction(a: ExecuteTransactionRequest) = eff1(client.executeTransaction(a))
    override def exportTableToPointInTime(a: ExportTableToPointInTimeRequest) = eff1(client.exportTableToPointInTime(a))
    override def getItem(a: GetItemRequest) = eff1(client.getItem(a))
    override def getResourcePolicy(a: GetResourcePolicyRequest) = eff1(client.getResourcePolicy(a))
    override def importTable(a: ImportTableRequest) = eff1(client.importTable(a))
    override def listBackups = eff1(client.listBackups)
    override def listBackups(a: ListBackupsRequest) = eff1(client.listBackups(a))
    override def listContributorInsights(a: ListContributorInsightsRequest) = eff1(client.listContributorInsights(a))
    override def listContributorInsightsPaginator(a: ListContributorInsightsRequest) = primitive1(client.listContributorInsightsPaginator(a))
    override def listExports(a: ListExportsRequest) = eff1(client.listExports(a))
    override def listExportsPaginator(a: ListExportsRequest) = primitive1(client.listExportsPaginator(a))
    override def listGlobalTables = eff1(client.listGlobalTables)
    override def listGlobalTables(a: ListGlobalTablesRequest) = eff1(client.listGlobalTables(a))
    override def listImports(a: ListImportsRequest) = eff1(client.listImports(a))
    override def listImportsPaginator(a: ListImportsRequest) = primitive1(client.listImportsPaginator(a))
    override def listTables = eff1(client.listTables)
    override def listTables(a: ListTablesRequest) = eff1(client.listTables(a))
    override def listTablesPaginator = primitive1(client.listTablesPaginator)
    override def listTablesPaginator(a: ListTablesRequest) = primitive1(client.listTablesPaginator(a))
    override def listTagsOfResource(a: ListTagsOfResourceRequest) = eff1(client.listTagsOfResource(a))
    override def putItem(a: PutItemRequest) = eff1(client.putItem(a))
    override def putResourcePolicy(a: PutResourcePolicyRequest) = eff1(client.putResourcePolicy(a))
    override def query(a: QueryRequest) = eff1(client.query(a))
    override def queryPaginator(a: QueryRequest) = primitive1(client.queryPaginator(a))
    override def restoreTableFromBackup(a: RestoreTableFromBackupRequest) = eff1(client.restoreTableFromBackup(a))
    override def restoreTableToPointInTime(a: RestoreTableToPointInTimeRequest) = eff1(client.restoreTableToPointInTime(a))
    override def scan(a: ScanRequest) = eff1(client.scan(a))
    override def scanPaginator(a: ScanRequest) = primitive1(client.scanPaginator(a))
    override def serviceName = primitive1(client.serviceName)
    override def tagResource(a: TagResourceRequest) = eff1(client.tagResource(a))
    override def transactGetItems(a: TransactGetItemsRequest) = eff1(client.transactGetItems(a))
    override def transactWriteItems(a: TransactWriteItemsRequest) = eff1(client.transactWriteItems(a))
    override def untagResource(a: UntagResourceRequest) = eff1(client.untagResource(a))
    override def updateContinuousBackups(a: UpdateContinuousBackupsRequest) = eff1(client.updateContinuousBackups(a))
    override def updateContributorInsights(a: UpdateContributorInsightsRequest) = eff1(client.updateContributorInsights(a))
    override def updateGlobalTable(a: UpdateGlobalTableRequest) = eff1(client.updateGlobalTable(a))
    override def updateGlobalTableSettings(a: UpdateGlobalTableSettingsRequest) = eff1(client.updateGlobalTableSettings(a))
    override def updateItem(a: UpdateItemRequest) = eff1(client.updateItem(a))
    override def updateKinesisStreamingDestination(a: UpdateKinesisStreamingDestinationRequest) = eff1(client.updateKinesisStreamingDestination(a))
    override def updateTable(a: UpdateTableRequest) = eff1(client.updateTable(a))
    override def updateTableReplicaAutoScaling(a: UpdateTableReplicaAutoScalingRequest) = eff1(client.updateTableReplicaAutoScaling(a))
    override def updateTimeToLive(a: UpdateTimeToLiveRequest) = eff1(client.updateTimeToLive(a))
    override def waiter = primitive1(client.waiter)


  }


}

