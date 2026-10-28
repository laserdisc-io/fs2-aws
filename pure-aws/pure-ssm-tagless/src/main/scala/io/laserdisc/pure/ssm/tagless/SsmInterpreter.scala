package io.laserdisc.pure.ssm.tagless

// Library imports
import cats.data.Kleisli
import cats.effect.{Async, Resource}

import software.amazon.awssdk.services.ssm.*
import software.amazon.awssdk.services.ssm.model.*

// Types referenced
import software.amazon.awssdk.services.ssm.SsmServiceClientConfiguration
import software.amazon.awssdk.services.ssm.paginators.DescribeActivationsPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeAssociationExecutionTargetsPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeAssociationExecutionsPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeAutomationExecutionsPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeAutomationStepExecutionsPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeAvailablePatchesPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeEffectiveInstanceAssociationsPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeEffectivePatchesForPatchBaselinePublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeInstanceAssociationsStatusPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeInstanceInformationPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeInstancePatchStatesForPatchGroupPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeInstancePatchStatesPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeInstancePatchesPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeInstancePropertiesPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeInventoryDeletionsPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeMaintenanceWindowExecutionTaskInvocationsPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeMaintenanceWindowExecutionTasksPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeMaintenanceWindowExecutionsPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeMaintenanceWindowSchedulePublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeMaintenanceWindowTargetsPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeMaintenanceWindowTasksPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeMaintenanceWindowsForTargetPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeMaintenanceWindowsPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeOpsItemsPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeParametersPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribePatchBaselinesPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribePatchGroupsPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribePatchPropertiesPublisher
import software.amazon.awssdk.services.ssm.paginators.DescribeSessionsPublisher
import software.amazon.awssdk.services.ssm.paginators.GetInventoryPublisher
import software.amazon.awssdk.services.ssm.paginators.GetInventorySchemaPublisher
import software.amazon.awssdk.services.ssm.paginators.GetOpsSummaryPublisher
import software.amazon.awssdk.services.ssm.paginators.GetParameterHistoryPublisher
import software.amazon.awssdk.services.ssm.paginators.GetParametersByPathPublisher
import software.amazon.awssdk.services.ssm.paginators.GetResourcePoliciesPublisher
import software.amazon.awssdk.services.ssm.paginators.ListAssociationVersionsPublisher
import software.amazon.awssdk.services.ssm.paginators.ListAssociationsPublisher
import software.amazon.awssdk.services.ssm.paginators.ListCloudConnectorsPublisher
import software.amazon.awssdk.services.ssm.paginators.ListCommandInvocationsPublisher
import software.amazon.awssdk.services.ssm.paginators.ListCommandsPublisher
import software.amazon.awssdk.services.ssm.paginators.ListComplianceItemsPublisher
import software.amazon.awssdk.services.ssm.paginators.ListComplianceSummariesPublisher
import software.amazon.awssdk.services.ssm.paginators.ListDocumentVersionsPublisher
import software.amazon.awssdk.services.ssm.paginators.ListDocumentsPublisher
import software.amazon.awssdk.services.ssm.paginators.ListNodesPublisher
import software.amazon.awssdk.services.ssm.paginators.ListNodesSummaryPublisher
import software.amazon.awssdk.services.ssm.paginators.ListOpsItemEventsPublisher
import software.amazon.awssdk.services.ssm.paginators.ListOpsItemRelatedItemsPublisher
import software.amazon.awssdk.services.ssm.paginators.ListOpsMetadataPublisher
import software.amazon.awssdk.services.ssm.paginators.ListResourceComplianceSummariesPublisher
import software.amazon.awssdk.services.ssm.paginators.ListResourceDataSyncPublisher
import software.amazon.awssdk.services.ssm.paginators.ValidateCloudConnectorPublisher
import software.amazon.awssdk.services.ssm.waiters.SsmAsyncWaiter

object SsmInterpreter {

  def apply[M[_]](
      implicit am: Async[M]
  ): SsmInterpreter[M] =
    new SsmInterpreter[M] {
      val asyncM: Async[M] = am
    }

}

// the interpreter was previously named `Interpreter`; kept (as both a type and a term) so 6.x code still compiles
@deprecated("use SsmInterpreter", "7.0.0")
trait Interpreter[M[_]] extends SsmInterpreter[M]

@deprecated("use SsmInterpreter", "7.0.0")
object Interpreter {
  def apply[M[_]](implicit am: Async[M]): Interpreter[M] =
    new Interpreter[M] {
      val asyncM: Async[M] = am
    }
}

// Family of interpreters into Kleisli arrows for some monad M.
trait SsmInterpreter[M[_]] { outer =>

  import java.util.concurrent.CompletableFuture

  implicit val asyncM: Async[M]

  lazy val SsmAsyncClientInterpreter: SsmAsyncClientInterpreter = new SsmAsyncClientInterpreter {}
  // Some methods are common to all interpreters and can be overridden to change behavior globally.

  private def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(j => asyncM.blocking(f(j)))
  private def primitive1[J, A](f: => A): M[A]              = asyncM.blocking(f)

  private def eff[J, A](fut: J => CompletableFuture[A]): Kleisli[M, J, A] = Kleisli { j =>
    asyncM.fromCompletableFuture(asyncM.delay(fut(j)))
  }
  private def eff1[J, A](fut: => CompletableFuture[A]): M[A] =
    asyncM.fromCompletableFuture(asyncM.delay(fut))

  // Interpreters
  trait SsmAsyncClientInterpreter extends SsmAsyncClientOp[Kleisli[M, SsmAsyncClient, *]] {

    // domain-specific operations are implemented in terms of `primitive`
    override def addTagsToResource(a: AddTagsToResourceRequest): Kleisli[M, SsmAsyncClient, AddTagsToResourceResponse] =
      eff(_.addTagsToResource(a))
    override def associateOpsItemRelatedItem(
        a: AssociateOpsItemRelatedItemRequest
    ): Kleisli[M, SsmAsyncClient, AssociateOpsItemRelatedItemResponse] = eff(_.associateOpsItemRelatedItem(a))
    override def cancelCommand(a: CancelCommandRequest): Kleisli[M, SsmAsyncClient, CancelCommandResponse] = eff(
      _.cancelCommand(a)
    )
    override def cancelMaintenanceWindowExecution(
        a: CancelMaintenanceWindowExecutionRequest
    ): Kleisli[M, SsmAsyncClient, CancelMaintenanceWindowExecutionResponse] = eff(_.cancelMaintenanceWindowExecution(a))
    override def close: Kleisli[M, SsmAsyncClient, Unit]                    = primitive(_.close)
    override def createActivation(a: CreateActivationRequest): Kleisli[M, SsmAsyncClient, CreateActivationResponse] =
      eff(_.createActivation(a))
    override def createAssociation(a: CreateAssociationRequest): Kleisli[M, SsmAsyncClient, CreateAssociationResponse] =
      eff(_.createAssociation(a))
    override def createAssociationBatch(
        a: CreateAssociationBatchRequest
    ): Kleisli[M, SsmAsyncClient, CreateAssociationBatchResponse] = eff(_.createAssociationBatch(a))
    override def createCloudConnector(
        a: CreateCloudConnectorRequest
    ): Kleisli[M, SsmAsyncClient, CreateCloudConnectorResponse] = eff(_.createCloudConnector(a))
    override def createDocument(a: CreateDocumentRequest): Kleisli[M, SsmAsyncClient, CreateDocumentResponse] = eff(
      _.createDocument(a)
    )
    override def createMaintenanceWindow(
        a: CreateMaintenanceWindowRequest
    ): Kleisli[M, SsmAsyncClient, CreateMaintenanceWindowResponse] = eff(_.createMaintenanceWindow(a))
    override def createOpsItem(a: CreateOpsItemRequest): Kleisli[M, SsmAsyncClient, CreateOpsItemResponse] = eff(
      _.createOpsItem(a)
    )
    override def createOpsMetadata(a: CreateOpsMetadataRequest): Kleisli[M, SsmAsyncClient, CreateOpsMetadataResponse] =
      eff(_.createOpsMetadata(a))
    override def createPatchBaseline(
        a: CreatePatchBaselineRequest
    ): Kleisli[M, SsmAsyncClient, CreatePatchBaselineResponse] = eff(_.createPatchBaseline(a))
    override def createResourceDataSync(
        a: CreateResourceDataSyncRequest
    ): Kleisli[M, SsmAsyncClient, CreateResourceDataSyncResponse] = eff(_.createResourceDataSync(a))
    override def deleteActivation(a: DeleteActivationRequest): Kleisli[M, SsmAsyncClient, DeleteActivationResponse] =
      eff(_.deleteActivation(a))
    override def deleteAssociation(a: DeleteAssociationRequest): Kleisli[M, SsmAsyncClient, DeleteAssociationResponse] =
      eff(_.deleteAssociation(a))
    override def deleteCloudConnector(
        a: DeleteCloudConnectorRequest
    ): Kleisli[M, SsmAsyncClient, DeleteCloudConnectorResponse] = eff(_.deleteCloudConnector(a))
    override def deleteDocument(a: DeleteDocumentRequest): Kleisli[M, SsmAsyncClient, DeleteDocumentResponse] = eff(
      _.deleteDocument(a)
    )
    override def deleteInventory(a: DeleteInventoryRequest): Kleisli[M, SsmAsyncClient, DeleteInventoryResponse] = eff(
      _.deleteInventory(a)
    )
    override def deleteMaintenanceWindow(
        a: DeleteMaintenanceWindowRequest
    ): Kleisli[M, SsmAsyncClient, DeleteMaintenanceWindowResponse] = eff(_.deleteMaintenanceWindow(a))
    override def deleteOpsItem(a: DeleteOpsItemRequest): Kleisli[M, SsmAsyncClient, DeleteOpsItemResponse] = eff(
      _.deleteOpsItem(a)
    )
    override def deleteOpsMetadata(a: DeleteOpsMetadataRequest): Kleisli[M, SsmAsyncClient, DeleteOpsMetadataResponse] =
      eff(_.deleteOpsMetadata(a))
    override def deleteParameter(a: DeleteParameterRequest): Kleisli[M, SsmAsyncClient, DeleteParameterResponse] = eff(
      _.deleteParameter(a)
    )
    override def deleteParameters(a: DeleteParametersRequest): Kleisli[M, SsmAsyncClient, DeleteParametersResponse] =
      eff(_.deleteParameters(a))
    override def deletePatchBaseline(
        a: DeletePatchBaselineRequest
    ): Kleisli[M, SsmAsyncClient, DeletePatchBaselineResponse] = eff(_.deletePatchBaseline(a))
    override def deleteResourceDataSync(
        a: DeleteResourceDataSyncRequest
    ): Kleisli[M, SsmAsyncClient, DeleteResourceDataSyncResponse] = eff(_.deleteResourceDataSync(a))
    override def deleteResourcePolicy(
        a: DeleteResourcePolicyRequest
    ): Kleisli[M, SsmAsyncClient, DeleteResourcePolicyResponse] = eff(_.deleteResourcePolicy(a))
    override def deregisterManagedInstance(
        a: DeregisterManagedInstanceRequest
    ): Kleisli[M, SsmAsyncClient, DeregisterManagedInstanceResponse] = eff(_.deregisterManagedInstance(a))
    override def deregisterPatchBaselineForPatchGroup(
        a: DeregisterPatchBaselineForPatchGroupRequest
    ): Kleisli[M, SsmAsyncClient, DeregisterPatchBaselineForPatchGroupResponse] = eff(
      _.deregisterPatchBaselineForPatchGroup(a)
    )
    override def deregisterTargetFromMaintenanceWindow(
        a: DeregisterTargetFromMaintenanceWindowRequest
    ): Kleisli[M, SsmAsyncClient, DeregisterTargetFromMaintenanceWindowResponse] = eff(
      _.deregisterTargetFromMaintenanceWindow(a)
    )
    override def deregisterTaskFromMaintenanceWindow(
        a: DeregisterTaskFromMaintenanceWindowRequest
    ): Kleisli[M, SsmAsyncClient, DeregisterTaskFromMaintenanceWindowResponse] = eff(
      _.deregisterTaskFromMaintenanceWindow(a)
    )
    override def describeActivations: Kleisli[M, SsmAsyncClient, DescribeActivationsResponse] = eff(
      _.describeActivations
    )
    override def describeActivations(
        a: DescribeActivationsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeActivationsResponse] = eff(_.describeActivations(a))
    override def describeActivationsPaginator: Kleisli[M, SsmAsyncClient, DescribeActivationsPublisher] = primitive(
      _.describeActivationsPaginator
    )
    override def describeActivationsPaginator(
        a: DescribeActivationsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeActivationsPublisher] = primitive(_.describeActivationsPaginator(a))
    override def describeAssociation(
        a: DescribeAssociationRequest
    ): Kleisli[M, SsmAsyncClient, DescribeAssociationResponse] = eff(_.describeAssociation(a))
    override def describeAssociationExecutionTargets(
        a: DescribeAssociationExecutionTargetsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeAssociationExecutionTargetsResponse] = eff(
      _.describeAssociationExecutionTargets(a)
    )
    override def describeAssociationExecutionTargetsPaginator(
        a: DescribeAssociationExecutionTargetsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeAssociationExecutionTargetsPublisher] = primitive(
      _.describeAssociationExecutionTargetsPaginator(a)
    )
    override def describeAssociationExecutions(
        a: DescribeAssociationExecutionsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeAssociationExecutionsResponse] = eff(_.describeAssociationExecutions(a))
    override def describeAssociationExecutionsPaginator(
        a: DescribeAssociationExecutionsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeAssociationExecutionsPublisher] = primitive(
      _.describeAssociationExecutionsPaginator(a)
    )
    override def describeAutomationExecutions: Kleisli[M, SsmAsyncClient, DescribeAutomationExecutionsResponse] = eff(
      _.describeAutomationExecutions
    )
    override def describeAutomationExecutions(
        a: DescribeAutomationExecutionsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeAutomationExecutionsResponse] = eff(_.describeAutomationExecutions(a))
    override def describeAutomationExecutionsPaginator
        : Kleisli[M, SsmAsyncClient, DescribeAutomationExecutionsPublisher] = primitive(
      _.describeAutomationExecutionsPaginator
    )
    override def describeAutomationExecutionsPaginator(
        a: DescribeAutomationExecutionsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeAutomationExecutionsPublisher] = primitive(
      _.describeAutomationExecutionsPaginator(a)
    )
    override def describeAutomationStepExecutions(
        a: DescribeAutomationStepExecutionsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeAutomationStepExecutionsResponse] = eff(_.describeAutomationStepExecutions(a))
    override def describeAutomationStepExecutionsPaginator(
        a: DescribeAutomationStepExecutionsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeAutomationStepExecutionsPublisher] = primitive(
      _.describeAutomationStepExecutionsPaginator(a)
    )
    override def describeAvailablePatches: Kleisli[M, SsmAsyncClient, DescribeAvailablePatchesResponse] = eff(
      _.describeAvailablePatches
    )
    override def describeAvailablePatches(
        a: DescribeAvailablePatchesRequest
    ): Kleisli[M, SsmAsyncClient, DescribeAvailablePatchesResponse] = eff(_.describeAvailablePatches(a))
    override def describeAvailablePatchesPaginator: Kleisli[M, SsmAsyncClient, DescribeAvailablePatchesPublisher] =
      primitive(_.describeAvailablePatchesPaginator)
    override def describeAvailablePatchesPaginator(
        a: DescribeAvailablePatchesRequest
    ): Kleisli[M, SsmAsyncClient, DescribeAvailablePatchesPublisher] = primitive(_.describeAvailablePatchesPaginator(a))
    override def describeDocument(a: DescribeDocumentRequest): Kleisli[M, SsmAsyncClient, DescribeDocumentResponse] =
      eff(_.describeDocument(a))
    override def describeDocumentPermission(
        a: DescribeDocumentPermissionRequest
    ): Kleisli[M, SsmAsyncClient, DescribeDocumentPermissionResponse] = eff(_.describeDocumentPermission(a))
    override def describeEffectiveInstanceAssociations(
        a: DescribeEffectiveInstanceAssociationsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeEffectiveInstanceAssociationsResponse] = eff(
      _.describeEffectiveInstanceAssociations(a)
    )
    override def describeEffectiveInstanceAssociationsPaginator(
        a: DescribeEffectiveInstanceAssociationsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeEffectiveInstanceAssociationsPublisher] = primitive(
      _.describeEffectiveInstanceAssociationsPaginator(a)
    )
    override def describeEffectivePatchesForPatchBaseline(
        a: DescribeEffectivePatchesForPatchBaselineRequest
    ): Kleisli[M, SsmAsyncClient, DescribeEffectivePatchesForPatchBaselineResponse] = eff(
      _.describeEffectivePatchesForPatchBaseline(a)
    )
    override def describeEffectivePatchesForPatchBaselinePaginator(
        a: DescribeEffectivePatchesForPatchBaselineRequest
    ): Kleisli[M, SsmAsyncClient, DescribeEffectivePatchesForPatchBaselinePublisher] = primitive(
      _.describeEffectivePatchesForPatchBaselinePaginator(a)
    )
    override def describeInstanceAssociationsStatus(
        a: DescribeInstanceAssociationsStatusRequest
    ): Kleisli[M, SsmAsyncClient, DescribeInstanceAssociationsStatusResponse] = eff(
      _.describeInstanceAssociationsStatus(a)
    )
    override def describeInstanceAssociationsStatusPaginator(
        a: DescribeInstanceAssociationsStatusRequest
    ): Kleisli[M, SsmAsyncClient, DescribeInstanceAssociationsStatusPublisher] = primitive(
      _.describeInstanceAssociationsStatusPaginator(a)
    )
    override def describeInstanceInformation: Kleisli[M, SsmAsyncClient, DescribeInstanceInformationResponse] = eff(
      _.describeInstanceInformation
    )
    override def describeInstanceInformation(
        a: DescribeInstanceInformationRequest
    ): Kleisli[M, SsmAsyncClient, DescribeInstanceInformationResponse] = eff(_.describeInstanceInformation(a))
    override def describeInstanceInformationPaginator
        : Kleisli[M, SsmAsyncClient, DescribeInstanceInformationPublisher] = primitive(
      _.describeInstanceInformationPaginator
    )
    override def describeInstanceInformationPaginator(
        a: DescribeInstanceInformationRequest
    ): Kleisli[M, SsmAsyncClient, DescribeInstanceInformationPublisher] = primitive(
      _.describeInstanceInformationPaginator(a)
    )
    override def describeInstancePatchStates(
        a: DescribeInstancePatchStatesRequest
    ): Kleisli[M, SsmAsyncClient, DescribeInstancePatchStatesResponse] = eff(_.describeInstancePatchStates(a))
    override def describeInstancePatchStatesForPatchGroup(
        a: DescribeInstancePatchStatesForPatchGroupRequest
    ): Kleisli[M, SsmAsyncClient, DescribeInstancePatchStatesForPatchGroupResponse] = eff(
      _.describeInstancePatchStatesForPatchGroup(a)
    )
    override def describeInstancePatchStatesForPatchGroupPaginator(
        a: DescribeInstancePatchStatesForPatchGroupRequest
    ): Kleisli[M, SsmAsyncClient, DescribeInstancePatchStatesForPatchGroupPublisher] = primitive(
      _.describeInstancePatchStatesForPatchGroupPaginator(a)
    )
    override def describeInstancePatchStatesPaginator(
        a: DescribeInstancePatchStatesRequest
    ): Kleisli[M, SsmAsyncClient, DescribeInstancePatchStatesPublisher] = primitive(
      _.describeInstancePatchStatesPaginator(a)
    )
    override def describeInstancePatches(
        a: DescribeInstancePatchesRequest
    ): Kleisli[M, SsmAsyncClient, DescribeInstancePatchesResponse] = eff(_.describeInstancePatches(a))
    override def describeInstancePatchesPaginator(
        a: DescribeInstancePatchesRequest
    ): Kleisli[M, SsmAsyncClient, DescribeInstancePatchesPublisher] = primitive(_.describeInstancePatchesPaginator(a))
    override def describeInstanceProperties(
        a: DescribeInstancePropertiesRequest
    ): Kleisli[M, SsmAsyncClient, DescribeInstancePropertiesResponse] = eff(_.describeInstanceProperties(a))
    override def describeInstancePropertiesPaginator(
        a: DescribeInstancePropertiesRequest
    ): Kleisli[M, SsmAsyncClient, DescribeInstancePropertiesPublisher] = primitive(
      _.describeInstancePropertiesPaginator(a)
    )
    override def describeInventoryDeletions: Kleisli[M, SsmAsyncClient, DescribeInventoryDeletionsResponse] = eff(
      _.describeInventoryDeletions
    )
    override def describeInventoryDeletions(
        a: DescribeInventoryDeletionsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeInventoryDeletionsResponse] = eff(_.describeInventoryDeletions(a))
    override def describeInventoryDeletionsPaginator: Kleisli[M, SsmAsyncClient, DescribeInventoryDeletionsPublisher] =
      primitive(_.describeInventoryDeletionsPaginator)
    override def describeInventoryDeletionsPaginator(
        a: DescribeInventoryDeletionsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeInventoryDeletionsPublisher] = primitive(
      _.describeInventoryDeletionsPaginator(a)
    )
    override def describeMaintenanceWindowExecutionTaskInvocations(
        a: DescribeMaintenanceWindowExecutionTaskInvocationsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowExecutionTaskInvocationsResponse] = eff(
      _.describeMaintenanceWindowExecutionTaskInvocations(a)
    )
    override def describeMaintenanceWindowExecutionTaskInvocationsPaginator(
        a: DescribeMaintenanceWindowExecutionTaskInvocationsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowExecutionTaskInvocationsPublisher] = primitive(
      _.describeMaintenanceWindowExecutionTaskInvocationsPaginator(a)
    )
    override def describeMaintenanceWindowExecutionTasks(
        a: DescribeMaintenanceWindowExecutionTasksRequest
    ): Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowExecutionTasksResponse] = eff(
      _.describeMaintenanceWindowExecutionTasks(a)
    )
    override def describeMaintenanceWindowExecutionTasksPaginator(
        a: DescribeMaintenanceWindowExecutionTasksRequest
    ): Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowExecutionTasksPublisher] = primitive(
      _.describeMaintenanceWindowExecutionTasksPaginator(a)
    )
    override def describeMaintenanceWindowExecutions(
        a: DescribeMaintenanceWindowExecutionsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowExecutionsResponse] = eff(
      _.describeMaintenanceWindowExecutions(a)
    )
    override def describeMaintenanceWindowExecutionsPaginator(
        a: DescribeMaintenanceWindowExecutionsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowExecutionsPublisher] = primitive(
      _.describeMaintenanceWindowExecutionsPaginator(a)
    )
    override def describeMaintenanceWindowSchedule(
        a: DescribeMaintenanceWindowScheduleRequest
    ): Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowScheduleResponse] = eff(
      _.describeMaintenanceWindowSchedule(a)
    )
    override def describeMaintenanceWindowSchedulePaginator(
        a: DescribeMaintenanceWindowScheduleRequest
    ): Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowSchedulePublisher] = primitive(
      _.describeMaintenanceWindowSchedulePaginator(a)
    )
    override def describeMaintenanceWindowTargets(
        a: DescribeMaintenanceWindowTargetsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowTargetsResponse] = eff(_.describeMaintenanceWindowTargets(a))
    override def describeMaintenanceWindowTargetsPaginator(
        a: DescribeMaintenanceWindowTargetsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowTargetsPublisher] = primitive(
      _.describeMaintenanceWindowTargetsPaginator(a)
    )
    override def describeMaintenanceWindowTasks(
        a: DescribeMaintenanceWindowTasksRequest
    ): Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowTasksResponse] = eff(_.describeMaintenanceWindowTasks(a))
    override def describeMaintenanceWindowTasksPaginator(
        a: DescribeMaintenanceWindowTasksRequest
    ): Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowTasksPublisher] = primitive(
      _.describeMaintenanceWindowTasksPaginator(a)
    )
    override def describeMaintenanceWindows: Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowsResponse] = eff(
      _.describeMaintenanceWindows
    )
    override def describeMaintenanceWindows(
        a: DescribeMaintenanceWindowsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowsResponse] = eff(_.describeMaintenanceWindows(a))
    override def describeMaintenanceWindowsForTarget(
        a: DescribeMaintenanceWindowsForTargetRequest
    ): Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowsForTargetResponse] = eff(
      _.describeMaintenanceWindowsForTarget(a)
    )
    override def describeMaintenanceWindowsForTargetPaginator(
        a: DescribeMaintenanceWindowsForTargetRequest
    ): Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowsForTargetPublisher] = primitive(
      _.describeMaintenanceWindowsForTargetPaginator(a)
    )
    override def describeMaintenanceWindowsPaginator: Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowsPublisher] =
      primitive(_.describeMaintenanceWindowsPaginator)
    override def describeMaintenanceWindowsPaginator(
        a: DescribeMaintenanceWindowsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeMaintenanceWindowsPublisher] = primitive(
      _.describeMaintenanceWindowsPaginator(a)
    )
    override def describeOpsItems(a: DescribeOpsItemsRequest): Kleisli[M, SsmAsyncClient, DescribeOpsItemsResponse] =
      eff(_.describeOpsItems(a))
    override def describeOpsItemsPaginator(
        a: DescribeOpsItemsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeOpsItemsPublisher] = primitive(_.describeOpsItemsPaginator(a))
    override def describeParameters: Kleisli[M, SsmAsyncClient, DescribeParametersResponse] = eff(_.describeParameters)
    override def describeParameters(
        a: DescribeParametersRequest
    ): Kleisli[M, SsmAsyncClient, DescribeParametersResponse] = eff(_.describeParameters(a))
    override def describeParametersPaginator: Kleisli[M, SsmAsyncClient, DescribeParametersPublisher] = primitive(
      _.describeParametersPaginator
    )
    override def describeParametersPaginator(
        a: DescribeParametersRequest
    ): Kleisli[M, SsmAsyncClient, DescribeParametersPublisher] = primitive(_.describeParametersPaginator(a))
    override def describePatchBaselines: Kleisli[M, SsmAsyncClient, DescribePatchBaselinesResponse] = eff(
      _.describePatchBaselines
    )
    override def describePatchBaselines(
        a: DescribePatchBaselinesRequest
    ): Kleisli[M, SsmAsyncClient, DescribePatchBaselinesResponse] = eff(_.describePatchBaselines(a))
    override def describePatchBaselinesPaginator: Kleisli[M, SsmAsyncClient, DescribePatchBaselinesPublisher] =
      primitive(_.describePatchBaselinesPaginator)
    override def describePatchBaselinesPaginator(
        a: DescribePatchBaselinesRequest
    ): Kleisli[M, SsmAsyncClient, DescribePatchBaselinesPublisher] = primitive(_.describePatchBaselinesPaginator(a))
    override def describePatchGroupState(
        a: DescribePatchGroupStateRequest
    ): Kleisli[M, SsmAsyncClient, DescribePatchGroupStateResponse] = eff(_.describePatchGroupState(a))
    override def describePatchGroups: Kleisli[M, SsmAsyncClient, DescribePatchGroupsResponse] = eff(
      _.describePatchGroups
    )
    override def describePatchGroups(
        a: DescribePatchGroupsRequest
    ): Kleisli[M, SsmAsyncClient, DescribePatchGroupsResponse] = eff(_.describePatchGroups(a))
    override def describePatchGroupsPaginator: Kleisli[M, SsmAsyncClient, DescribePatchGroupsPublisher] = primitive(
      _.describePatchGroupsPaginator
    )
    override def describePatchGroupsPaginator(
        a: DescribePatchGroupsRequest
    ): Kleisli[M, SsmAsyncClient, DescribePatchGroupsPublisher] = primitive(_.describePatchGroupsPaginator(a))
    override def describePatchProperties(
        a: DescribePatchPropertiesRequest
    ): Kleisli[M, SsmAsyncClient, DescribePatchPropertiesResponse] = eff(_.describePatchProperties(a))
    override def describePatchPropertiesPaginator(
        a: DescribePatchPropertiesRequest
    ): Kleisli[M, SsmAsyncClient, DescribePatchPropertiesPublisher] = primitive(_.describePatchPropertiesPaginator(a))
    override def describeSessions(a: DescribeSessionsRequest): Kleisli[M, SsmAsyncClient, DescribeSessionsResponse] =
      eff(_.describeSessions(a))
    override def describeSessionsPaginator(
        a: DescribeSessionsRequest
    ): Kleisli[M, SsmAsyncClient, DescribeSessionsPublisher] = primitive(_.describeSessionsPaginator(a))
    override def disassociateOpsItemRelatedItem(
        a: DisassociateOpsItemRelatedItemRequest
    ): Kleisli[M, SsmAsyncClient, DisassociateOpsItemRelatedItemResponse] = eff(_.disassociateOpsItemRelatedItem(a))
    override def getAccessToken(a: GetAccessTokenRequest): Kleisli[M, SsmAsyncClient, GetAccessTokenResponse] = eff(
      _.getAccessToken(a)
    )
    override def getAutomationExecution(
        a: GetAutomationExecutionRequest
    ): Kleisli[M, SsmAsyncClient, GetAutomationExecutionResponse] = eff(_.getAutomationExecution(a))
    override def getCalendarState(a: GetCalendarStateRequest): Kleisli[M, SsmAsyncClient, GetCalendarStateResponse] =
      eff(_.getCalendarState(a))
    override def getCloudConnector(a: GetCloudConnectorRequest): Kleisli[M, SsmAsyncClient, GetCloudConnectorResponse] =
      eff(_.getCloudConnector(a))
    override def getCommandInvocation(
        a: GetCommandInvocationRequest
    ): Kleisli[M, SsmAsyncClient, GetCommandInvocationResponse] = eff(_.getCommandInvocation(a))
    override def getConnectionStatus(
        a: GetConnectionStatusRequest
    ): Kleisli[M, SsmAsyncClient, GetConnectionStatusResponse] = eff(_.getConnectionStatus(a))
    override def getDefaultPatchBaseline: Kleisli[M, SsmAsyncClient, GetDefaultPatchBaselineResponse] = eff(
      _.getDefaultPatchBaseline
    )
    override def getDefaultPatchBaseline(
        a: GetDefaultPatchBaselineRequest
    ): Kleisli[M, SsmAsyncClient, GetDefaultPatchBaselineResponse] = eff(_.getDefaultPatchBaseline(a))
    override def getDeployablePatchSnapshotForInstance(
        a: GetDeployablePatchSnapshotForInstanceRequest
    ): Kleisli[M, SsmAsyncClient, GetDeployablePatchSnapshotForInstanceResponse] = eff(
      _.getDeployablePatchSnapshotForInstance(a)
    )
    override def getDocument(a: GetDocumentRequest): Kleisli[M, SsmAsyncClient, GetDocumentResponse] = eff(
      _.getDocument(a)
    )
    override def getExecutionPreview(
        a: GetExecutionPreviewRequest
    ): Kleisli[M, SsmAsyncClient, GetExecutionPreviewResponse]                  = eff(_.getExecutionPreview(a))
    override def getInventory: Kleisli[M, SsmAsyncClient, GetInventoryResponse] = eff(_.getInventory)
    override def getInventory(a: GetInventoryRequest): Kleisli[M, SsmAsyncClient, GetInventoryResponse] = eff(
      _.getInventory(a)
    )
    override def getInventoryPaginator: Kleisli[M, SsmAsyncClient, GetInventoryPublisher] = primitive(
      _.getInventoryPaginator
    )
    override def getInventoryPaginator(a: GetInventoryRequest): Kleisli[M, SsmAsyncClient, GetInventoryPublisher] =
      primitive(_.getInventoryPaginator(a))
    override def getInventorySchema: Kleisli[M, SsmAsyncClient, GetInventorySchemaResponse] = eff(_.getInventorySchema)
    override def getInventorySchema(
        a: GetInventorySchemaRequest
    ): Kleisli[M, SsmAsyncClient, GetInventorySchemaResponse] = eff(_.getInventorySchema(a))
    override def getInventorySchemaPaginator: Kleisli[M, SsmAsyncClient, GetInventorySchemaPublisher] = primitive(
      _.getInventorySchemaPaginator
    )
    override def getInventorySchemaPaginator(
        a: GetInventorySchemaRequest
    ): Kleisli[M, SsmAsyncClient, GetInventorySchemaPublisher] = primitive(_.getInventorySchemaPaginator(a))
    override def getMaintenanceWindow(
        a: GetMaintenanceWindowRequest
    ): Kleisli[M, SsmAsyncClient, GetMaintenanceWindowResponse] = eff(_.getMaintenanceWindow(a))
    override def getMaintenanceWindowExecution(
        a: GetMaintenanceWindowExecutionRequest
    ): Kleisli[M, SsmAsyncClient, GetMaintenanceWindowExecutionResponse] = eff(_.getMaintenanceWindowExecution(a))
    override def getMaintenanceWindowExecutionTask(
        a: GetMaintenanceWindowExecutionTaskRequest
    ): Kleisli[M, SsmAsyncClient, GetMaintenanceWindowExecutionTaskResponse] = eff(
      _.getMaintenanceWindowExecutionTask(a)
    )
    override def getMaintenanceWindowExecutionTaskInvocation(
        a: GetMaintenanceWindowExecutionTaskInvocationRequest
    ): Kleisli[M, SsmAsyncClient, GetMaintenanceWindowExecutionTaskInvocationResponse] = eff(
      _.getMaintenanceWindowExecutionTaskInvocation(a)
    )
    override def getMaintenanceWindowTask(
        a: GetMaintenanceWindowTaskRequest
    ): Kleisli[M, SsmAsyncClient, GetMaintenanceWindowTaskResponse] = eff(_.getMaintenanceWindowTask(a))
    override def getOpsItem(a: GetOpsItemRequest): Kleisli[M, SsmAsyncClient, GetOpsItemResponse] = eff(_.getOpsItem(a))
    override def getOpsMetadata(a: GetOpsMetadataRequest): Kleisli[M, SsmAsyncClient, GetOpsMetadataResponse] = eff(
      _.getOpsMetadata(a)
    )
    override def getOpsSummary(a: GetOpsSummaryRequest): Kleisli[M, SsmAsyncClient, GetOpsSummaryResponse] = eff(
      _.getOpsSummary(a)
    )
    override def getOpsSummaryPaginator(a: GetOpsSummaryRequest): Kleisli[M, SsmAsyncClient, GetOpsSummaryPublisher] =
      primitive(_.getOpsSummaryPaginator(a))
    override def getParameter(a: GetParameterRequest): Kleisli[M, SsmAsyncClient, GetParameterResponse] = eff(
      _.getParameter(a)
    )
    override def getParameterHistory(
        a: GetParameterHistoryRequest
    ): Kleisli[M, SsmAsyncClient, GetParameterHistoryResponse] = eff(_.getParameterHistory(a))
    override def getParameterHistoryPaginator(
        a: GetParameterHistoryRequest
    ): Kleisli[M, SsmAsyncClient, GetParameterHistoryPublisher] = primitive(_.getParameterHistoryPaginator(a))
    override def getParameters(a: GetParametersRequest): Kleisli[M, SsmAsyncClient, GetParametersResponse] = eff(
      _.getParameters(a)
    )
    override def getParametersByPath(
        a: GetParametersByPathRequest
    ): Kleisli[M, SsmAsyncClient, GetParametersByPathResponse] = eff(_.getParametersByPath(a))
    override def getParametersByPathPaginator(
        a: GetParametersByPathRequest
    ): Kleisli[M, SsmAsyncClient, GetParametersByPathPublisher] = primitive(_.getParametersByPathPaginator(a))
    override def getPatchBaseline(a: GetPatchBaselineRequest): Kleisli[M, SsmAsyncClient, GetPatchBaselineResponse] =
      eff(_.getPatchBaseline(a))
    override def getPatchBaselineForPatchGroup(
        a: GetPatchBaselineForPatchGroupRequest
    ): Kleisli[M, SsmAsyncClient, GetPatchBaselineForPatchGroupResponse] = eff(_.getPatchBaselineForPatchGroup(a))
    override def getResourcePolicies(
        a: GetResourcePoliciesRequest
    ): Kleisli[M, SsmAsyncClient, GetResourcePoliciesResponse] = eff(_.getResourcePolicies(a))
    override def getResourcePoliciesPaginator(
        a: GetResourcePoliciesRequest
    ): Kleisli[M, SsmAsyncClient, GetResourcePoliciesPublisher] = primitive(_.getResourcePoliciesPaginator(a))
    override def getServiceSetting(a: GetServiceSettingRequest): Kleisli[M, SsmAsyncClient, GetServiceSettingResponse] =
      eff(_.getServiceSetting(a))
    override def labelParameterVersion(
        a: LabelParameterVersionRequest
    ): Kleisli[M, SsmAsyncClient, LabelParameterVersionResponse] = eff(_.labelParameterVersion(a))
    override def listAssociationVersions(
        a: ListAssociationVersionsRequest
    ): Kleisli[M, SsmAsyncClient, ListAssociationVersionsResponse] = eff(_.listAssociationVersions(a))
    override def listAssociationVersionsPaginator(
        a: ListAssociationVersionsRequest
    ): Kleisli[M, SsmAsyncClient, ListAssociationVersionsPublisher] = primitive(_.listAssociationVersionsPaginator(a))
    override def listAssociations: Kleisli[M, SsmAsyncClient, ListAssociationsResponse] = eff(_.listAssociations)
    override def listAssociations(a: ListAssociationsRequest): Kleisli[M, SsmAsyncClient, ListAssociationsResponse] =
      eff(_.listAssociations(a))
    override def listAssociationsPaginator: Kleisli[M, SsmAsyncClient, ListAssociationsPublisher] = primitive(
      _.listAssociationsPaginator
    )
    override def listAssociationsPaginator(
        a: ListAssociationsRequest
    ): Kleisli[M, SsmAsyncClient, ListAssociationsPublisher] = primitive(_.listAssociationsPaginator(a))
    override def listCloudConnectors(
        a: ListCloudConnectorsRequest
    ): Kleisli[M, SsmAsyncClient, ListCloudConnectorsResponse] = eff(_.listCloudConnectors(a))
    override def listCloudConnectorsPaginator(
        a: ListCloudConnectorsRequest
    ): Kleisli[M, SsmAsyncClient, ListCloudConnectorsPublisher] = primitive(_.listCloudConnectorsPaginator(a))
    override def listCommandInvocations: Kleisli[M, SsmAsyncClient, ListCommandInvocationsResponse] = eff(
      _.listCommandInvocations
    )
    override def listCommandInvocations(
        a: ListCommandInvocationsRequest
    ): Kleisli[M, SsmAsyncClient, ListCommandInvocationsResponse] = eff(_.listCommandInvocations(a))
    override def listCommandInvocationsPaginator: Kleisli[M, SsmAsyncClient, ListCommandInvocationsPublisher] =
      primitive(_.listCommandInvocationsPaginator)
    override def listCommandInvocationsPaginator(
        a: ListCommandInvocationsRequest
    ): Kleisli[M, SsmAsyncClient, ListCommandInvocationsPublisher] = primitive(_.listCommandInvocationsPaginator(a))
    override def listCommands: Kleisli[M, SsmAsyncClient, ListCommandsResponse] = eff(_.listCommands)
    override def listCommands(a: ListCommandsRequest): Kleisli[M, SsmAsyncClient, ListCommandsResponse] = eff(
      _.listCommands(a)
    )
    override def listCommandsPaginator: Kleisli[M, SsmAsyncClient, ListCommandsPublisher] = primitive(
      _.listCommandsPaginator
    )
    override def listCommandsPaginator(a: ListCommandsRequest): Kleisli[M, SsmAsyncClient, ListCommandsPublisher] =
      primitive(_.listCommandsPaginator(a))
    override def listComplianceItems(
        a: ListComplianceItemsRequest
    ): Kleisli[M, SsmAsyncClient, ListComplianceItemsResponse] = eff(_.listComplianceItems(a))
    override def listComplianceItemsPaginator(
        a: ListComplianceItemsRequest
    ): Kleisli[M, SsmAsyncClient, ListComplianceItemsPublisher] = primitive(_.listComplianceItemsPaginator(a))
    override def listComplianceSummaries: Kleisli[M, SsmAsyncClient, ListComplianceSummariesResponse] = eff(
      _.listComplianceSummaries
    )
    override def listComplianceSummaries(
        a: ListComplianceSummariesRequest
    ): Kleisli[M, SsmAsyncClient, ListComplianceSummariesResponse] = eff(_.listComplianceSummaries(a))
    override def listComplianceSummariesPaginator: Kleisli[M, SsmAsyncClient, ListComplianceSummariesPublisher] =
      primitive(_.listComplianceSummariesPaginator)
    override def listComplianceSummariesPaginator(
        a: ListComplianceSummariesRequest
    ): Kleisli[M, SsmAsyncClient, ListComplianceSummariesPublisher] = primitive(_.listComplianceSummariesPaginator(a))
    override def listDocumentMetadataHistory(
        a: ListDocumentMetadataHistoryRequest
    ): Kleisli[M, SsmAsyncClient, ListDocumentMetadataHistoryResponse] = eff(_.listDocumentMetadataHistory(a))
    override def listDocumentVersions(
        a: ListDocumentVersionsRequest
    ): Kleisli[M, SsmAsyncClient, ListDocumentVersionsResponse] = eff(_.listDocumentVersions(a))
    override def listDocumentVersionsPaginator(
        a: ListDocumentVersionsRequest
    ): Kleisli[M, SsmAsyncClient, ListDocumentVersionsPublisher] = primitive(_.listDocumentVersionsPaginator(a))
    override def listDocuments: Kleisli[M, SsmAsyncClient, ListDocumentsResponse] = eff(_.listDocuments)
    override def listDocuments(a: ListDocumentsRequest): Kleisli[M, SsmAsyncClient, ListDocumentsResponse] = eff(
      _.listDocuments(a)
    )
    override def listDocumentsPaginator: Kleisli[M, SsmAsyncClient, ListDocumentsPublisher] = primitive(
      _.listDocumentsPaginator
    )
    override def listDocumentsPaginator(a: ListDocumentsRequest): Kleisli[M, SsmAsyncClient, ListDocumentsPublisher] =
      primitive(_.listDocumentsPaginator(a))
    override def listInventoryEntries(
        a: ListInventoryEntriesRequest
    ): Kleisli[M, SsmAsyncClient, ListInventoryEntriesResponse] = eff(_.listInventoryEntries(a))
    override def listNodes(a: ListNodesRequest): Kleisli[M, SsmAsyncClient, ListNodesResponse] = eff(_.listNodes(a))
    override def listNodesPaginator(a: ListNodesRequest): Kleisli[M, SsmAsyncClient, ListNodesPublisher] = primitive(
      _.listNodesPaginator(a)
    )
    override def listNodesSummary(a: ListNodesSummaryRequest): Kleisli[M, SsmAsyncClient, ListNodesSummaryResponse] =
      eff(_.listNodesSummary(a))
    override def listNodesSummaryPaginator(
        a: ListNodesSummaryRequest
    ): Kleisli[M, SsmAsyncClient, ListNodesSummaryPublisher] = primitive(_.listNodesSummaryPaginator(a))
    override def listOpsItemEvents(a: ListOpsItemEventsRequest): Kleisli[M, SsmAsyncClient, ListOpsItemEventsResponse] =
      eff(_.listOpsItemEvents(a))
    override def listOpsItemEventsPaginator(
        a: ListOpsItemEventsRequest
    ): Kleisli[M, SsmAsyncClient, ListOpsItemEventsPublisher] = primitive(_.listOpsItemEventsPaginator(a))
    override def listOpsItemRelatedItems(
        a: ListOpsItemRelatedItemsRequest
    ): Kleisli[M, SsmAsyncClient, ListOpsItemRelatedItemsResponse] = eff(_.listOpsItemRelatedItems(a))
    override def listOpsItemRelatedItemsPaginator(
        a: ListOpsItemRelatedItemsRequest
    ): Kleisli[M, SsmAsyncClient, ListOpsItemRelatedItemsPublisher] = primitive(_.listOpsItemRelatedItemsPaginator(a))
    override def listOpsMetadata(a: ListOpsMetadataRequest): Kleisli[M, SsmAsyncClient, ListOpsMetadataResponse] = eff(
      _.listOpsMetadata(a)
    )
    override def listOpsMetadataPaginator(
        a: ListOpsMetadataRequest
    ): Kleisli[M, SsmAsyncClient, ListOpsMetadataPublisher] = primitive(_.listOpsMetadataPaginator(a))
    override def listResourceComplianceSummaries: Kleisli[M, SsmAsyncClient, ListResourceComplianceSummariesResponse] =
      eff(_.listResourceComplianceSummaries)
    override def listResourceComplianceSummaries(
        a: ListResourceComplianceSummariesRequest
    ): Kleisli[M, SsmAsyncClient, ListResourceComplianceSummariesResponse] = eff(_.listResourceComplianceSummaries(a))
    override def listResourceComplianceSummariesPaginator
        : Kleisli[M, SsmAsyncClient, ListResourceComplianceSummariesPublisher] = primitive(
      _.listResourceComplianceSummariesPaginator
    )
    override def listResourceComplianceSummariesPaginator(
        a: ListResourceComplianceSummariesRequest
    ): Kleisli[M, SsmAsyncClient, ListResourceComplianceSummariesPublisher] = primitive(
      _.listResourceComplianceSummariesPaginator(a)
    )
    override def listResourceDataSync: Kleisli[M, SsmAsyncClient, ListResourceDataSyncResponse] = eff(
      _.listResourceDataSync
    )
    override def listResourceDataSync(
        a: ListResourceDataSyncRequest
    ): Kleisli[M, SsmAsyncClient, ListResourceDataSyncResponse] = eff(_.listResourceDataSync(a))
    override def listResourceDataSyncPaginator: Kleisli[M, SsmAsyncClient, ListResourceDataSyncPublisher] = primitive(
      _.listResourceDataSyncPaginator
    )
    override def listResourceDataSyncPaginator(
        a: ListResourceDataSyncRequest
    ): Kleisli[M, SsmAsyncClient, ListResourceDataSyncPublisher] = primitive(_.listResourceDataSyncPaginator(a))
    override def listTagsForResource(
        a: ListTagsForResourceRequest
    ): Kleisli[M, SsmAsyncClient, ListTagsForResourceResponse] = eff(_.listTagsForResource(a))
    override def modifyDocumentPermission(
        a: ModifyDocumentPermissionRequest
    ): Kleisli[M, SsmAsyncClient, ModifyDocumentPermissionResponse] = eff(_.modifyDocumentPermission(a))
    override def putComplianceItems(
        a: PutComplianceItemsRequest
    ): Kleisli[M, SsmAsyncClient, PutComplianceItemsResponse] = eff(_.putComplianceItems(a))
    override def putInventory(a: PutInventoryRequest): Kleisli[M, SsmAsyncClient, PutInventoryResponse] = eff(
      _.putInventory(a)
    )
    override def putParameter(a: PutParameterRequest): Kleisli[M, SsmAsyncClient, PutParameterResponse] = eff(
      _.putParameter(a)
    )
    override def putResourcePolicy(a: PutResourcePolicyRequest): Kleisli[M, SsmAsyncClient, PutResourcePolicyResponse] =
      eff(_.putResourcePolicy(a))
    override def registerDefaultPatchBaseline(
        a: RegisterDefaultPatchBaselineRequest
    ): Kleisli[M, SsmAsyncClient, RegisterDefaultPatchBaselineResponse] = eff(_.registerDefaultPatchBaseline(a))
    override def registerPatchBaselineForPatchGroup(
        a: RegisterPatchBaselineForPatchGroupRequest
    ): Kleisli[M, SsmAsyncClient, RegisterPatchBaselineForPatchGroupResponse] = eff(
      _.registerPatchBaselineForPatchGroup(a)
    )
    override def registerTargetWithMaintenanceWindow(
        a: RegisterTargetWithMaintenanceWindowRequest
    ): Kleisli[M, SsmAsyncClient, RegisterTargetWithMaintenanceWindowResponse] = eff(
      _.registerTargetWithMaintenanceWindow(a)
    )
    override def registerTaskWithMaintenanceWindow(
        a: RegisterTaskWithMaintenanceWindowRequest
    ): Kleisli[M, SsmAsyncClient, RegisterTaskWithMaintenanceWindowResponse] = eff(
      _.registerTaskWithMaintenanceWindow(a)
    )
    override def removeTagsFromResource(
        a: RemoveTagsFromResourceRequest
    ): Kleisli[M, SsmAsyncClient, RemoveTagsFromResourceResponse] = eff(_.removeTagsFromResource(a))
    override def resetServiceSetting(
        a: ResetServiceSettingRequest
    ): Kleisli[M, SsmAsyncClient, ResetServiceSettingResponse] = eff(_.resetServiceSetting(a))
    override def resumeSession(a: ResumeSessionRequest): Kleisli[M, SsmAsyncClient, ResumeSessionResponse] = eff(
      _.resumeSession(a)
    )
    override def sendAutomationSignal(
        a: SendAutomationSignalRequest
    ): Kleisli[M, SsmAsyncClient, SendAutomationSignalResponse] = eff(_.sendAutomationSignal(a))
    override def sendCommand(a: SendCommandRequest): Kleisli[M, SsmAsyncClient, SendCommandResponse] = eff(
      _.sendCommand(a)
    )
    override def serviceClientConfiguration: Kleisli[M, SsmAsyncClient, SsmServiceClientConfiguration] = primitive(
      _.serviceClientConfiguration
    )
    override def serviceName: Kleisli[M, SsmAsyncClient, String] = primitive(_.serviceName)
    override def startAccessRequest(
        a: StartAccessRequestRequest
    ): Kleisli[M, SsmAsyncClient, StartAccessRequestResponse] = eff(_.startAccessRequest(a))
    override def startAssociationsOnce(
        a: StartAssociationsOnceRequest
    ): Kleisli[M, SsmAsyncClient, StartAssociationsOnceResponse] = eff(_.startAssociationsOnce(a))
    override def startAutomationExecution(
        a: StartAutomationExecutionRequest
    ): Kleisli[M, SsmAsyncClient, StartAutomationExecutionResponse] = eff(_.startAutomationExecution(a))
    override def startChangeRequestExecution(
        a: StartChangeRequestExecutionRequest
    ): Kleisli[M, SsmAsyncClient, StartChangeRequestExecutionResponse] = eff(_.startChangeRequestExecution(a))
    override def startExecutionPreview(
        a: StartExecutionPreviewRequest
    ): Kleisli[M, SsmAsyncClient, StartExecutionPreviewResponse] = eff(_.startExecutionPreview(a))
    override def startSession(a: StartSessionRequest): Kleisli[M, SsmAsyncClient, StartSessionResponse] = eff(
      _.startSession(a)
    )
    override def stopAutomationExecution(
        a: StopAutomationExecutionRequest
    ): Kleisli[M, SsmAsyncClient, StopAutomationExecutionResponse] = eff(_.stopAutomationExecution(a))
    override def terminateSession(a: TerminateSessionRequest): Kleisli[M, SsmAsyncClient, TerminateSessionResponse] =
      eff(_.terminateSession(a))
    override def unlabelParameterVersion(
        a: UnlabelParameterVersionRequest
    ): Kleisli[M, SsmAsyncClient, UnlabelParameterVersionResponse] = eff(_.unlabelParameterVersion(a))
    override def updateAssociation(a: UpdateAssociationRequest): Kleisli[M, SsmAsyncClient, UpdateAssociationResponse] =
      eff(_.updateAssociation(a))
    override def updateAssociationStatus(
        a: UpdateAssociationStatusRequest
    ): Kleisli[M, SsmAsyncClient, UpdateAssociationStatusResponse] = eff(_.updateAssociationStatus(a))
    override def updateCloudConnector(
        a: UpdateCloudConnectorRequest
    ): Kleisli[M, SsmAsyncClient, UpdateCloudConnectorResponse] = eff(_.updateCloudConnector(a))
    override def updateDocument(a: UpdateDocumentRequest): Kleisli[M, SsmAsyncClient, UpdateDocumentResponse] = eff(
      _.updateDocument(a)
    )
    override def updateDocumentDefaultVersion(
        a: UpdateDocumentDefaultVersionRequest
    ): Kleisli[M, SsmAsyncClient, UpdateDocumentDefaultVersionResponse] = eff(_.updateDocumentDefaultVersion(a))
    override def updateDocumentMetadata(
        a: UpdateDocumentMetadataRequest
    ): Kleisli[M, SsmAsyncClient, UpdateDocumentMetadataResponse] = eff(_.updateDocumentMetadata(a))
    override def updateMaintenanceWindow(
        a: UpdateMaintenanceWindowRequest
    ): Kleisli[M, SsmAsyncClient, UpdateMaintenanceWindowResponse] = eff(_.updateMaintenanceWindow(a))
    override def updateMaintenanceWindowTarget(
        a: UpdateMaintenanceWindowTargetRequest
    ): Kleisli[M, SsmAsyncClient, UpdateMaintenanceWindowTargetResponse] = eff(_.updateMaintenanceWindowTarget(a))
    override def updateMaintenanceWindowTask(
        a: UpdateMaintenanceWindowTaskRequest
    ): Kleisli[M, SsmAsyncClient, UpdateMaintenanceWindowTaskResponse] = eff(_.updateMaintenanceWindowTask(a))
    override def updateManagedInstanceRole(
        a: UpdateManagedInstanceRoleRequest
    ): Kleisli[M, SsmAsyncClient, UpdateManagedInstanceRoleResponse] = eff(_.updateManagedInstanceRole(a))
    override def updateOpsItem(a: UpdateOpsItemRequest): Kleisli[M, SsmAsyncClient, UpdateOpsItemResponse] = eff(
      _.updateOpsItem(a)
    )
    override def updateOpsMetadata(a: UpdateOpsMetadataRequest): Kleisli[M, SsmAsyncClient, UpdateOpsMetadataResponse] =
      eff(_.updateOpsMetadata(a))
    override def updatePatchBaseline(
        a: UpdatePatchBaselineRequest
    ): Kleisli[M, SsmAsyncClient, UpdatePatchBaselineResponse] = eff(_.updatePatchBaseline(a))
    override def updateResourceDataSync(
        a: UpdateResourceDataSyncRequest
    ): Kleisli[M, SsmAsyncClient, UpdateResourceDataSyncResponse] = eff(_.updateResourceDataSync(a))
    override def updateServiceSetting(
        a: UpdateServiceSettingRequest
    ): Kleisli[M, SsmAsyncClient, UpdateServiceSettingResponse] = eff(_.updateServiceSetting(a))
    override def validateCloudConnector(
        a: ValidateCloudConnectorRequest
    ): Kleisli[M, SsmAsyncClient, ValidateCloudConnectorResponse] = eff(_.validateCloudConnector(a))
    override def validateCloudConnectorPaginator(
        a: ValidateCloudConnectorRequest
    ): Kleisli[M, SsmAsyncClient, ValidateCloudConnectorPublisher]  = primitive(_.validateCloudConnectorPaginator(a))
    override def waiter: Kleisli[M, SsmAsyncClient, SsmAsyncWaiter] = primitive(_.waiter)

    def lens[E](f: E => SsmAsyncClient): SsmAsyncClientOp[Kleisli[M, E, *]] =
      new SsmAsyncClientOp[Kleisli[M, E, *]] {
        override def addTagsToResource(a: AddTagsToResourceRequest): Kleisli[M, E, AddTagsToResourceResponse] =
          Kleisli(e => eff1(f(e).addTagsToResource(a)))
        override def associateOpsItemRelatedItem(
            a: AssociateOpsItemRelatedItemRequest
        ): Kleisli[M, E, AssociateOpsItemRelatedItemResponse] = Kleisli(e => eff1(f(e).associateOpsItemRelatedItem(a)))
        override def cancelCommand(a: CancelCommandRequest): Kleisli[M, E, CancelCommandResponse] =
          Kleisli(e => eff1(f(e).cancelCommand(a)))
        override def cancelMaintenanceWindowExecution(
            a: CancelMaintenanceWindowExecutionRequest
        ): Kleisli[M, E, CancelMaintenanceWindowExecutionResponse] =
          Kleisli(e => eff1(f(e).cancelMaintenanceWindowExecution(a)))
        override def close: Kleisli[M, E, Unit] = Kleisli(e => primitive1(f(e).close))
        override def createActivation(a: CreateActivationRequest): Kleisli[M, E, CreateActivationResponse] =
          Kleisli(e => eff1(f(e).createActivation(a)))
        override def createAssociation(a: CreateAssociationRequest): Kleisli[M, E, CreateAssociationResponse] =
          Kleisli(e => eff1(f(e).createAssociation(a)))
        override def createAssociationBatch(
            a: CreateAssociationBatchRequest
        ): Kleisli[M, E, CreateAssociationBatchResponse] = Kleisli(e => eff1(f(e).createAssociationBatch(a)))
        override def createCloudConnector(a: CreateCloudConnectorRequest): Kleisli[M, E, CreateCloudConnectorResponse] =
          Kleisli(e => eff1(f(e).createCloudConnector(a)))
        override def createDocument(a: CreateDocumentRequest): Kleisli[M, E, CreateDocumentResponse] =
          Kleisli(e => eff1(f(e).createDocument(a)))
        override def createMaintenanceWindow(
            a: CreateMaintenanceWindowRequest
        ): Kleisli[M, E, CreateMaintenanceWindowResponse] = Kleisli(e => eff1(f(e).createMaintenanceWindow(a)))
        override def createOpsItem(a: CreateOpsItemRequest): Kleisli[M, E, CreateOpsItemResponse] =
          Kleisli(e => eff1(f(e).createOpsItem(a)))
        override def createOpsMetadata(a: CreateOpsMetadataRequest): Kleisli[M, E, CreateOpsMetadataResponse] =
          Kleisli(e => eff1(f(e).createOpsMetadata(a)))
        override def createPatchBaseline(a: CreatePatchBaselineRequest): Kleisli[M, E, CreatePatchBaselineResponse] =
          Kleisli(e => eff1(f(e).createPatchBaseline(a)))
        override def createResourceDataSync(
            a: CreateResourceDataSyncRequest
        ): Kleisli[M, E, CreateResourceDataSyncResponse] = Kleisli(e => eff1(f(e).createResourceDataSync(a)))
        override def deleteActivation(a: DeleteActivationRequest): Kleisli[M, E, DeleteActivationResponse] =
          Kleisli(e => eff1(f(e).deleteActivation(a)))
        override def deleteAssociation(a: DeleteAssociationRequest): Kleisli[M, E, DeleteAssociationResponse] =
          Kleisli(e => eff1(f(e).deleteAssociation(a)))
        override def deleteCloudConnector(a: DeleteCloudConnectorRequest): Kleisli[M, E, DeleteCloudConnectorResponse] =
          Kleisli(e => eff1(f(e).deleteCloudConnector(a)))
        override def deleteDocument(a: DeleteDocumentRequest): Kleisli[M, E, DeleteDocumentResponse] =
          Kleisli(e => eff1(f(e).deleteDocument(a)))
        override def deleteInventory(a: DeleteInventoryRequest): Kleisli[M, E, DeleteInventoryResponse] =
          Kleisli(e => eff1(f(e).deleteInventory(a)))
        override def deleteMaintenanceWindow(
            a: DeleteMaintenanceWindowRequest
        ): Kleisli[M, E, DeleteMaintenanceWindowResponse] = Kleisli(e => eff1(f(e).deleteMaintenanceWindow(a)))
        override def deleteOpsItem(a: DeleteOpsItemRequest): Kleisli[M, E, DeleteOpsItemResponse] =
          Kleisli(e => eff1(f(e).deleteOpsItem(a)))
        override def deleteOpsMetadata(a: DeleteOpsMetadataRequest): Kleisli[M, E, DeleteOpsMetadataResponse] =
          Kleisli(e => eff1(f(e).deleteOpsMetadata(a)))
        override def deleteParameter(a: DeleteParameterRequest): Kleisli[M, E, DeleteParameterResponse] =
          Kleisli(e => eff1(f(e).deleteParameter(a)))
        override def deleteParameters(a: DeleteParametersRequest): Kleisli[M, E, DeleteParametersResponse] =
          Kleisli(e => eff1(f(e).deleteParameters(a)))
        override def deletePatchBaseline(a: DeletePatchBaselineRequest): Kleisli[M, E, DeletePatchBaselineResponse] =
          Kleisli(e => eff1(f(e).deletePatchBaseline(a)))
        override def deleteResourceDataSync(
            a: DeleteResourceDataSyncRequest
        ): Kleisli[M, E, DeleteResourceDataSyncResponse] = Kleisli(e => eff1(f(e).deleteResourceDataSync(a)))
        override def deleteResourcePolicy(a: DeleteResourcePolicyRequest): Kleisli[M, E, DeleteResourcePolicyResponse] =
          Kleisli(e => eff1(f(e).deleteResourcePolicy(a)))
        override def deregisterManagedInstance(
            a: DeregisterManagedInstanceRequest
        ): Kleisli[M, E, DeregisterManagedInstanceResponse] = Kleisli(e => eff1(f(e).deregisterManagedInstance(a)))
        override def deregisterPatchBaselineForPatchGroup(
            a: DeregisterPatchBaselineForPatchGroupRequest
        ): Kleisli[M, E, DeregisterPatchBaselineForPatchGroupResponse] =
          Kleisli(e => eff1(f(e).deregisterPatchBaselineForPatchGroup(a)))
        override def deregisterTargetFromMaintenanceWindow(
            a: DeregisterTargetFromMaintenanceWindowRequest
        ): Kleisli[M, E, DeregisterTargetFromMaintenanceWindowResponse] =
          Kleisli(e => eff1(f(e).deregisterTargetFromMaintenanceWindow(a)))
        override def deregisterTaskFromMaintenanceWindow(
            a: DeregisterTaskFromMaintenanceWindowRequest
        ): Kleisli[M, E, DeregisterTaskFromMaintenanceWindowResponse] =
          Kleisli(e => eff1(f(e).deregisterTaskFromMaintenanceWindow(a)))
        override def describeActivations: Kleisli[M, E, DescribeActivationsResponse] =
          Kleisli(e => eff1(f(e).describeActivations))
        override def describeActivations(a: DescribeActivationsRequest): Kleisli[M, E, DescribeActivationsResponse] =
          Kleisli(e => eff1(f(e).describeActivations(a)))
        override def describeActivationsPaginator: Kleisli[M, E, DescribeActivationsPublisher] =
          Kleisli(e => primitive1(f(e).describeActivationsPaginator))
        override def describeActivationsPaginator(
            a: DescribeActivationsRequest
        ): Kleisli[M, E, DescribeActivationsPublisher] = Kleisli(e => primitive1(f(e).describeActivationsPaginator(a)))
        override def describeAssociation(a: DescribeAssociationRequest): Kleisli[M, E, DescribeAssociationResponse] =
          Kleisli(e => eff1(f(e).describeAssociation(a)))
        override def describeAssociationExecutionTargets(
            a: DescribeAssociationExecutionTargetsRequest
        ): Kleisli[M, E, DescribeAssociationExecutionTargetsResponse] =
          Kleisli(e => eff1(f(e).describeAssociationExecutionTargets(a)))
        override def describeAssociationExecutionTargetsPaginator(
            a: DescribeAssociationExecutionTargetsRequest
        ): Kleisli[M, E, DescribeAssociationExecutionTargetsPublisher] =
          Kleisli(e => primitive1(f(e).describeAssociationExecutionTargetsPaginator(a)))
        override def describeAssociationExecutions(
            a: DescribeAssociationExecutionsRequest
        ): Kleisli[M, E, DescribeAssociationExecutionsResponse] =
          Kleisli(e => eff1(f(e).describeAssociationExecutions(a)))
        override def describeAssociationExecutionsPaginator(
            a: DescribeAssociationExecutionsRequest
        ): Kleisli[M, E, DescribeAssociationExecutionsPublisher] =
          Kleisli(e => primitive1(f(e).describeAssociationExecutionsPaginator(a)))
        override def describeAutomationExecutions: Kleisli[M, E, DescribeAutomationExecutionsResponse] =
          Kleisli(e => eff1(f(e).describeAutomationExecutions))
        override def describeAutomationExecutions(
            a: DescribeAutomationExecutionsRequest
        ): Kleisli[M, E, DescribeAutomationExecutionsResponse] =
          Kleisli(e => eff1(f(e).describeAutomationExecutions(a)))
        override def describeAutomationExecutionsPaginator: Kleisli[M, E, DescribeAutomationExecutionsPublisher] =
          Kleisli(e => primitive1(f(e).describeAutomationExecutionsPaginator))
        override def describeAutomationExecutionsPaginator(
            a: DescribeAutomationExecutionsRequest
        ): Kleisli[M, E, DescribeAutomationExecutionsPublisher] =
          Kleisli(e => primitive1(f(e).describeAutomationExecutionsPaginator(a)))
        override def describeAutomationStepExecutions(
            a: DescribeAutomationStepExecutionsRequest
        ): Kleisli[M, E, DescribeAutomationStepExecutionsResponse] =
          Kleisli(e => eff1(f(e).describeAutomationStepExecutions(a)))
        override def describeAutomationStepExecutionsPaginator(
            a: DescribeAutomationStepExecutionsRequest
        ): Kleisli[M, E, DescribeAutomationStepExecutionsPublisher] =
          Kleisli(e => primitive1(f(e).describeAutomationStepExecutionsPaginator(a)))
        override def describeAvailablePatches: Kleisli[M, E, DescribeAvailablePatchesResponse] =
          Kleisli(e => eff1(f(e).describeAvailablePatches))
        override def describeAvailablePatches(
            a: DescribeAvailablePatchesRequest
        ): Kleisli[M, E, DescribeAvailablePatchesResponse] = Kleisli(e => eff1(f(e).describeAvailablePatches(a)))
        override def describeAvailablePatchesPaginator: Kleisli[M, E, DescribeAvailablePatchesPublisher] =
          Kleisli(e => primitive1(f(e).describeAvailablePatchesPaginator))
        override def describeAvailablePatchesPaginator(
            a: DescribeAvailablePatchesRequest
        ): Kleisli[M, E, DescribeAvailablePatchesPublisher] =
          Kleisli(e => primitive1(f(e).describeAvailablePatchesPaginator(a)))
        override def describeDocument(a: DescribeDocumentRequest): Kleisli[M, E, DescribeDocumentResponse] =
          Kleisli(e => eff1(f(e).describeDocument(a)))
        override def describeDocumentPermission(
            a: DescribeDocumentPermissionRequest
        ): Kleisli[M, E, DescribeDocumentPermissionResponse] = Kleisli(e => eff1(f(e).describeDocumentPermission(a)))
        override def describeEffectiveInstanceAssociations(
            a: DescribeEffectiveInstanceAssociationsRequest
        ): Kleisli[M, E, DescribeEffectiveInstanceAssociationsResponse] =
          Kleisli(e => eff1(f(e).describeEffectiveInstanceAssociations(a)))
        override def describeEffectiveInstanceAssociationsPaginator(
            a: DescribeEffectiveInstanceAssociationsRequest
        ): Kleisli[M, E, DescribeEffectiveInstanceAssociationsPublisher] =
          Kleisli(e => primitive1(f(e).describeEffectiveInstanceAssociationsPaginator(a)))
        override def describeEffectivePatchesForPatchBaseline(
            a: DescribeEffectivePatchesForPatchBaselineRequest
        ): Kleisli[M, E, DescribeEffectivePatchesForPatchBaselineResponse] =
          Kleisli(e => eff1(f(e).describeEffectivePatchesForPatchBaseline(a)))
        override def describeEffectivePatchesForPatchBaselinePaginator(
            a: DescribeEffectivePatchesForPatchBaselineRequest
        ): Kleisli[M, E, DescribeEffectivePatchesForPatchBaselinePublisher] =
          Kleisli(e => primitive1(f(e).describeEffectivePatchesForPatchBaselinePaginator(a)))
        override def describeInstanceAssociationsStatus(
            a: DescribeInstanceAssociationsStatusRequest
        ): Kleisli[M, E, DescribeInstanceAssociationsStatusResponse] =
          Kleisli(e => eff1(f(e).describeInstanceAssociationsStatus(a)))
        override def describeInstanceAssociationsStatusPaginator(
            a: DescribeInstanceAssociationsStatusRequest
        ): Kleisli[M, E, DescribeInstanceAssociationsStatusPublisher] =
          Kleisli(e => primitive1(f(e).describeInstanceAssociationsStatusPaginator(a)))
        override def describeInstanceInformation: Kleisli[M, E, DescribeInstanceInformationResponse] =
          Kleisli(e => eff1(f(e).describeInstanceInformation))
        override def describeInstanceInformation(
            a: DescribeInstanceInformationRequest
        ): Kleisli[M, E, DescribeInstanceInformationResponse] = Kleisli(e => eff1(f(e).describeInstanceInformation(a)))
        override def describeInstanceInformationPaginator: Kleisli[M, E, DescribeInstanceInformationPublisher] =
          Kleisli(e => primitive1(f(e).describeInstanceInformationPaginator))
        override def describeInstanceInformationPaginator(
            a: DescribeInstanceInformationRequest
        ): Kleisli[M, E, DescribeInstanceInformationPublisher] =
          Kleisli(e => primitive1(f(e).describeInstanceInformationPaginator(a)))
        override def describeInstancePatchStates(
            a: DescribeInstancePatchStatesRequest
        ): Kleisli[M, E, DescribeInstancePatchStatesResponse] = Kleisli(e => eff1(f(e).describeInstancePatchStates(a)))
        override def describeInstancePatchStatesForPatchGroup(
            a: DescribeInstancePatchStatesForPatchGroupRequest
        ): Kleisli[M, E, DescribeInstancePatchStatesForPatchGroupResponse] =
          Kleisli(e => eff1(f(e).describeInstancePatchStatesForPatchGroup(a)))
        override def describeInstancePatchStatesForPatchGroupPaginator(
            a: DescribeInstancePatchStatesForPatchGroupRequest
        ): Kleisli[M, E, DescribeInstancePatchStatesForPatchGroupPublisher] =
          Kleisli(e => primitive1(f(e).describeInstancePatchStatesForPatchGroupPaginator(a)))
        override def describeInstancePatchStatesPaginator(
            a: DescribeInstancePatchStatesRequest
        ): Kleisli[M, E, DescribeInstancePatchStatesPublisher] =
          Kleisli(e => primitive1(f(e).describeInstancePatchStatesPaginator(a)))
        override def describeInstancePatches(
            a: DescribeInstancePatchesRequest
        ): Kleisli[M, E, DescribeInstancePatchesResponse] = Kleisli(e => eff1(f(e).describeInstancePatches(a)))
        override def describeInstancePatchesPaginator(
            a: DescribeInstancePatchesRequest
        ): Kleisli[M, E, DescribeInstancePatchesPublisher] =
          Kleisli(e => primitive1(f(e).describeInstancePatchesPaginator(a)))
        override def describeInstanceProperties(
            a: DescribeInstancePropertiesRequest
        ): Kleisli[M, E, DescribeInstancePropertiesResponse] = Kleisli(e => eff1(f(e).describeInstanceProperties(a)))
        override def describeInstancePropertiesPaginator(
            a: DescribeInstancePropertiesRequest
        ): Kleisli[M, E, DescribeInstancePropertiesPublisher] =
          Kleisli(e => primitive1(f(e).describeInstancePropertiesPaginator(a)))
        override def describeInventoryDeletions: Kleisli[M, E, DescribeInventoryDeletionsResponse] =
          Kleisli(e => eff1(f(e).describeInventoryDeletions))
        override def describeInventoryDeletions(
            a: DescribeInventoryDeletionsRequest
        ): Kleisli[M, E, DescribeInventoryDeletionsResponse] = Kleisli(e => eff1(f(e).describeInventoryDeletions(a)))
        override def describeInventoryDeletionsPaginator: Kleisli[M, E, DescribeInventoryDeletionsPublisher] =
          Kleisli(e => primitive1(f(e).describeInventoryDeletionsPaginator))
        override def describeInventoryDeletionsPaginator(
            a: DescribeInventoryDeletionsRequest
        ): Kleisli[M, E, DescribeInventoryDeletionsPublisher] =
          Kleisli(e => primitive1(f(e).describeInventoryDeletionsPaginator(a)))
        override def describeMaintenanceWindowExecutionTaskInvocations(
            a: DescribeMaintenanceWindowExecutionTaskInvocationsRequest
        ): Kleisli[M, E, DescribeMaintenanceWindowExecutionTaskInvocationsResponse] =
          Kleisli(e => eff1(f(e).describeMaintenanceWindowExecutionTaskInvocations(a)))
        override def describeMaintenanceWindowExecutionTaskInvocationsPaginator(
            a: DescribeMaintenanceWindowExecutionTaskInvocationsRequest
        ): Kleisli[M, E, DescribeMaintenanceWindowExecutionTaskInvocationsPublisher] =
          Kleisli(e => primitive1(f(e).describeMaintenanceWindowExecutionTaskInvocationsPaginator(a)))
        override def describeMaintenanceWindowExecutionTasks(
            a: DescribeMaintenanceWindowExecutionTasksRequest
        ): Kleisli[M, E, DescribeMaintenanceWindowExecutionTasksResponse] =
          Kleisli(e => eff1(f(e).describeMaintenanceWindowExecutionTasks(a)))
        override def describeMaintenanceWindowExecutionTasksPaginator(
            a: DescribeMaintenanceWindowExecutionTasksRequest
        ): Kleisli[M, E, DescribeMaintenanceWindowExecutionTasksPublisher] =
          Kleisli(e => primitive1(f(e).describeMaintenanceWindowExecutionTasksPaginator(a)))
        override def describeMaintenanceWindowExecutions(
            a: DescribeMaintenanceWindowExecutionsRequest
        ): Kleisli[M, E, DescribeMaintenanceWindowExecutionsResponse] =
          Kleisli(e => eff1(f(e).describeMaintenanceWindowExecutions(a)))
        override def describeMaintenanceWindowExecutionsPaginator(
            a: DescribeMaintenanceWindowExecutionsRequest
        ): Kleisli[M, E, DescribeMaintenanceWindowExecutionsPublisher] =
          Kleisli(e => primitive1(f(e).describeMaintenanceWindowExecutionsPaginator(a)))
        override def describeMaintenanceWindowSchedule(
            a: DescribeMaintenanceWindowScheduleRequest
        ): Kleisli[M, E, DescribeMaintenanceWindowScheduleResponse] =
          Kleisli(e => eff1(f(e).describeMaintenanceWindowSchedule(a)))
        override def describeMaintenanceWindowSchedulePaginator(
            a: DescribeMaintenanceWindowScheduleRequest
        ): Kleisli[M, E, DescribeMaintenanceWindowSchedulePublisher] =
          Kleisli(e => primitive1(f(e).describeMaintenanceWindowSchedulePaginator(a)))
        override def describeMaintenanceWindowTargets(
            a: DescribeMaintenanceWindowTargetsRequest
        ): Kleisli[M, E, DescribeMaintenanceWindowTargetsResponse] =
          Kleisli(e => eff1(f(e).describeMaintenanceWindowTargets(a)))
        override def describeMaintenanceWindowTargetsPaginator(
            a: DescribeMaintenanceWindowTargetsRequest
        ): Kleisli[M, E, DescribeMaintenanceWindowTargetsPublisher] =
          Kleisli(e => primitive1(f(e).describeMaintenanceWindowTargetsPaginator(a)))
        override def describeMaintenanceWindowTasks(
            a: DescribeMaintenanceWindowTasksRequest
        ): Kleisli[M, E, DescribeMaintenanceWindowTasksResponse] =
          Kleisli(e => eff1(f(e).describeMaintenanceWindowTasks(a)))
        override def describeMaintenanceWindowTasksPaginator(
            a: DescribeMaintenanceWindowTasksRequest
        ): Kleisli[M, E, DescribeMaintenanceWindowTasksPublisher] =
          Kleisli(e => primitive1(f(e).describeMaintenanceWindowTasksPaginator(a)))
        override def describeMaintenanceWindows: Kleisli[M, E, DescribeMaintenanceWindowsResponse] =
          Kleisli(e => eff1(f(e).describeMaintenanceWindows))
        override def describeMaintenanceWindows(
            a: DescribeMaintenanceWindowsRequest
        ): Kleisli[M, E, DescribeMaintenanceWindowsResponse] = Kleisli(e => eff1(f(e).describeMaintenanceWindows(a)))
        override def describeMaintenanceWindowsForTarget(
            a: DescribeMaintenanceWindowsForTargetRequest
        ): Kleisli[M, E, DescribeMaintenanceWindowsForTargetResponse] =
          Kleisli(e => eff1(f(e).describeMaintenanceWindowsForTarget(a)))
        override def describeMaintenanceWindowsForTargetPaginator(
            a: DescribeMaintenanceWindowsForTargetRequest
        ): Kleisli[M, E, DescribeMaintenanceWindowsForTargetPublisher] =
          Kleisli(e => primitive1(f(e).describeMaintenanceWindowsForTargetPaginator(a)))
        override def describeMaintenanceWindowsPaginator: Kleisli[M, E, DescribeMaintenanceWindowsPublisher] =
          Kleisli(e => primitive1(f(e).describeMaintenanceWindowsPaginator))
        override def describeMaintenanceWindowsPaginator(
            a: DescribeMaintenanceWindowsRequest
        ): Kleisli[M, E, DescribeMaintenanceWindowsPublisher] =
          Kleisli(e => primitive1(f(e).describeMaintenanceWindowsPaginator(a)))
        override def describeOpsItems(a: DescribeOpsItemsRequest): Kleisli[M, E, DescribeOpsItemsResponse] =
          Kleisli(e => eff1(f(e).describeOpsItems(a)))
        override def describeOpsItemsPaginator(a: DescribeOpsItemsRequest): Kleisli[M, E, DescribeOpsItemsPublisher] =
          Kleisli(e => primitive1(f(e).describeOpsItemsPaginator(a)))
        override def describeParameters: Kleisli[M, E, DescribeParametersResponse] =
          Kleisli(e => eff1(f(e).describeParameters))
        override def describeParameters(a: DescribeParametersRequest): Kleisli[M, E, DescribeParametersResponse] =
          Kleisli(e => eff1(f(e).describeParameters(a)))
        override def describeParametersPaginator: Kleisli[M, E, DescribeParametersPublisher] =
          Kleisli(e => primitive1(f(e).describeParametersPaginator))
        override def describeParametersPaginator(
            a: DescribeParametersRequest
        ): Kleisli[M, E, DescribeParametersPublisher] = Kleisli(e => primitive1(f(e).describeParametersPaginator(a)))
        override def describePatchBaselines: Kleisli[M, E, DescribePatchBaselinesResponse] =
          Kleisli(e => eff1(f(e).describePatchBaselines))
        override def describePatchBaselines(
            a: DescribePatchBaselinesRequest
        ): Kleisli[M, E, DescribePatchBaselinesResponse] = Kleisli(e => eff1(f(e).describePatchBaselines(a)))
        override def describePatchBaselinesPaginator: Kleisli[M, E, DescribePatchBaselinesPublisher] =
          Kleisli(e => primitive1(f(e).describePatchBaselinesPaginator))
        override def describePatchBaselinesPaginator(
            a: DescribePatchBaselinesRequest
        ): Kleisli[M, E, DescribePatchBaselinesPublisher] =
          Kleisli(e => primitive1(f(e).describePatchBaselinesPaginator(a)))
        override def describePatchGroupState(
            a: DescribePatchGroupStateRequest
        ): Kleisli[M, E, DescribePatchGroupStateResponse] = Kleisli(e => eff1(f(e).describePatchGroupState(a)))
        override def describePatchGroups: Kleisli[M, E, DescribePatchGroupsResponse] =
          Kleisli(e => eff1(f(e).describePatchGroups))
        override def describePatchGroups(a: DescribePatchGroupsRequest): Kleisli[M, E, DescribePatchGroupsResponse] =
          Kleisli(e => eff1(f(e).describePatchGroups(a)))
        override def describePatchGroupsPaginator: Kleisli[M, E, DescribePatchGroupsPublisher] =
          Kleisli(e => primitive1(f(e).describePatchGroupsPaginator))
        override def describePatchGroupsPaginator(
            a: DescribePatchGroupsRequest
        ): Kleisli[M, E, DescribePatchGroupsPublisher] = Kleisli(e => primitive1(f(e).describePatchGroupsPaginator(a)))
        override def describePatchProperties(
            a: DescribePatchPropertiesRequest
        ): Kleisli[M, E, DescribePatchPropertiesResponse] = Kleisli(e => eff1(f(e).describePatchProperties(a)))
        override def describePatchPropertiesPaginator(
            a: DescribePatchPropertiesRequest
        ): Kleisli[M, E, DescribePatchPropertiesPublisher] =
          Kleisli(e => primitive1(f(e).describePatchPropertiesPaginator(a)))
        override def describeSessions(a: DescribeSessionsRequest): Kleisli[M, E, DescribeSessionsResponse] =
          Kleisli(e => eff1(f(e).describeSessions(a)))
        override def describeSessionsPaginator(a: DescribeSessionsRequest): Kleisli[M, E, DescribeSessionsPublisher] =
          Kleisli(e => primitive1(f(e).describeSessionsPaginator(a)))
        override def disassociateOpsItemRelatedItem(
            a: DisassociateOpsItemRelatedItemRequest
        ): Kleisli[M, E, DisassociateOpsItemRelatedItemResponse] =
          Kleisli(e => eff1(f(e).disassociateOpsItemRelatedItem(a)))
        override def getAccessToken(a: GetAccessTokenRequest): Kleisli[M, E, GetAccessTokenResponse] =
          Kleisli(e => eff1(f(e).getAccessToken(a)))
        override def getAutomationExecution(
            a: GetAutomationExecutionRequest
        ): Kleisli[M, E, GetAutomationExecutionResponse] = Kleisli(e => eff1(f(e).getAutomationExecution(a)))
        override def getCalendarState(a: GetCalendarStateRequest): Kleisli[M, E, GetCalendarStateResponse] =
          Kleisli(e => eff1(f(e).getCalendarState(a)))
        override def getCloudConnector(a: GetCloudConnectorRequest): Kleisli[M, E, GetCloudConnectorResponse] =
          Kleisli(e => eff1(f(e).getCloudConnector(a)))
        override def getCommandInvocation(a: GetCommandInvocationRequest): Kleisli[M, E, GetCommandInvocationResponse] =
          Kleisli(e => eff1(f(e).getCommandInvocation(a)))
        override def getConnectionStatus(a: GetConnectionStatusRequest): Kleisli[M, E, GetConnectionStatusResponse] =
          Kleisli(e => eff1(f(e).getConnectionStatus(a)))
        override def getDefaultPatchBaseline: Kleisli[M, E, GetDefaultPatchBaselineResponse] =
          Kleisli(e => eff1(f(e).getDefaultPatchBaseline))
        override def getDefaultPatchBaseline(
            a: GetDefaultPatchBaselineRequest
        ): Kleisli[M, E, GetDefaultPatchBaselineResponse] = Kleisli(e => eff1(f(e).getDefaultPatchBaseline(a)))
        override def getDeployablePatchSnapshotForInstance(
            a: GetDeployablePatchSnapshotForInstanceRequest
        ): Kleisli[M, E, GetDeployablePatchSnapshotForInstanceResponse] =
          Kleisli(e => eff1(f(e).getDeployablePatchSnapshotForInstance(a)))
        override def getDocument(a: GetDocumentRequest): Kleisli[M, E, GetDocumentResponse] =
          Kleisli(e => eff1(f(e).getDocument(a)))
        override def getExecutionPreview(a: GetExecutionPreviewRequest): Kleisli[M, E, GetExecutionPreviewResponse] =
          Kleisli(e => eff1(f(e).getExecutionPreview(a)))
        override def getInventory: Kleisli[M, E, GetInventoryResponse] = Kleisli(e => eff1(f(e).getInventory))
        override def getInventory(a: GetInventoryRequest): Kleisli[M, E, GetInventoryResponse] =
          Kleisli(e => eff1(f(e).getInventory(a)))
        override def getInventoryPaginator: Kleisli[M, E, GetInventoryPublisher] =
          Kleisli(e => primitive1(f(e).getInventoryPaginator))
        override def getInventoryPaginator(a: GetInventoryRequest): Kleisli[M, E, GetInventoryPublisher] =
          Kleisli(e => primitive1(f(e).getInventoryPaginator(a)))
        override def getInventorySchema: Kleisli[M, E, GetInventorySchemaResponse] =
          Kleisli(e => eff1(f(e).getInventorySchema))
        override def getInventorySchema(a: GetInventorySchemaRequest): Kleisli[M, E, GetInventorySchemaResponse] =
          Kleisli(e => eff1(f(e).getInventorySchema(a)))
        override def getInventorySchemaPaginator: Kleisli[M, E, GetInventorySchemaPublisher] =
          Kleisli(e => primitive1(f(e).getInventorySchemaPaginator))
        override def getInventorySchemaPaginator(
            a: GetInventorySchemaRequest
        ): Kleisli[M, E, GetInventorySchemaPublisher] = Kleisli(e => primitive1(f(e).getInventorySchemaPaginator(a)))
        override def getMaintenanceWindow(a: GetMaintenanceWindowRequest): Kleisli[M, E, GetMaintenanceWindowResponse] =
          Kleisli(e => eff1(f(e).getMaintenanceWindow(a)))
        override def getMaintenanceWindowExecution(
            a: GetMaintenanceWindowExecutionRequest
        ): Kleisli[M, E, GetMaintenanceWindowExecutionResponse] =
          Kleisli(e => eff1(f(e).getMaintenanceWindowExecution(a)))
        override def getMaintenanceWindowExecutionTask(
            a: GetMaintenanceWindowExecutionTaskRequest
        ): Kleisli[M, E, GetMaintenanceWindowExecutionTaskResponse] =
          Kleisli(e => eff1(f(e).getMaintenanceWindowExecutionTask(a)))
        override def getMaintenanceWindowExecutionTaskInvocation(
            a: GetMaintenanceWindowExecutionTaskInvocationRequest
        ): Kleisli[M, E, GetMaintenanceWindowExecutionTaskInvocationResponse] =
          Kleisli(e => eff1(f(e).getMaintenanceWindowExecutionTaskInvocation(a)))
        override def getMaintenanceWindowTask(
            a: GetMaintenanceWindowTaskRequest
        ): Kleisli[M, E, GetMaintenanceWindowTaskResponse] = Kleisli(e => eff1(f(e).getMaintenanceWindowTask(a)))
        override def getOpsItem(a: GetOpsItemRequest): Kleisli[M, E, GetOpsItemResponse] =
          Kleisli(e => eff1(f(e).getOpsItem(a)))
        override def getOpsMetadata(a: GetOpsMetadataRequest): Kleisli[M, E, GetOpsMetadataResponse] =
          Kleisli(e => eff1(f(e).getOpsMetadata(a)))
        override def getOpsSummary(a: GetOpsSummaryRequest): Kleisli[M, E, GetOpsSummaryResponse] =
          Kleisli(e => eff1(f(e).getOpsSummary(a)))
        override def getOpsSummaryPaginator(a: GetOpsSummaryRequest): Kleisli[M, E, GetOpsSummaryPublisher] =
          Kleisli(e => primitive1(f(e).getOpsSummaryPaginator(a)))
        override def getParameter(a: GetParameterRequest): Kleisli[M, E, GetParameterResponse] =
          Kleisli(e => eff1(f(e).getParameter(a)))
        override def getParameterHistory(a: GetParameterHistoryRequest): Kleisli[M, E, GetParameterHistoryResponse] =
          Kleisli(e => eff1(f(e).getParameterHistory(a)))
        override def getParameterHistoryPaginator(
            a: GetParameterHistoryRequest
        ): Kleisli[M, E, GetParameterHistoryPublisher] = Kleisli(e => primitive1(f(e).getParameterHistoryPaginator(a)))
        override def getParameters(a: GetParametersRequest): Kleisli[M, E, GetParametersResponse] =
          Kleisli(e => eff1(f(e).getParameters(a)))
        override def getParametersByPath(a: GetParametersByPathRequest): Kleisli[M, E, GetParametersByPathResponse] =
          Kleisli(e => eff1(f(e).getParametersByPath(a)))
        override def getParametersByPathPaginator(
            a: GetParametersByPathRequest
        ): Kleisli[M, E, GetParametersByPathPublisher] = Kleisli(e => primitive1(f(e).getParametersByPathPaginator(a)))
        override def getPatchBaseline(a: GetPatchBaselineRequest): Kleisli[M, E, GetPatchBaselineResponse] =
          Kleisli(e => eff1(f(e).getPatchBaseline(a)))
        override def getPatchBaselineForPatchGroup(
            a: GetPatchBaselineForPatchGroupRequest
        ): Kleisli[M, E, GetPatchBaselineForPatchGroupResponse] =
          Kleisli(e => eff1(f(e).getPatchBaselineForPatchGroup(a)))
        override def getResourcePolicies(a: GetResourcePoliciesRequest): Kleisli[M, E, GetResourcePoliciesResponse] =
          Kleisli(e => eff1(f(e).getResourcePolicies(a)))
        override def getResourcePoliciesPaginator(
            a: GetResourcePoliciesRequest
        ): Kleisli[M, E, GetResourcePoliciesPublisher] = Kleisli(e => primitive1(f(e).getResourcePoliciesPaginator(a)))
        override def getServiceSetting(a: GetServiceSettingRequest): Kleisli[M, E, GetServiceSettingResponse] =
          Kleisli(e => eff1(f(e).getServiceSetting(a)))
        override def labelParameterVersion(
            a: LabelParameterVersionRequest
        ): Kleisli[M, E, LabelParameterVersionResponse] = Kleisli(e => eff1(f(e).labelParameterVersion(a)))
        override def listAssociationVersions(
            a: ListAssociationVersionsRequest
        ): Kleisli[M, E, ListAssociationVersionsResponse] = Kleisli(e => eff1(f(e).listAssociationVersions(a)))
        override def listAssociationVersionsPaginator(
            a: ListAssociationVersionsRequest
        ): Kleisli[M, E, ListAssociationVersionsPublisher] =
          Kleisli(e => primitive1(f(e).listAssociationVersionsPaginator(a)))
        override def listAssociations: Kleisli[M, E, ListAssociationsResponse] =
          Kleisli(e => eff1(f(e).listAssociations))
        override def listAssociations(a: ListAssociationsRequest): Kleisli[M, E, ListAssociationsResponse] =
          Kleisli(e => eff1(f(e).listAssociations(a)))
        override def listAssociationsPaginator: Kleisli[M, E, ListAssociationsPublisher] =
          Kleisli(e => primitive1(f(e).listAssociationsPaginator))
        override def listAssociationsPaginator(a: ListAssociationsRequest): Kleisli[M, E, ListAssociationsPublisher] =
          Kleisli(e => primitive1(f(e).listAssociationsPaginator(a)))
        override def listCloudConnectors(a: ListCloudConnectorsRequest): Kleisli[M, E, ListCloudConnectorsResponse] =
          Kleisli(e => eff1(f(e).listCloudConnectors(a)))
        override def listCloudConnectorsPaginator(
            a: ListCloudConnectorsRequest
        ): Kleisli[M, E, ListCloudConnectorsPublisher] = Kleisli(e => primitive1(f(e).listCloudConnectorsPaginator(a)))
        override def listCommandInvocations: Kleisli[M, E, ListCommandInvocationsResponse] =
          Kleisli(e => eff1(f(e).listCommandInvocations))
        override def listCommandInvocations(
            a: ListCommandInvocationsRequest
        ): Kleisli[M, E, ListCommandInvocationsResponse] = Kleisli(e => eff1(f(e).listCommandInvocations(a)))
        override def listCommandInvocationsPaginator: Kleisli[M, E, ListCommandInvocationsPublisher] =
          Kleisli(e => primitive1(f(e).listCommandInvocationsPaginator))
        override def listCommandInvocationsPaginator(
            a: ListCommandInvocationsRequest
        ): Kleisli[M, E, ListCommandInvocationsPublisher] =
          Kleisli(e => primitive1(f(e).listCommandInvocationsPaginator(a)))
        override def listCommands: Kleisli[M, E, ListCommandsResponse] = Kleisli(e => eff1(f(e).listCommands))
        override def listCommands(a: ListCommandsRequest): Kleisli[M, E, ListCommandsResponse] =
          Kleisli(e => eff1(f(e).listCommands(a)))
        override def listCommandsPaginator: Kleisli[M, E, ListCommandsPublisher] =
          Kleisli(e => primitive1(f(e).listCommandsPaginator))
        override def listCommandsPaginator(a: ListCommandsRequest): Kleisli[M, E, ListCommandsPublisher] =
          Kleisli(e => primitive1(f(e).listCommandsPaginator(a)))
        override def listComplianceItems(a: ListComplianceItemsRequest): Kleisli[M, E, ListComplianceItemsResponse] =
          Kleisli(e => eff1(f(e).listComplianceItems(a)))
        override def listComplianceItemsPaginator(
            a: ListComplianceItemsRequest
        ): Kleisli[M, E, ListComplianceItemsPublisher] = Kleisli(e => primitive1(f(e).listComplianceItemsPaginator(a)))
        override def listComplianceSummaries: Kleisli[M, E, ListComplianceSummariesResponse] =
          Kleisli(e => eff1(f(e).listComplianceSummaries))
        override def listComplianceSummaries(
            a: ListComplianceSummariesRequest
        ): Kleisli[M, E, ListComplianceSummariesResponse] = Kleisli(e => eff1(f(e).listComplianceSummaries(a)))
        override def listComplianceSummariesPaginator: Kleisli[M, E, ListComplianceSummariesPublisher] =
          Kleisli(e => primitive1(f(e).listComplianceSummariesPaginator))
        override def listComplianceSummariesPaginator(
            a: ListComplianceSummariesRequest
        ): Kleisli[M, E, ListComplianceSummariesPublisher] =
          Kleisli(e => primitive1(f(e).listComplianceSummariesPaginator(a)))
        override def listDocumentMetadataHistory(
            a: ListDocumentMetadataHistoryRequest
        ): Kleisli[M, E, ListDocumentMetadataHistoryResponse] = Kleisli(e => eff1(f(e).listDocumentMetadataHistory(a)))
        override def listDocumentVersions(a: ListDocumentVersionsRequest): Kleisli[M, E, ListDocumentVersionsResponse] =
          Kleisli(e => eff1(f(e).listDocumentVersions(a)))
        override def listDocumentVersionsPaginator(
            a: ListDocumentVersionsRequest
        ): Kleisli[M, E, ListDocumentVersionsPublisher] =
          Kleisli(e => primitive1(f(e).listDocumentVersionsPaginator(a)))
        override def listDocuments: Kleisli[M, E, ListDocumentsResponse] = Kleisli(e => eff1(f(e).listDocuments))
        override def listDocuments(a: ListDocumentsRequest): Kleisli[M, E, ListDocumentsResponse] =
          Kleisli(e => eff1(f(e).listDocuments(a)))
        override def listDocumentsPaginator: Kleisli[M, E, ListDocumentsPublisher] =
          Kleisli(e => primitive1(f(e).listDocumentsPaginator))
        override def listDocumentsPaginator(a: ListDocumentsRequest): Kleisli[M, E, ListDocumentsPublisher] =
          Kleisli(e => primitive1(f(e).listDocumentsPaginator(a)))
        override def listInventoryEntries(a: ListInventoryEntriesRequest): Kleisli[M, E, ListInventoryEntriesResponse] =
          Kleisli(e => eff1(f(e).listInventoryEntries(a)))
        override def listNodes(a: ListNodesRequest): Kleisli[M, E, ListNodesResponse] =
          Kleisli(e => eff1(f(e).listNodes(a)))
        override def listNodesPaginator(a: ListNodesRequest): Kleisli[M, E, ListNodesPublisher] =
          Kleisli(e => primitive1(f(e).listNodesPaginator(a)))
        override def listNodesSummary(a: ListNodesSummaryRequest): Kleisli[M, E, ListNodesSummaryResponse] =
          Kleisli(e => eff1(f(e).listNodesSummary(a)))
        override def listNodesSummaryPaginator(a: ListNodesSummaryRequest): Kleisli[M, E, ListNodesSummaryPublisher] =
          Kleisli(e => primitive1(f(e).listNodesSummaryPaginator(a)))
        override def listOpsItemEvents(a: ListOpsItemEventsRequest): Kleisli[M, E, ListOpsItemEventsResponse] =
          Kleisli(e => eff1(f(e).listOpsItemEvents(a)))
        override def listOpsItemEventsPaginator(
            a: ListOpsItemEventsRequest
        ): Kleisli[M, E, ListOpsItemEventsPublisher] = Kleisli(e => primitive1(f(e).listOpsItemEventsPaginator(a)))
        override def listOpsItemRelatedItems(
            a: ListOpsItemRelatedItemsRequest
        ): Kleisli[M, E, ListOpsItemRelatedItemsResponse] = Kleisli(e => eff1(f(e).listOpsItemRelatedItems(a)))
        override def listOpsItemRelatedItemsPaginator(
            a: ListOpsItemRelatedItemsRequest
        ): Kleisli[M, E, ListOpsItemRelatedItemsPublisher] =
          Kleisli(e => primitive1(f(e).listOpsItemRelatedItemsPaginator(a)))
        override def listOpsMetadata(a: ListOpsMetadataRequest): Kleisli[M, E, ListOpsMetadataResponse] =
          Kleisli(e => eff1(f(e).listOpsMetadata(a)))
        override def listOpsMetadataPaginator(a: ListOpsMetadataRequest): Kleisli[M, E, ListOpsMetadataPublisher] =
          Kleisli(e => primitive1(f(e).listOpsMetadataPaginator(a)))
        override def listResourceComplianceSummaries: Kleisli[M, E, ListResourceComplianceSummariesResponse] =
          Kleisli(e => eff1(f(e).listResourceComplianceSummaries))
        override def listResourceComplianceSummaries(
            a: ListResourceComplianceSummariesRequest
        ): Kleisli[M, E, ListResourceComplianceSummariesResponse] =
          Kleisli(e => eff1(f(e).listResourceComplianceSummaries(a)))
        override def listResourceComplianceSummariesPaginator: Kleisli[M, E, ListResourceComplianceSummariesPublisher] =
          Kleisli(e => primitive1(f(e).listResourceComplianceSummariesPaginator))
        override def listResourceComplianceSummariesPaginator(
            a: ListResourceComplianceSummariesRequest
        ): Kleisli[M, E, ListResourceComplianceSummariesPublisher] =
          Kleisli(e => primitive1(f(e).listResourceComplianceSummariesPaginator(a)))
        override def listResourceDataSync: Kleisli[M, E, ListResourceDataSyncResponse] =
          Kleisli(e => eff1(f(e).listResourceDataSync))
        override def listResourceDataSync(a: ListResourceDataSyncRequest): Kleisli[M, E, ListResourceDataSyncResponse] =
          Kleisli(e => eff1(f(e).listResourceDataSync(a)))
        override def listResourceDataSyncPaginator: Kleisli[M, E, ListResourceDataSyncPublisher] =
          Kleisli(e => primitive1(f(e).listResourceDataSyncPaginator))
        override def listResourceDataSyncPaginator(
            a: ListResourceDataSyncRequest
        ): Kleisli[M, E, ListResourceDataSyncPublisher] =
          Kleisli(e => primitive1(f(e).listResourceDataSyncPaginator(a)))
        override def listTagsForResource(a: ListTagsForResourceRequest): Kleisli[M, E, ListTagsForResourceResponse] =
          Kleisli(e => eff1(f(e).listTagsForResource(a)))
        override def modifyDocumentPermission(
            a: ModifyDocumentPermissionRequest
        ): Kleisli[M, E, ModifyDocumentPermissionResponse] = Kleisli(e => eff1(f(e).modifyDocumentPermission(a)))
        override def putComplianceItems(a: PutComplianceItemsRequest): Kleisli[M, E, PutComplianceItemsResponse] =
          Kleisli(e => eff1(f(e).putComplianceItems(a)))
        override def putInventory(a: PutInventoryRequest): Kleisli[M, E, PutInventoryResponse] =
          Kleisli(e => eff1(f(e).putInventory(a)))
        override def putParameter(a: PutParameterRequest): Kleisli[M, E, PutParameterResponse] =
          Kleisli(e => eff1(f(e).putParameter(a)))
        override def putResourcePolicy(a: PutResourcePolicyRequest): Kleisli[M, E, PutResourcePolicyResponse] =
          Kleisli(e => eff1(f(e).putResourcePolicy(a)))
        override def registerDefaultPatchBaseline(
            a: RegisterDefaultPatchBaselineRequest
        ): Kleisli[M, E, RegisterDefaultPatchBaselineResponse] =
          Kleisli(e => eff1(f(e).registerDefaultPatchBaseline(a)))
        override def registerPatchBaselineForPatchGroup(
            a: RegisterPatchBaselineForPatchGroupRequest
        ): Kleisli[M, E, RegisterPatchBaselineForPatchGroupResponse] =
          Kleisli(e => eff1(f(e).registerPatchBaselineForPatchGroup(a)))
        override def registerTargetWithMaintenanceWindow(
            a: RegisterTargetWithMaintenanceWindowRequest
        ): Kleisli[M, E, RegisterTargetWithMaintenanceWindowResponse] =
          Kleisli(e => eff1(f(e).registerTargetWithMaintenanceWindow(a)))
        override def registerTaskWithMaintenanceWindow(
            a: RegisterTaskWithMaintenanceWindowRequest
        ): Kleisli[M, E, RegisterTaskWithMaintenanceWindowResponse] =
          Kleisli(e => eff1(f(e).registerTaskWithMaintenanceWindow(a)))
        override def removeTagsFromResource(
            a: RemoveTagsFromResourceRequest
        ): Kleisli[M, E, RemoveTagsFromResourceResponse] = Kleisli(e => eff1(f(e).removeTagsFromResource(a)))
        override def resetServiceSetting(a: ResetServiceSettingRequest): Kleisli[M, E, ResetServiceSettingResponse] =
          Kleisli(e => eff1(f(e).resetServiceSetting(a)))
        override def resumeSession(a: ResumeSessionRequest): Kleisli[M, E, ResumeSessionResponse] =
          Kleisli(e => eff1(f(e).resumeSession(a)))
        override def sendAutomationSignal(a: SendAutomationSignalRequest): Kleisli[M, E, SendAutomationSignalResponse] =
          Kleisli(e => eff1(f(e).sendAutomationSignal(a)))
        override def sendCommand(a: SendCommandRequest): Kleisli[M, E, SendCommandResponse] =
          Kleisli(e => eff1(f(e).sendCommand(a)))
        override def serviceClientConfiguration: Kleisli[M, E, SsmServiceClientConfiguration] =
          Kleisli(e => primitive1(f(e).serviceClientConfiguration))
        override def serviceName: Kleisli[M, E, String] = Kleisli(e => primitive1(f(e).serviceName))
        override def startAccessRequest(a: StartAccessRequestRequest): Kleisli[M, E, StartAccessRequestResponse] =
          Kleisli(e => eff1(f(e).startAccessRequest(a)))
        override def startAssociationsOnce(
            a: StartAssociationsOnceRequest
        ): Kleisli[M, E, StartAssociationsOnceResponse] = Kleisli(e => eff1(f(e).startAssociationsOnce(a)))
        override def startAutomationExecution(
            a: StartAutomationExecutionRequest
        ): Kleisli[M, E, StartAutomationExecutionResponse] = Kleisli(e => eff1(f(e).startAutomationExecution(a)))
        override def startChangeRequestExecution(
            a: StartChangeRequestExecutionRequest
        ): Kleisli[M, E, StartChangeRequestExecutionResponse] = Kleisli(e => eff1(f(e).startChangeRequestExecution(a)))
        override def startExecutionPreview(
            a: StartExecutionPreviewRequest
        ): Kleisli[M, E, StartExecutionPreviewResponse] = Kleisli(e => eff1(f(e).startExecutionPreview(a)))
        override def startSession(a: StartSessionRequest): Kleisli[M, E, StartSessionResponse] =
          Kleisli(e => eff1(f(e).startSession(a)))
        override def stopAutomationExecution(
            a: StopAutomationExecutionRequest
        ): Kleisli[M, E, StopAutomationExecutionResponse] = Kleisli(e => eff1(f(e).stopAutomationExecution(a)))
        override def terminateSession(a: TerminateSessionRequest): Kleisli[M, E, TerminateSessionResponse] =
          Kleisli(e => eff1(f(e).terminateSession(a)))
        override def unlabelParameterVersion(
            a: UnlabelParameterVersionRequest
        ): Kleisli[M, E, UnlabelParameterVersionResponse] = Kleisli(e => eff1(f(e).unlabelParameterVersion(a)))
        override def updateAssociation(a: UpdateAssociationRequest): Kleisli[M, E, UpdateAssociationResponse] =
          Kleisli(e => eff1(f(e).updateAssociation(a)))
        override def updateAssociationStatus(
            a: UpdateAssociationStatusRequest
        ): Kleisli[M, E, UpdateAssociationStatusResponse] = Kleisli(e => eff1(f(e).updateAssociationStatus(a)))
        override def updateCloudConnector(a: UpdateCloudConnectorRequest): Kleisli[M, E, UpdateCloudConnectorResponse] =
          Kleisli(e => eff1(f(e).updateCloudConnector(a)))
        override def updateDocument(a: UpdateDocumentRequest): Kleisli[M, E, UpdateDocumentResponse] =
          Kleisli(e => eff1(f(e).updateDocument(a)))
        override def updateDocumentDefaultVersion(
            a: UpdateDocumentDefaultVersionRequest
        ): Kleisli[M, E, UpdateDocumentDefaultVersionResponse] =
          Kleisli(e => eff1(f(e).updateDocumentDefaultVersion(a)))
        override def updateDocumentMetadata(
            a: UpdateDocumentMetadataRequest
        ): Kleisli[M, E, UpdateDocumentMetadataResponse] = Kleisli(e => eff1(f(e).updateDocumentMetadata(a)))
        override def updateMaintenanceWindow(
            a: UpdateMaintenanceWindowRequest
        ): Kleisli[M, E, UpdateMaintenanceWindowResponse] = Kleisli(e => eff1(f(e).updateMaintenanceWindow(a)))
        override def updateMaintenanceWindowTarget(
            a: UpdateMaintenanceWindowTargetRequest
        ): Kleisli[M, E, UpdateMaintenanceWindowTargetResponse] =
          Kleisli(e => eff1(f(e).updateMaintenanceWindowTarget(a)))
        override def updateMaintenanceWindowTask(
            a: UpdateMaintenanceWindowTaskRequest
        ): Kleisli[M, E, UpdateMaintenanceWindowTaskResponse] = Kleisli(e => eff1(f(e).updateMaintenanceWindowTask(a)))
        override def updateManagedInstanceRole(
            a: UpdateManagedInstanceRoleRequest
        ): Kleisli[M, E, UpdateManagedInstanceRoleResponse] = Kleisli(e => eff1(f(e).updateManagedInstanceRole(a)))
        override def updateOpsItem(a: UpdateOpsItemRequest): Kleisli[M, E, UpdateOpsItemResponse] =
          Kleisli(e => eff1(f(e).updateOpsItem(a)))
        override def updateOpsMetadata(a: UpdateOpsMetadataRequest): Kleisli[M, E, UpdateOpsMetadataResponse] =
          Kleisli(e => eff1(f(e).updateOpsMetadata(a)))
        override def updatePatchBaseline(a: UpdatePatchBaselineRequest): Kleisli[M, E, UpdatePatchBaselineResponse] =
          Kleisli(e => eff1(f(e).updatePatchBaseline(a)))
        override def updateResourceDataSync(
            a: UpdateResourceDataSyncRequest
        ): Kleisli[M, E, UpdateResourceDataSyncResponse] = Kleisli(e => eff1(f(e).updateResourceDataSync(a)))
        override def updateServiceSetting(a: UpdateServiceSettingRequest): Kleisli[M, E, UpdateServiceSettingResponse] =
          Kleisli(e => eff1(f(e).updateServiceSetting(a)))
        override def validateCloudConnector(
            a: ValidateCloudConnectorRequest
        ): Kleisli[M, E, ValidateCloudConnectorResponse] = Kleisli(e => eff1(f(e).validateCloudConnector(a)))
        override def validateCloudConnectorPaginator(
            a: ValidateCloudConnectorRequest
        ): Kleisli[M, E, ValidateCloudConnectorPublisher] =
          Kleisli(e => primitive1(f(e).validateCloudConnectorPaginator(a)))
        override def waiter: Kleisli[M, E, SsmAsyncWaiter] = Kleisli(e => primitive1(f(e).waiter))
      }
  }
  // end interpreters

  /** Builds a `SsmAsyncClient` from the given builder, exposed as the [[SsmAsyncClientOp]] algebra in a managed `Resource`. */
  def resource(builder: SsmAsyncClientBuilder): Resource[M, SsmAsyncClientOp[M]] = clientResource(builder).map(create)

  /** Like `resource(builder)`, but using a default client builder. */
  def resource: Resource[M, SsmAsyncClientOp[M]] = resource(SsmAsyncClient.builder())

  /** Builds the underlying `SsmAsyncClient` from the given builder as a managed `Resource`. */
  def clientResource(builder: SsmAsyncClientBuilder): Resource[M, SsmAsyncClient] =
    Resource.fromAutoCloseable(asyncM.delay(builder.build()))

  /** Like `clientResource(builder)`, but using a default client builder. */
  def clientResource: Resource[M, SsmAsyncClient] = clientResource(SsmAsyncClient.builder())

  @deprecated("use clientResource(builder)", "7.0.0")
  def SsmAsyncClientResource(builder: SsmAsyncClientBuilder): Resource[M, SsmAsyncClient] = clientResource(builder)

  @deprecated("use resource(builder)", "7.0.0")
  def SsmAsyncClientOpResource(builder: SsmAsyncClientBuilder): Resource[M, SsmAsyncClientOp[M]] = resource(builder)

  def create(client: SsmAsyncClient): SsmAsyncClientOp[M] = new SsmAsyncClientOp[M] {

    // domain-specific operations are implemented in terms of `primitive`
    override def addTagsToResource(a: AddTagsToResourceRequest): M[AddTagsToResourceResponse] = eff1(
      client.addTagsToResource(a)
    )
    override def associateOpsItemRelatedItem(
        a: AssociateOpsItemRelatedItemRequest
    ): M[AssociateOpsItemRelatedItemResponse] = eff1(client.associateOpsItemRelatedItem(a))
    override def cancelCommand(a: CancelCommandRequest): M[CancelCommandResponse] = eff1(client.cancelCommand(a))
    override def cancelMaintenanceWindowExecution(
        a: CancelMaintenanceWindowExecutionRequest
    ): M[CancelMaintenanceWindowExecutionResponse] = eff1(client.cancelMaintenanceWindowExecution(a))
    override def close: M[Unit]                    = primitive1(client.close)
    override def createActivation(a: CreateActivationRequest): M[CreateActivationResponse] = eff1(
      client.createActivation(a)
    )
    override def createAssociation(a: CreateAssociationRequest): M[CreateAssociationResponse] = eff1(
      client.createAssociation(a)
    )
    override def createAssociationBatch(a: CreateAssociationBatchRequest): M[CreateAssociationBatchResponse] = eff1(
      client.createAssociationBatch(a)
    )
    override def createCloudConnector(a: CreateCloudConnectorRequest): M[CreateCloudConnectorResponse] = eff1(
      client.createCloudConnector(a)
    )
    override def createDocument(a: CreateDocumentRequest): M[CreateDocumentResponse] = eff1(client.createDocument(a))
    override def createMaintenanceWindow(a: CreateMaintenanceWindowRequest): M[CreateMaintenanceWindowResponse] = eff1(
      client.createMaintenanceWindow(a)
    )
    override def createOpsItem(a: CreateOpsItemRequest): M[CreateOpsItemResponse] = eff1(client.createOpsItem(a))
    override def createOpsMetadata(a: CreateOpsMetadataRequest): M[CreateOpsMetadataResponse] = eff1(
      client.createOpsMetadata(a)
    )
    override def createPatchBaseline(a: CreatePatchBaselineRequest): M[CreatePatchBaselineResponse] = eff1(
      client.createPatchBaseline(a)
    )
    override def createResourceDataSync(a: CreateResourceDataSyncRequest): M[CreateResourceDataSyncResponse] = eff1(
      client.createResourceDataSync(a)
    )
    override def deleteActivation(a: DeleteActivationRequest): M[DeleteActivationResponse] = eff1(
      client.deleteActivation(a)
    )
    override def deleteAssociation(a: DeleteAssociationRequest): M[DeleteAssociationResponse] = eff1(
      client.deleteAssociation(a)
    )
    override def deleteCloudConnector(a: DeleteCloudConnectorRequest): M[DeleteCloudConnectorResponse] = eff1(
      client.deleteCloudConnector(a)
    )
    override def deleteDocument(a: DeleteDocumentRequest): M[DeleteDocumentResponse]    = eff1(client.deleteDocument(a))
    override def deleteInventory(a: DeleteInventoryRequest): M[DeleteInventoryResponse] = eff1(
      client.deleteInventory(a)
    )
    override def deleteMaintenanceWindow(a: DeleteMaintenanceWindowRequest): M[DeleteMaintenanceWindowResponse] = eff1(
      client.deleteMaintenanceWindow(a)
    )
    override def deleteOpsItem(a: DeleteOpsItemRequest): M[DeleteOpsItemResponse] = eff1(client.deleteOpsItem(a))
    override def deleteOpsMetadata(a: DeleteOpsMetadataRequest): M[DeleteOpsMetadataResponse] = eff1(
      client.deleteOpsMetadata(a)
    )
    override def deleteParameter(a: DeleteParameterRequest): M[DeleteParameterResponse] = eff1(
      client.deleteParameter(a)
    )
    override def deleteParameters(a: DeleteParametersRequest): M[DeleteParametersResponse] = eff1(
      client.deleteParameters(a)
    )
    override def deletePatchBaseline(a: DeletePatchBaselineRequest): M[DeletePatchBaselineResponse] = eff1(
      client.deletePatchBaseline(a)
    )
    override def deleteResourceDataSync(a: DeleteResourceDataSyncRequest): M[DeleteResourceDataSyncResponse] = eff1(
      client.deleteResourceDataSync(a)
    )
    override def deleteResourcePolicy(a: DeleteResourcePolicyRequest): M[DeleteResourcePolicyResponse] = eff1(
      client.deleteResourcePolicy(a)
    )
    override def deregisterManagedInstance(a: DeregisterManagedInstanceRequest): M[DeregisterManagedInstanceResponse] =
      eff1(client.deregisterManagedInstance(a))
    override def deregisterPatchBaselineForPatchGroup(
        a: DeregisterPatchBaselineForPatchGroupRequest
    ): M[DeregisterPatchBaselineForPatchGroupResponse] = eff1(client.deregisterPatchBaselineForPatchGroup(a))
    override def deregisterTargetFromMaintenanceWindow(
        a: DeregisterTargetFromMaintenanceWindowRequest
    ): M[DeregisterTargetFromMaintenanceWindowResponse] = eff1(client.deregisterTargetFromMaintenanceWindow(a))
    override def deregisterTaskFromMaintenanceWindow(
        a: DeregisterTaskFromMaintenanceWindowRequest
    ): M[DeregisterTaskFromMaintenanceWindowResponse] = eff1(client.deregisterTaskFromMaintenanceWindow(a))
    override def describeActivations: M[DescribeActivationsResponse] = eff1(client.describeActivations)
    override def describeActivations(a: DescribeActivationsRequest): M[DescribeActivationsResponse] = eff1(
      client.describeActivations(a)
    )
    override def describeActivationsPaginator: M[DescribeActivationsPublisher] = primitive1(
      client.describeActivationsPaginator
    )
    override def describeActivationsPaginator(a: DescribeActivationsRequest): M[DescribeActivationsPublisher] =
      primitive1(client.describeActivationsPaginator(a))
    override def describeAssociation(a: DescribeAssociationRequest): M[DescribeAssociationResponse] = eff1(
      client.describeAssociation(a)
    )
    override def describeAssociationExecutionTargets(
        a: DescribeAssociationExecutionTargetsRequest
    ): M[DescribeAssociationExecutionTargetsResponse] = eff1(client.describeAssociationExecutionTargets(a))
    override def describeAssociationExecutionTargetsPaginator(
        a: DescribeAssociationExecutionTargetsRequest
    ): M[DescribeAssociationExecutionTargetsPublisher] = primitive1(
      client.describeAssociationExecutionTargetsPaginator(a)
    )
    override def describeAssociationExecutions(
        a: DescribeAssociationExecutionsRequest
    ): M[DescribeAssociationExecutionsResponse] = eff1(client.describeAssociationExecutions(a))
    override def describeAssociationExecutionsPaginator(
        a: DescribeAssociationExecutionsRequest
    ): M[DescribeAssociationExecutionsPublisher] = primitive1(client.describeAssociationExecutionsPaginator(a))
    override def describeAutomationExecutions: M[DescribeAutomationExecutionsResponse] = eff1(
      client.describeAutomationExecutions
    )
    override def describeAutomationExecutions(
        a: DescribeAutomationExecutionsRequest
    ): M[DescribeAutomationExecutionsResponse] = eff1(client.describeAutomationExecutions(a))
    override def describeAutomationExecutionsPaginator: M[DescribeAutomationExecutionsPublisher] = primitive1(
      client.describeAutomationExecutionsPaginator
    )
    override def describeAutomationExecutionsPaginator(
        a: DescribeAutomationExecutionsRequest
    ): M[DescribeAutomationExecutionsPublisher] = primitive1(client.describeAutomationExecutionsPaginator(a))
    override def describeAutomationStepExecutions(
        a: DescribeAutomationStepExecutionsRequest
    ): M[DescribeAutomationStepExecutionsResponse] = eff1(client.describeAutomationStepExecutions(a))
    override def describeAutomationStepExecutionsPaginator(
        a: DescribeAutomationStepExecutionsRequest
    ): M[DescribeAutomationStepExecutionsPublisher] = primitive1(client.describeAutomationStepExecutionsPaginator(a))
    override def describeAvailablePatches: M[DescribeAvailablePatchesResponse] = eff1(client.describeAvailablePatches)
    override def describeAvailablePatches(a: DescribeAvailablePatchesRequest): M[DescribeAvailablePatchesResponse] =
      eff1(client.describeAvailablePatches(a))
    override def describeAvailablePatchesPaginator: M[DescribeAvailablePatchesPublisher] = primitive1(
      client.describeAvailablePatchesPaginator
    )
    override def describeAvailablePatchesPaginator(
        a: DescribeAvailablePatchesRequest
    ): M[DescribeAvailablePatchesPublisher] = primitive1(client.describeAvailablePatchesPaginator(a))
    override def describeDocument(a: DescribeDocumentRequest): M[DescribeDocumentResponse] = eff1(
      client.describeDocument(a)
    )
    override def describeDocumentPermission(
        a: DescribeDocumentPermissionRequest
    ): M[DescribeDocumentPermissionResponse] = eff1(client.describeDocumentPermission(a))
    override def describeEffectiveInstanceAssociations(
        a: DescribeEffectiveInstanceAssociationsRequest
    ): M[DescribeEffectiveInstanceAssociationsResponse] = eff1(client.describeEffectiveInstanceAssociations(a))
    override def describeEffectiveInstanceAssociationsPaginator(
        a: DescribeEffectiveInstanceAssociationsRequest
    ): M[DescribeEffectiveInstanceAssociationsPublisher] = primitive1(
      client.describeEffectiveInstanceAssociationsPaginator(a)
    )
    override def describeEffectivePatchesForPatchBaseline(
        a: DescribeEffectivePatchesForPatchBaselineRequest
    ): M[DescribeEffectivePatchesForPatchBaselineResponse] = eff1(client.describeEffectivePatchesForPatchBaseline(a))
    override def describeEffectivePatchesForPatchBaselinePaginator(
        a: DescribeEffectivePatchesForPatchBaselineRequest
    ): M[DescribeEffectivePatchesForPatchBaselinePublisher] = primitive1(
      client.describeEffectivePatchesForPatchBaselinePaginator(a)
    )
    override def describeInstanceAssociationsStatus(
        a: DescribeInstanceAssociationsStatusRequest
    ): M[DescribeInstanceAssociationsStatusResponse] = eff1(client.describeInstanceAssociationsStatus(a))
    override def describeInstanceAssociationsStatusPaginator(
        a: DescribeInstanceAssociationsStatusRequest
    ): M[DescribeInstanceAssociationsStatusPublisher] = primitive1(
      client.describeInstanceAssociationsStatusPaginator(a)
    )
    override def describeInstanceInformation: M[DescribeInstanceInformationResponse] = eff1(
      client.describeInstanceInformation
    )
    override def describeInstanceInformation(
        a: DescribeInstanceInformationRequest
    ): M[DescribeInstanceInformationResponse] = eff1(client.describeInstanceInformation(a))
    override def describeInstanceInformationPaginator: M[DescribeInstanceInformationPublisher] = primitive1(
      client.describeInstanceInformationPaginator
    )
    override def describeInstanceInformationPaginator(
        a: DescribeInstanceInformationRequest
    ): M[DescribeInstanceInformationPublisher] = primitive1(client.describeInstanceInformationPaginator(a))
    override def describeInstancePatchStates(
        a: DescribeInstancePatchStatesRequest
    ): M[DescribeInstancePatchStatesResponse] = eff1(client.describeInstancePatchStates(a))
    override def describeInstancePatchStatesForPatchGroup(
        a: DescribeInstancePatchStatesForPatchGroupRequest
    ): M[DescribeInstancePatchStatesForPatchGroupResponse] = eff1(client.describeInstancePatchStatesForPatchGroup(a))
    override def describeInstancePatchStatesForPatchGroupPaginator(
        a: DescribeInstancePatchStatesForPatchGroupRequest
    ): M[DescribeInstancePatchStatesForPatchGroupPublisher] = primitive1(
      client.describeInstancePatchStatesForPatchGroupPaginator(a)
    )
    override def describeInstancePatchStatesPaginator(
        a: DescribeInstancePatchStatesRequest
    ): M[DescribeInstancePatchStatesPublisher] = primitive1(client.describeInstancePatchStatesPaginator(a))
    override def describeInstancePatches(a: DescribeInstancePatchesRequest): M[DescribeInstancePatchesResponse] = eff1(
      client.describeInstancePatches(a)
    )
    override def describeInstancePatchesPaginator(
        a: DescribeInstancePatchesRequest
    ): M[DescribeInstancePatchesPublisher] = primitive1(client.describeInstancePatchesPaginator(a))
    override def describeInstanceProperties(
        a: DescribeInstancePropertiesRequest
    ): M[DescribeInstancePropertiesResponse] = eff1(client.describeInstanceProperties(a))
    override def describeInstancePropertiesPaginator(
        a: DescribeInstancePropertiesRequest
    ): M[DescribeInstancePropertiesPublisher] = primitive1(client.describeInstancePropertiesPaginator(a))
    override def describeInventoryDeletions: M[DescribeInventoryDeletionsResponse] = eff1(
      client.describeInventoryDeletions
    )
    override def describeInventoryDeletions(
        a: DescribeInventoryDeletionsRequest
    ): M[DescribeInventoryDeletionsResponse] = eff1(client.describeInventoryDeletions(a))
    override def describeInventoryDeletionsPaginator: M[DescribeInventoryDeletionsPublisher] = primitive1(
      client.describeInventoryDeletionsPaginator
    )
    override def describeInventoryDeletionsPaginator(
        a: DescribeInventoryDeletionsRequest
    ): M[DescribeInventoryDeletionsPublisher] = primitive1(client.describeInventoryDeletionsPaginator(a))
    override def describeMaintenanceWindowExecutionTaskInvocations(
        a: DescribeMaintenanceWindowExecutionTaskInvocationsRequest
    ): M[DescribeMaintenanceWindowExecutionTaskInvocationsResponse] = eff1(
      client.describeMaintenanceWindowExecutionTaskInvocations(a)
    )
    override def describeMaintenanceWindowExecutionTaskInvocationsPaginator(
        a: DescribeMaintenanceWindowExecutionTaskInvocationsRequest
    ): M[DescribeMaintenanceWindowExecutionTaskInvocationsPublisher] = primitive1(
      client.describeMaintenanceWindowExecutionTaskInvocationsPaginator(a)
    )
    override def describeMaintenanceWindowExecutionTasks(
        a: DescribeMaintenanceWindowExecutionTasksRequest
    ): M[DescribeMaintenanceWindowExecutionTasksResponse] = eff1(client.describeMaintenanceWindowExecutionTasks(a))
    override def describeMaintenanceWindowExecutionTasksPaginator(
        a: DescribeMaintenanceWindowExecutionTasksRequest
    ): M[DescribeMaintenanceWindowExecutionTasksPublisher] = primitive1(
      client.describeMaintenanceWindowExecutionTasksPaginator(a)
    )
    override def describeMaintenanceWindowExecutions(
        a: DescribeMaintenanceWindowExecutionsRequest
    ): M[DescribeMaintenanceWindowExecutionsResponse] = eff1(client.describeMaintenanceWindowExecutions(a))
    override def describeMaintenanceWindowExecutionsPaginator(
        a: DescribeMaintenanceWindowExecutionsRequest
    ): M[DescribeMaintenanceWindowExecutionsPublisher] = primitive1(
      client.describeMaintenanceWindowExecutionsPaginator(a)
    )
    override def describeMaintenanceWindowSchedule(
        a: DescribeMaintenanceWindowScheduleRequest
    ): M[DescribeMaintenanceWindowScheduleResponse] = eff1(client.describeMaintenanceWindowSchedule(a))
    override def describeMaintenanceWindowSchedulePaginator(
        a: DescribeMaintenanceWindowScheduleRequest
    ): M[DescribeMaintenanceWindowSchedulePublisher] = primitive1(client.describeMaintenanceWindowSchedulePaginator(a))
    override def describeMaintenanceWindowTargets(
        a: DescribeMaintenanceWindowTargetsRequest
    ): M[DescribeMaintenanceWindowTargetsResponse] = eff1(client.describeMaintenanceWindowTargets(a))
    override def describeMaintenanceWindowTargetsPaginator(
        a: DescribeMaintenanceWindowTargetsRequest
    ): M[DescribeMaintenanceWindowTargetsPublisher] = primitive1(client.describeMaintenanceWindowTargetsPaginator(a))
    override def describeMaintenanceWindowTasks(
        a: DescribeMaintenanceWindowTasksRequest
    ): M[DescribeMaintenanceWindowTasksResponse] = eff1(client.describeMaintenanceWindowTasks(a))
    override def describeMaintenanceWindowTasksPaginator(
        a: DescribeMaintenanceWindowTasksRequest
    ): M[DescribeMaintenanceWindowTasksPublisher] = primitive1(client.describeMaintenanceWindowTasksPaginator(a))
    override def describeMaintenanceWindows: M[DescribeMaintenanceWindowsResponse] = eff1(
      client.describeMaintenanceWindows
    )
    override def describeMaintenanceWindows(
        a: DescribeMaintenanceWindowsRequest
    ): M[DescribeMaintenanceWindowsResponse] = eff1(client.describeMaintenanceWindows(a))
    override def describeMaintenanceWindowsForTarget(
        a: DescribeMaintenanceWindowsForTargetRequest
    ): M[DescribeMaintenanceWindowsForTargetResponse] = eff1(client.describeMaintenanceWindowsForTarget(a))
    override def describeMaintenanceWindowsForTargetPaginator(
        a: DescribeMaintenanceWindowsForTargetRequest
    ): M[DescribeMaintenanceWindowsForTargetPublisher] = primitive1(
      client.describeMaintenanceWindowsForTargetPaginator(a)
    )
    override def describeMaintenanceWindowsPaginator: M[DescribeMaintenanceWindowsPublisher] = primitive1(
      client.describeMaintenanceWindowsPaginator
    )
    override def describeMaintenanceWindowsPaginator(
        a: DescribeMaintenanceWindowsRequest
    ): M[DescribeMaintenanceWindowsPublisher] = primitive1(client.describeMaintenanceWindowsPaginator(a))
    override def describeOpsItems(a: DescribeOpsItemsRequest): M[DescribeOpsItemsResponse] = eff1(
      client.describeOpsItems(a)
    )
    override def describeOpsItemsPaginator(a: DescribeOpsItemsRequest): M[DescribeOpsItemsPublisher] = primitive1(
      client.describeOpsItemsPaginator(a)
    )
    override def describeParameters: M[DescribeParametersResponse] = eff1(client.describeParameters)
    override def describeParameters(a: DescribeParametersRequest): M[DescribeParametersResponse] = eff1(
      client.describeParameters(a)
    )
    override def describeParametersPaginator: M[DescribeParametersPublisher] = primitive1(
      client.describeParametersPaginator
    )
    override def describeParametersPaginator(a: DescribeParametersRequest): M[DescribeParametersPublisher] = primitive1(
      client.describeParametersPaginator(a)
    )
    override def describePatchBaselines: M[DescribePatchBaselinesResponse] = eff1(client.describePatchBaselines)
    override def describePatchBaselines(a: DescribePatchBaselinesRequest): M[DescribePatchBaselinesResponse] = eff1(
      client.describePatchBaselines(a)
    )
    override def describePatchBaselinesPaginator: M[DescribePatchBaselinesPublisher] = primitive1(
      client.describePatchBaselinesPaginator
    )
    override def describePatchBaselinesPaginator(a: DescribePatchBaselinesRequest): M[DescribePatchBaselinesPublisher] =
      primitive1(client.describePatchBaselinesPaginator(a))
    override def describePatchGroupState(a: DescribePatchGroupStateRequest): M[DescribePatchGroupStateResponse] = eff1(
      client.describePatchGroupState(a)
    )
    override def describePatchGroups: M[DescribePatchGroupsResponse] = eff1(client.describePatchGroups)
    override def describePatchGroups(a: DescribePatchGroupsRequest): M[DescribePatchGroupsResponse] = eff1(
      client.describePatchGroups(a)
    )
    override def describePatchGroupsPaginator: M[DescribePatchGroupsPublisher] = primitive1(
      client.describePatchGroupsPaginator
    )
    override def describePatchGroupsPaginator(a: DescribePatchGroupsRequest): M[DescribePatchGroupsPublisher] =
      primitive1(client.describePatchGroupsPaginator(a))
    override def describePatchProperties(a: DescribePatchPropertiesRequest): M[DescribePatchPropertiesResponse] = eff1(
      client.describePatchProperties(a)
    )
    override def describePatchPropertiesPaginator(
        a: DescribePatchPropertiesRequest
    ): M[DescribePatchPropertiesPublisher] = primitive1(client.describePatchPropertiesPaginator(a))
    override def describeSessions(a: DescribeSessionsRequest): M[DescribeSessionsResponse] = eff1(
      client.describeSessions(a)
    )
    override def describeSessionsPaginator(a: DescribeSessionsRequest): M[DescribeSessionsPublisher] = primitive1(
      client.describeSessionsPaginator(a)
    )
    override def disassociateOpsItemRelatedItem(
        a: DisassociateOpsItemRelatedItemRequest
    ): M[DisassociateOpsItemRelatedItemResponse] = eff1(client.disassociateOpsItemRelatedItem(a))
    override def getAccessToken(a: GetAccessTokenRequest): M[GetAccessTokenResponse] = eff1(client.getAccessToken(a))
    override def getAutomationExecution(a: GetAutomationExecutionRequest): M[GetAutomationExecutionResponse] = eff1(
      client.getAutomationExecution(a)
    )
    override def getCalendarState(a: GetCalendarStateRequest): M[GetCalendarStateResponse] = eff1(
      client.getCalendarState(a)
    )
    override def getCloudConnector(a: GetCloudConnectorRequest): M[GetCloudConnectorResponse] = eff1(
      client.getCloudConnector(a)
    )
    override def getCommandInvocation(a: GetCommandInvocationRequest): M[GetCommandInvocationResponse] = eff1(
      client.getCommandInvocation(a)
    )
    override def getConnectionStatus(a: GetConnectionStatusRequest): M[GetConnectionStatusResponse] = eff1(
      client.getConnectionStatus(a)
    )
    override def getDefaultPatchBaseline: M[GetDefaultPatchBaselineResponse] = eff1(client.getDefaultPatchBaseline)
    override def getDefaultPatchBaseline(a: GetDefaultPatchBaselineRequest): M[GetDefaultPatchBaselineResponse] = eff1(
      client.getDefaultPatchBaseline(a)
    )
    override def getDeployablePatchSnapshotForInstance(
        a: GetDeployablePatchSnapshotForInstanceRequest
    ): M[GetDeployablePatchSnapshotForInstanceResponse] = eff1(client.getDeployablePatchSnapshotForInstance(a))
    override def getDocument(a: GetDocumentRequest): M[GetDocumentResponse] = eff1(client.getDocument(a))
    override def getExecutionPreview(a: GetExecutionPreviewRequest): M[GetExecutionPreviewResponse] = eff1(
      client.getExecutionPreview(a)
    )
    override def getInventory: M[GetInventoryResponse]                         = eff1(client.getInventory)
    override def getInventory(a: GetInventoryRequest): M[GetInventoryResponse] = eff1(client.getInventory(a))
    override def getInventoryPaginator: M[GetInventoryPublisher] = primitive1(client.getInventoryPaginator)
    override def getInventoryPaginator(a: GetInventoryRequest): M[GetInventoryPublisher] = primitive1(
      client.getInventoryPaginator(a)
    )
    override def getInventorySchema: M[GetInventorySchemaResponse] = eff1(client.getInventorySchema)
    override def getInventorySchema(a: GetInventorySchemaRequest): M[GetInventorySchemaResponse] = eff1(
      client.getInventorySchema(a)
    )
    override def getInventorySchemaPaginator: M[GetInventorySchemaPublisher] = primitive1(
      client.getInventorySchemaPaginator
    )
    override def getInventorySchemaPaginator(a: GetInventorySchemaRequest): M[GetInventorySchemaPublisher] = primitive1(
      client.getInventorySchemaPaginator(a)
    )
    override def getMaintenanceWindow(a: GetMaintenanceWindowRequest): M[GetMaintenanceWindowResponse] = eff1(
      client.getMaintenanceWindow(a)
    )
    override def getMaintenanceWindowExecution(
        a: GetMaintenanceWindowExecutionRequest
    ): M[GetMaintenanceWindowExecutionResponse] = eff1(client.getMaintenanceWindowExecution(a))
    override def getMaintenanceWindowExecutionTask(
        a: GetMaintenanceWindowExecutionTaskRequest
    ): M[GetMaintenanceWindowExecutionTaskResponse] = eff1(client.getMaintenanceWindowExecutionTask(a))
    override def getMaintenanceWindowExecutionTaskInvocation(
        a: GetMaintenanceWindowExecutionTaskInvocationRequest
    ): M[GetMaintenanceWindowExecutionTaskInvocationResponse] = eff1(
      client.getMaintenanceWindowExecutionTaskInvocation(a)
    )
    override def getMaintenanceWindowTask(a: GetMaintenanceWindowTaskRequest): M[GetMaintenanceWindowTaskResponse] =
      eff1(client.getMaintenanceWindowTask(a))
    override def getOpsItem(a: GetOpsItemRequest): M[GetOpsItemResponse]             = eff1(client.getOpsItem(a))
    override def getOpsMetadata(a: GetOpsMetadataRequest): M[GetOpsMetadataResponse] = eff1(client.getOpsMetadata(a))
    override def getOpsSummary(a: GetOpsSummaryRequest): M[GetOpsSummaryResponse]    = eff1(client.getOpsSummary(a))
    override def getOpsSummaryPaginator(a: GetOpsSummaryRequest): M[GetOpsSummaryPublisher] = primitive1(
      client.getOpsSummaryPaginator(a)
    )
    override def getParameter(a: GetParameterRequest): M[GetParameterResponse] = eff1(client.getParameter(a))
    override def getParameterHistory(a: GetParameterHistoryRequest): M[GetParameterHistoryResponse] = eff1(
      client.getParameterHistory(a)
    )
    override def getParameterHistoryPaginator(a: GetParameterHistoryRequest): M[GetParameterHistoryPublisher] =
      primitive1(client.getParameterHistoryPaginator(a))
    override def getParameters(a: GetParametersRequest): M[GetParametersResponse] = eff1(client.getParameters(a))
    override def getParametersByPath(a: GetParametersByPathRequest): M[GetParametersByPathResponse] = eff1(
      client.getParametersByPath(a)
    )
    override def getParametersByPathPaginator(a: GetParametersByPathRequest): M[GetParametersByPathPublisher] =
      primitive1(client.getParametersByPathPaginator(a))
    override def getPatchBaseline(a: GetPatchBaselineRequest): M[GetPatchBaselineResponse] = eff1(
      client.getPatchBaseline(a)
    )
    override def getPatchBaselineForPatchGroup(
        a: GetPatchBaselineForPatchGroupRequest
    ): M[GetPatchBaselineForPatchGroupResponse] = eff1(client.getPatchBaselineForPatchGroup(a))
    override def getResourcePolicies(a: GetResourcePoliciesRequest): M[GetResourcePoliciesResponse] = eff1(
      client.getResourcePolicies(a)
    )
    override def getResourcePoliciesPaginator(a: GetResourcePoliciesRequest): M[GetResourcePoliciesPublisher] =
      primitive1(client.getResourcePoliciesPaginator(a))
    override def getServiceSetting(a: GetServiceSettingRequest): M[GetServiceSettingResponse] = eff1(
      client.getServiceSetting(a)
    )
    override def labelParameterVersion(a: LabelParameterVersionRequest): M[LabelParameterVersionResponse] = eff1(
      client.labelParameterVersion(a)
    )
    override def listAssociationVersions(a: ListAssociationVersionsRequest): M[ListAssociationVersionsResponse] = eff1(
      client.listAssociationVersions(a)
    )
    override def listAssociationVersionsPaginator(
        a: ListAssociationVersionsRequest
    ): M[ListAssociationVersionsPublisher]                     = primitive1(client.listAssociationVersionsPaginator(a))
    override def listAssociations: M[ListAssociationsResponse] = eff1(client.listAssociations)
    override def listAssociations(a: ListAssociationsRequest): M[ListAssociationsResponse] = eff1(
      client.listAssociations(a)
    )
    override def listAssociationsPaginator: M[ListAssociationsPublisher] = primitive1(client.listAssociationsPaginator)
    override def listAssociationsPaginator(a: ListAssociationsRequest): M[ListAssociationsPublisher] = primitive1(
      client.listAssociationsPaginator(a)
    )
    override def listCloudConnectors(a: ListCloudConnectorsRequest): M[ListCloudConnectorsResponse] = eff1(
      client.listCloudConnectors(a)
    )
    override def listCloudConnectorsPaginator(a: ListCloudConnectorsRequest): M[ListCloudConnectorsPublisher] =
      primitive1(client.listCloudConnectorsPaginator(a))
    override def listCommandInvocations: M[ListCommandInvocationsResponse] = eff1(client.listCommandInvocations)
    override def listCommandInvocations(a: ListCommandInvocationsRequest): M[ListCommandInvocationsResponse] = eff1(
      client.listCommandInvocations(a)
    )
    override def listCommandInvocationsPaginator: M[ListCommandInvocationsPublisher] = primitive1(
      client.listCommandInvocationsPaginator
    )
    override def listCommandInvocationsPaginator(a: ListCommandInvocationsRequest): M[ListCommandInvocationsPublisher] =
      primitive1(client.listCommandInvocationsPaginator(a))
    override def listCommands: M[ListCommandsResponse]                         = eff1(client.listCommands)
    override def listCommands(a: ListCommandsRequest): M[ListCommandsResponse] = eff1(client.listCommands(a))
    override def listCommandsPaginator: M[ListCommandsPublisher] = primitive1(client.listCommandsPaginator)
    override def listCommandsPaginator(a: ListCommandsRequest): M[ListCommandsPublisher] = primitive1(
      client.listCommandsPaginator(a)
    )
    override def listComplianceItems(a: ListComplianceItemsRequest): M[ListComplianceItemsResponse] = eff1(
      client.listComplianceItems(a)
    )
    override def listComplianceItemsPaginator(a: ListComplianceItemsRequest): M[ListComplianceItemsPublisher] =
      primitive1(client.listComplianceItemsPaginator(a))
    override def listComplianceSummaries: M[ListComplianceSummariesResponse] = eff1(client.listComplianceSummaries)
    override def listComplianceSummaries(a: ListComplianceSummariesRequest): M[ListComplianceSummariesResponse] = eff1(
      client.listComplianceSummaries(a)
    )
    override def listComplianceSummariesPaginator: M[ListComplianceSummariesPublisher] = primitive1(
      client.listComplianceSummariesPaginator
    )
    override def listComplianceSummariesPaginator(
        a: ListComplianceSummariesRequest
    ): M[ListComplianceSummariesPublisher] = primitive1(client.listComplianceSummariesPaginator(a))
    override def listDocumentMetadataHistory(
        a: ListDocumentMetadataHistoryRequest
    ): M[ListDocumentMetadataHistoryResponse] = eff1(client.listDocumentMetadataHistory(a))
    override def listDocumentVersions(a: ListDocumentVersionsRequest): M[ListDocumentVersionsResponse] = eff1(
      client.listDocumentVersions(a)
    )
    override def listDocumentVersionsPaginator(a: ListDocumentVersionsRequest): M[ListDocumentVersionsPublisher] =
      primitive1(client.listDocumentVersionsPaginator(a))
    override def listDocuments: M[ListDocumentsResponse]                          = eff1(client.listDocuments)
    override def listDocuments(a: ListDocumentsRequest): M[ListDocumentsResponse] = eff1(client.listDocuments(a))
    override def listDocumentsPaginator: M[ListDocumentsPublisher] = primitive1(client.listDocumentsPaginator)
    override def listDocumentsPaginator(a: ListDocumentsRequest): M[ListDocumentsPublisher] = primitive1(
      client.listDocumentsPaginator(a)
    )
    override def listInventoryEntries(a: ListInventoryEntriesRequest): M[ListInventoryEntriesResponse] = eff1(
      client.listInventoryEntries(a)
    )
    override def listNodes(a: ListNodesRequest): M[ListNodesResponse]           = eff1(client.listNodes(a))
    override def listNodesPaginator(a: ListNodesRequest): M[ListNodesPublisher] = primitive1(
      client.listNodesPaginator(a)
    )
    override def listNodesSummary(a: ListNodesSummaryRequest): M[ListNodesSummaryResponse] = eff1(
      client.listNodesSummary(a)
    )
    override def listNodesSummaryPaginator(a: ListNodesSummaryRequest): M[ListNodesSummaryPublisher] = primitive1(
      client.listNodesSummaryPaginator(a)
    )
    override def listOpsItemEvents(a: ListOpsItemEventsRequest): M[ListOpsItemEventsResponse] = eff1(
      client.listOpsItemEvents(a)
    )
    override def listOpsItemEventsPaginator(a: ListOpsItemEventsRequest): M[ListOpsItemEventsPublisher] = primitive1(
      client.listOpsItemEventsPaginator(a)
    )
    override def listOpsItemRelatedItems(a: ListOpsItemRelatedItemsRequest): M[ListOpsItemRelatedItemsResponse] = eff1(
      client.listOpsItemRelatedItems(a)
    )
    override def listOpsItemRelatedItemsPaginator(
        a: ListOpsItemRelatedItemsRequest
    ): M[ListOpsItemRelatedItemsPublisher] = primitive1(client.listOpsItemRelatedItemsPaginator(a))
    override def listOpsMetadata(a: ListOpsMetadataRequest): M[ListOpsMetadataResponse] = eff1(
      client.listOpsMetadata(a)
    )
    override def listOpsMetadataPaginator(a: ListOpsMetadataRequest): M[ListOpsMetadataPublisher] = primitive1(
      client.listOpsMetadataPaginator(a)
    )
    override def listResourceComplianceSummaries: M[ListResourceComplianceSummariesResponse] = eff1(
      client.listResourceComplianceSummaries
    )
    override def listResourceComplianceSummaries(
        a: ListResourceComplianceSummariesRequest
    ): M[ListResourceComplianceSummariesResponse] = eff1(client.listResourceComplianceSummaries(a))
    override def listResourceComplianceSummariesPaginator: M[ListResourceComplianceSummariesPublisher] = primitive1(
      client.listResourceComplianceSummariesPaginator
    )
    override def listResourceComplianceSummariesPaginator(
        a: ListResourceComplianceSummariesRequest
    ): M[ListResourceComplianceSummariesPublisher] = primitive1(client.listResourceComplianceSummariesPaginator(a))
    override def listResourceDataSync: M[ListResourceDataSyncResponse] = eff1(client.listResourceDataSync)
    override def listResourceDataSync(a: ListResourceDataSyncRequest): M[ListResourceDataSyncResponse] = eff1(
      client.listResourceDataSync(a)
    )
    override def listResourceDataSyncPaginator: M[ListResourceDataSyncPublisher] = primitive1(
      client.listResourceDataSyncPaginator
    )
    override def listResourceDataSyncPaginator(a: ListResourceDataSyncRequest): M[ListResourceDataSyncPublisher] =
      primitive1(client.listResourceDataSyncPaginator(a))
    override def listTagsForResource(a: ListTagsForResourceRequest): M[ListTagsForResourceResponse] = eff1(
      client.listTagsForResource(a)
    )
    override def modifyDocumentPermission(a: ModifyDocumentPermissionRequest): M[ModifyDocumentPermissionResponse] =
      eff1(client.modifyDocumentPermission(a))
    override def putComplianceItems(a: PutComplianceItemsRequest): M[PutComplianceItemsResponse] = eff1(
      client.putComplianceItems(a)
    )
    override def putInventory(a: PutInventoryRequest): M[PutInventoryResponse] = eff1(client.putInventory(a))
    override def putParameter(a: PutParameterRequest): M[PutParameterResponse] = eff1(client.putParameter(a))
    override def putResourcePolicy(a: PutResourcePolicyRequest): M[PutResourcePolicyResponse] = eff1(
      client.putResourcePolicy(a)
    )
    override def registerDefaultPatchBaseline(
        a: RegisterDefaultPatchBaselineRequest
    ): M[RegisterDefaultPatchBaselineResponse] = eff1(client.registerDefaultPatchBaseline(a))
    override def registerPatchBaselineForPatchGroup(
        a: RegisterPatchBaselineForPatchGroupRequest
    ): M[RegisterPatchBaselineForPatchGroupResponse] = eff1(client.registerPatchBaselineForPatchGroup(a))
    override def registerTargetWithMaintenanceWindow(
        a: RegisterTargetWithMaintenanceWindowRequest
    ): M[RegisterTargetWithMaintenanceWindowResponse] = eff1(client.registerTargetWithMaintenanceWindow(a))
    override def registerTaskWithMaintenanceWindow(
        a: RegisterTaskWithMaintenanceWindowRequest
    ): M[RegisterTaskWithMaintenanceWindowResponse] = eff1(client.registerTaskWithMaintenanceWindow(a))
    override def removeTagsFromResource(a: RemoveTagsFromResourceRequest): M[RemoveTagsFromResourceResponse] = eff1(
      client.removeTagsFromResource(a)
    )
    override def resetServiceSetting(a: ResetServiceSettingRequest): M[ResetServiceSettingResponse] = eff1(
      client.resetServiceSetting(a)
    )
    override def resumeSession(a: ResumeSessionRequest): M[ResumeSessionResponse] = eff1(client.resumeSession(a))
    override def sendAutomationSignal(a: SendAutomationSignalRequest): M[SendAutomationSignalResponse] = eff1(
      client.sendAutomationSignal(a)
    )
    override def sendCommand(a: SendCommandRequest): M[SendCommandResponse]   = eff1(client.sendCommand(a))
    override def serviceClientConfiguration: M[SsmServiceClientConfiguration] = primitive1(
      client.serviceClientConfiguration
    )
    override def serviceName: M[String] = primitive1(client.serviceName)
    override def startAccessRequest(a: StartAccessRequestRequest): M[StartAccessRequestResponse] = eff1(
      client.startAccessRequest(a)
    )
    override def startAssociationsOnce(a: StartAssociationsOnceRequest): M[StartAssociationsOnceResponse] = eff1(
      client.startAssociationsOnce(a)
    )
    override def startAutomationExecution(a: StartAutomationExecutionRequest): M[StartAutomationExecutionResponse] =
      eff1(client.startAutomationExecution(a))
    override def startChangeRequestExecution(
        a: StartChangeRequestExecutionRequest
    ): M[StartChangeRequestExecutionResponse] = eff1(client.startChangeRequestExecution(a))
    override def startExecutionPreview(a: StartExecutionPreviewRequest): M[StartExecutionPreviewResponse] = eff1(
      client.startExecutionPreview(a)
    )
    override def startSession(a: StartSessionRequest): M[StartSessionResponse] = eff1(client.startSession(a))
    override def stopAutomationExecution(a: StopAutomationExecutionRequest): M[StopAutomationExecutionResponse] = eff1(
      client.stopAutomationExecution(a)
    )
    override def terminateSession(a: TerminateSessionRequest): M[TerminateSessionResponse] = eff1(
      client.terminateSession(a)
    )
    override def unlabelParameterVersion(a: UnlabelParameterVersionRequest): M[UnlabelParameterVersionResponse] = eff1(
      client.unlabelParameterVersion(a)
    )
    override def updateAssociation(a: UpdateAssociationRequest): M[UpdateAssociationResponse] = eff1(
      client.updateAssociation(a)
    )
    override def updateAssociationStatus(a: UpdateAssociationStatusRequest): M[UpdateAssociationStatusResponse] = eff1(
      client.updateAssociationStatus(a)
    )
    override def updateCloudConnector(a: UpdateCloudConnectorRequest): M[UpdateCloudConnectorResponse] = eff1(
      client.updateCloudConnector(a)
    )
    override def updateDocument(a: UpdateDocumentRequest): M[UpdateDocumentResponse] = eff1(client.updateDocument(a))
    override def updateDocumentDefaultVersion(
        a: UpdateDocumentDefaultVersionRequest
    ): M[UpdateDocumentDefaultVersionResponse] = eff1(client.updateDocumentDefaultVersion(a))
    override def updateDocumentMetadata(a: UpdateDocumentMetadataRequest): M[UpdateDocumentMetadataResponse] = eff1(
      client.updateDocumentMetadata(a)
    )
    override def updateMaintenanceWindow(a: UpdateMaintenanceWindowRequest): M[UpdateMaintenanceWindowResponse] = eff1(
      client.updateMaintenanceWindow(a)
    )
    override def updateMaintenanceWindowTarget(
        a: UpdateMaintenanceWindowTargetRequest
    ): M[UpdateMaintenanceWindowTargetResponse] = eff1(client.updateMaintenanceWindowTarget(a))
    override def updateMaintenanceWindowTask(
        a: UpdateMaintenanceWindowTaskRequest
    ): M[UpdateMaintenanceWindowTaskResponse] = eff1(client.updateMaintenanceWindowTask(a))
    override def updateManagedInstanceRole(a: UpdateManagedInstanceRoleRequest): M[UpdateManagedInstanceRoleResponse] =
      eff1(client.updateManagedInstanceRole(a))
    override def updateOpsItem(a: UpdateOpsItemRequest): M[UpdateOpsItemResponse] = eff1(client.updateOpsItem(a))
    override def updateOpsMetadata(a: UpdateOpsMetadataRequest): M[UpdateOpsMetadataResponse] = eff1(
      client.updateOpsMetadata(a)
    )
    override def updatePatchBaseline(a: UpdatePatchBaselineRequest): M[UpdatePatchBaselineResponse] = eff1(
      client.updatePatchBaseline(a)
    )
    override def updateResourceDataSync(a: UpdateResourceDataSyncRequest): M[UpdateResourceDataSyncResponse] = eff1(
      client.updateResourceDataSync(a)
    )
    override def updateServiceSetting(a: UpdateServiceSettingRequest): M[UpdateServiceSettingResponse] = eff1(
      client.updateServiceSetting(a)
    )
    override def validateCloudConnector(a: ValidateCloudConnectorRequest): M[ValidateCloudConnectorResponse] = eff1(
      client.validateCloudConnector(a)
    )
    override def validateCloudConnectorPaginator(a: ValidateCloudConnectorRequest): M[ValidateCloudConnectorPublisher] =
      primitive1(client.validateCloudConnectorPaginator(a))
    override def waiter: M[SsmAsyncWaiter] = primitive1(client.waiter)

  }

}
