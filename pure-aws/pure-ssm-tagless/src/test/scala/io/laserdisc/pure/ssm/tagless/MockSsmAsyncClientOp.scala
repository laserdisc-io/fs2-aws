package io.laserdisc.pure.ssm.tagless

import software.amazon.awssdk.services.ssm.model.*

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

/** A stub implementation of [[SsmAsyncClientOp]], where all methods throw a NotImplementedError.  Intended to
  * be overridden in tests with mock implementations of the methods under test.
  */
class MockSsmAsyncClientOp[F[_]] extends SsmAsyncClientOp[F] {

  def addTagsToResource(a: AddTagsToResourceRequest): F[AddTagsToResourceResponse]                                                                                                           = ???
  def associateOpsItemRelatedItem(a: AssociateOpsItemRelatedItemRequest): F[AssociateOpsItemRelatedItemResponse]                                                                             = ???
  def cancelCommand(a: CancelCommandRequest): F[CancelCommandResponse]                                                                                                                       = ???
  def cancelMaintenanceWindowExecution(a: CancelMaintenanceWindowExecutionRequest): F[CancelMaintenanceWindowExecutionResponse]                                                              = ???
  def close: F[Unit]                                                                                                                                                                         = ???
  def createActivation(a: CreateActivationRequest): F[CreateActivationResponse]                                                                                                              = ???
  def createAssociation(a: CreateAssociationRequest): F[CreateAssociationResponse]                                                                                                           = ???
  def createAssociationBatch(a: CreateAssociationBatchRequest): F[CreateAssociationBatchResponse]                                                                                            = ???
  def createCloudConnector(a: CreateCloudConnectorRequest): F[CreateCloudConnectorResponse]                                                                                                  = ???
  def createDocument(a: CreateDocumentRequest): F[CreateDocumentResponse]                                                                                                                    = ???
  def createMaintenanceWindow(a: CreateMaintenanceWindowRequest): F[CreateMaintenanceWindowResponse]                                                                                         = ???
  def createOpsItem(a: CreateOpsItemRequest): F[CreateOpsItemResponse]                                                                                                                       = ???
  def createOpsMetadata(a: CreateOpsMetadataRequest): F[CreateOpsMetadataResponse]                                                                                                           = ???
  def createPatchBaseline(a: CreatePatchBaselineRequest): F[CreatePatchBaselineResponse]                                                                                                     = ???
  def createResourceDataSync(a: CreateResourceDataSyncRequest): F[CreateResourceDataSyncResponse]                                                                                            = ???
  def deleteActivation(a: DeleteActivationRequest): F[DeleteActivationResponse]                                                                                                              = ???
  def deleteAssociation(a: DeleteAssociationRequest): F[DeleteAssociationResponse]                                                                                                           = ???
  def deleteCloudConnector(a: DeleteCloudConnectorRequest): F[DeleteCloudConnectorResponse]                                                                                                  = ???
  def deleteDocument(a: DeleteDocumentRequest): F[DeleteDocumentResponse]                                                                                                                    = ???
  def deleteInventory(a: DeleteInventoryRequest): F[DeleteInventoryResponse]                                                                                                                 = ???
  def deleteMaintenanceWindow(a: DeleteMaintenanceWindowRequest): F[DeleteMaintenanceWindowResponse]                                                                                         = ???
  def deleteOpsItem(a: DeleteOpsItemRequest): F[DeleteOpsItemResponse]                                                                                                                       = ???
  def deleteOpsMetadata(a: DeleteOpsMetadataRequest): F[DeleteOpsMetadataResponse]                                                                                                           = ???
  def deleteParameter(a: DeleteParameterRequest): F[DeleteParameterResponse]                                                                                                                 = ???
  def deleteParameters(a: DeleteParametersRequest): F[DeleteParametersResponse]                                                                                                              = ???
  def deletePatchBaseline(a: DeletePatchBaselineRequest): F[DeletePatchBaselineResponse]                                                                                                     = ???
  def deleteResourceDataSync(a: DeleteResourceDataSyncRequest): F[DeleteResourceDataSyncResponse]                                                                                            = ???
  def deleteResourcePolicy(a: DeleteResourcePolicyRequest): F[DeleteResourcePolicyResponse]                                                                                                  = ???
  def deregisterManagedInstance(a: DeregisterManagedInstanceRequest): F[DeregisterManagedInstanceResponse]                                                                                   = ???
  def deregisterPatchBaselineForPatchGroup(a: DeregisterPatchBaselineForPatchGroupRequest): F[DeregisterPatchBaselineForPatchGroupResponse]                                                  = ???
  def deregisterTargetFromMaintenanceWindow(a: DeregisterTargetFromMaintenanceWindowRequest): F[DeregisterTargetFromMaintenanceWindowResponse]                                               = ???
  def deregisterTaskFromMaintenanceWindow(a: DeregisterTaskFromMaintenanceWindowRequest): F[DeregisterTaskFromMaintenanceWindowResponse]                                                     = ???
  def describeActivations: F[DescribeActivationsResponse]                                                                                                                                    = ???
  def describeActivations(a: DescribeActivationsRequest): F[DescribeActivationsResponse]                                                                                                     = ???
  def describeActivationsPaginator: F[DescribeActivationsPublisher]                                                                                                                          = ???
  def describeActivationsPaginator(a: DescribeActivationsRequest): F[DescribeActivationsPublisher]                                                                                           = ???
  def describeAssociation(a: DescribeAssociationRequest): F[DescribeAssociationResponse]                                                                                                     = ???
  def describeAssociationExecutionTargets(a: DescribeAssociationExecutionTargetsRequest): F[DescribeAssociationExecutionTargetsResponse]                                                     = ???
  def describeAssociationExecutionTargetsPaginator(a: DescribeAssociationExecutionTargetsRequest): F[DescribeAssociationExecutionTargetsPublisher]                                           = ???
  def describeAssociationExecutions(a: DescribeAssociationExecutionsRequest): F[DescribeAssociationExecutionsResponse]                                                                       = ???
  def describeAssociationExecutionsPaginator(a: DescribeAssociationExecutionsRequest): F[DescribeAssociationExecutionsPublisher]                                                             = ???
  def describeAutomationExecutions: F[DescribeAutomationExecutionsResponse]                                                                                                                  = ???
  def describeAutomationExecutions(a: DescribeAutomationExecutionsRequest): F[DescribeAutomationExecutionsResponse]                                                                          = ???
  def describeAutomationExecutionsPaginator: F[DescribeAutomationExecutionsPublisher]                                                                                                        = ???
  def describeAutomationExecutionsPaginator(a: DescribeAutomationExecutionsRequest): F[DescribeAutomationExecutionsPublisher]                                                                = ???
  def describeAutomationStepExecutions(a: DescribeAutomationStepExecutionsRequest): F[DescribeAutomationStepExecutionsResponse]                                                              = ???
  def describeAutomationStepExecutionsPaginator(a: DescribeAutomationStepExecutionsRequest): F[DescribeAutomationStepExecutionsPublisher]                                                    = ???
  def describeAvailablePatches: F[DescribeAvailablePatchesResponse]                                                                                                                          = ???
  def describeAvailablePatches(a: DescribeAvailablePatchesRequest): F[DescribeAvailablePatchesResponse]                                                                                      = ???
  def describeAvailablePatchesPaginator: F[DescribeAvailablePatchesPublisher]                                                                                                                = ???
  def describeAvailablePatchesPaginator(a: DescribeAvailablePatchesRequest): F[DescribeAvailablePatchesPublisher]                                                                            = ???
  def describeDocument(a: DescribeDocumentRequest): F[DescribeDocumentResponse]                                                                                                              = ???
  def describeDocumentPermission(a: DescribeDocumentPermissionRequest): F[DescribeDocumentPermissionResponse]                                                                                = ???
  def describeEffectiveInstanceAssociations(a: DescribeEffectiveInstanceAssociationsRequest): F[DescribeEffectiveInstanceAssociationsResponse]                                               = ???
  def describeEffectiveInstanceAssociationsPaginator(a: DescribeEffectiveInstanceAssociationsRequest): F[DescribeEffectiveInstanceAssociationsPublisher]                                     = ???
  def describeEffectivePatchesForPatchBaseline(a: DescribeEffectivePatchesForPatchBaselineRequest): F[DescribeEffectivePatchesForPatchBaselineResponse]                                      = ???
  def describeEffectivePatchesForPatchBaselinePaginator(a: DescribeEffectivePatchesForPatchBaselineRequest): F[DescribeEffectivePatchesForPatchBaselinePublisher]                            = ???
  def describeInstanceAssociationsStatus(a: DescribeInstanceAssociationsStatusRequest): F[DescribeInstanceAssociationsStatusResponse]                                                        = ???
  def describeInstanceAssociationsStatusPaginator(a: DescribeInstanceAssociationsStatusRequest): F[DescribeInstanceAssociationsStatusPublisher]                                              = ???
  def describeInstanceInformation: F[DescribeInstanceInformationResponse]                                                                                                                    = ???
  def describeInstanceInformation(a: DescribeInstanceInformationRequest): F[DescribeInstanceInformationResponse]                                                                             = ???
  def describeInstanceInformationPaginator: F[DescribeInstanceInformationPublisher]                                                                                                          = ???
  def describeInstanceInformationPaginator(a: DescribeInstanceInformationRequest): F[DescribeInstanceInformationPublisher]                                                                   = ???
  def describeInstancePatchStates(a: DescribeInstancePatchStatesRequest): F[DescribeInstancePatchStatesResponse]                                                                             = ???
  def describeInstancePatchStatesForPatchGroup(a: DescribeInstancePatchStatesForPatchGroupRequest): F[DescribeInstancePatchStatesForPatchGroupResponse]                                      = ???
  def describeInstancePatchStatesForPatchGroupPaginator(a: DescribeInstancePatchStatesForPatchGroupRequest): F[DescribeInstancePatchStatesForPatchGroupPublisher]                            = ???
  def describeInstancePatchStatesPaginator(a: DescribeInstancePatchStatesRequest): F[DescribeInstancePatchStatesPublisher]                                                                   = ???
  def describeInstancePatches(a: DescribeInstancePatchesRequest): F[DescribeInstancePatchesResponse]                                                                                         = ???
  def describeInstancePatchesPaginator(a: DescribeInstancePatchesRequest): F[DescribeInstancePatchesPublisher]                                                                               = ???
  def describeInstanceProperties(a: DescribeInstancePropertiesRequest): F[DescribeInstancePropertiesResponse]                                                                                = ???
  def describeInstancePropertiesPaginator(a: DescribeInstancePropertiesRequest): F[DescribeInstancePropertiesPublisher]                                                                      = ???
  def describeInventoryDeletions: F[DescribeInventoryDeletionsResponse]                                                                                                                      = ???
  def describeInventoryDeletions(a: DescribeInventoryDeletionsRequest): F[DescribeInventoryDeletionsResponse]                                                                                = ???
  def describeInventoryDeletionsPaginator: F[DescribeInventoryDeletionsPublisher]                                                                                                            = ???
  def describeInventoryDeletionsPaginator(a: DescribeInventoryDeletionsRequest): F[DescribeInventoryDeletionsPublisher]                                                                      = ???
  def describeMaintenanceWindowExecutionTaskInvocations(a: DescribeMaintenanceWindowExecutionTaskInvocationsRequest): F[DescribeMaintenanceWindowExecutionTaskInvocationsResponse]           = ???
  def describeMaintenanceWindowExecutionTaskInvocationsPaginator(a: DescribeMaintenanceWindowExecutionTaskInvocationsRequest): F[DescribeMaintenanceWindowExecutionTaskInvocationsPublisher] = ???
  def describeMaintenanceWindowExecutionTasks(a: DescribeMaintenanceWindowExecutionTasksRequest): F[DescribeMaintenanceWindowExecutionTasksResponse]                                         = ???
  def describeMaintenanceWindowExecutionTasksPaginator(a: DescribeMaintenanceWindowExecutionTasksRequest): F[DescribeMaintenanceWindowExecutionTasksPublisher]                               = ???
  def describeMaintenanceWindowExecutions(a: DescribeMaintenanceWindowExecutionsRequest): F[DescribeMaintenanceWindowExecutionsResponse]                                                     = ???
  def describeMaintenanceWindowExecutionsPaginator(a: DescribeMaintenanceWindowExecutionsRequest): F[DescribeMaintenanceWindowExecutionsPublisher]                                           = ???
  def describeMaintenanceWindowSchedule(a: DescribeMaintenanceWindowScheduleRequest): F[DescribeMaintenanceWindowScheduleResponse]                                                           = ???
  def describeMaintenanceWindowSchedulePaginator(a: DescribeMaintenanceWindowScheduleRequest): F[DescribeMaintenanceWindowSchedulePublisher]                                                 = ???
  def describeMaintenanceWindowTargets(a: DescribeMaintenanceWindowTargetsRequest): F[DescribeMaintenanceWindowTargetsResponse]                                                              = ???
  def describeMaintenanceWindowTargetsPaginator(a: DescribeMaintenanceWindowTargetsRequest): F[DescribeMaintenanceWindowTargetsPublisher]                                                    = ???
  def describeMaintenanceWindowTasks(a: DescribeMaintenanceWindowTasksRequest): F[DescribeMaintenanceWindowTasksResponse]                                                                    = ???
  def describeMaintenanceWindowTasksPaginator(a: DescribeMaintenanceWindowTasksRequest): F[DescribeMaintenanceWindowTasksPublisher]                                                          = ???
  def describeMaintenanceWindows: F[DescribeMaintenanceWindowsResponse]                                                                                                                      = ???
  def describeMaintenanceWindows(a: DescribeMaintenanceWindowsRequest): F[DescribeMaintenanceWindowsResponse]                                                                                = ???
  def describeMaintenanceWindowsForTarget(a: DescribeMaintenanceWindowsForTargetRequest): F[DescribeMaintenanceWindowsForTargetResponse]                                                     = ???
  def describeMaintenanceWindowsForTargetPaginator(a: DescribeMaintenanceWindowsForTargetRequest): F[DescribeMaintenanceWindowsForTargetPublisher]                                           = ???
  def describeMaintenanceWindowsPaginator: F[DescribeMaintenanceWindowsPublisher]                                                                                                            = ???
  def describeMaintenanceWindowsPaginator(a: DescribeMaintenanceWindowsRequest): F[DescribeMaintenanceWindowsPublisher]                                                                      = ???
  def describeOpsItems(a: DescribeOpsItemsRequest): F[DescribeOpsItemsResponse]                                                                                                              = ???
  def describeOpsItemsPaginator(a: DescribeOpsItemsRequest): F[DescribeOpsItemsPublisher]                                                                                                    = ???
  def describeParameters: F[DescribeParametersResponse]                                                                                                                                      = ???
  def describeParameters(a: DescribeParametersRequest): F[DescribeParametersResponse]                                                                                                        = ???
  def describeParametersPaginator: F[DescribeParametersPublisher]                                                                                                                            = ???
  def describeParametersPaginator(a: DescribeParametersRequest): F[DescribeParametersPublisher]                                                                                              = ???
  def describePatchBaselines: F[DescribePatchBaselinesResponse]                                                                                                                              = ???
  def describePatchBaselines(a: DescribePatchBaselinesRequest): F[DescribePatchBaselinesResponse]                                                                                            = ???
  def describePatchBaselinesPaginator: F[DescribePatchBaselinesPublisher]                                                                                                                    = ???
  def describePatchBaselinesPaginator(a: DescribePatchBaselinesRequest): F[DescribePatchBaselinesPublisher]                                                                                  = ???
  def describePatchGroupState(a: DescribePatchGroupStateRequest): F[DescribePatchGroupStateResponse]                                                                                         = ???
  def describePatchGroups: F[DescribePatchGroupsResponse]                                                                                                                                    = ???
  def describePatchGroups(a: DescribePatchGroupsRequest): F[DescribePatchGroupsResponse]                                                                                                     = ???
  def describePatchGroupsPaginator: F[DescribePatchGroupsPublisher]                                                                                                                          = ???
  def describePatchGroupsPaginator(a: DescribePatchGroupsRequest): F[DescribePatchGroupsPublisher]                                                                                           = ???
  def describePatchProperties(a: DescribePatchPropertiesRequest): F[DescribePatchPropertiesResponse]                                                                                         = ???
  def describePatchPropertiesPaginator(a: DescribePatchPropertiesRequest): F[DescribePatchPropertiesPublisher]                                                                               = ???
  def describeSessions(a: DescribeSessionsRequest): F[DescribeSessionsResponse]                                                                                                              = ???
  def describeSessionsPaginator(a: DescribeSessionsRequest): F[DescribeSessionsPublisher]                                                                                                    = ???
  def disassociateOpsItemRelatedItem(a: DisassociateOpsItemRelatedItemRequest): F[DisassociateOpsItemRelatedItemResponse]                                                                    = ???
  def getAccessToken(a: GetAccessTokenRequest): F[GetAccessTokenResponse]                                                                                                                    = ???
  def getAutomationExecution(a: GetAutomationExecutionRequest): F[GetAutomationExecutionResponse]                                                                                            = ???
  def getCalendarState(a: GetCalendarStateRequest): F[GetCalendarStateResponse]                                                                                                              = ???
  def getCloudConnector(a: GetCloudConnectorRequest): F[GetCloudConnectorResponse]                                                                                                           = ???
  def getCommandInvocation(a: GetCommandInvocationRequest): F[GetCommandInvocationResponse]                                                                                                  = ???
  def getConnectionStatus(a: GetConnectionStatusRequest): F[GetConnectionStatusResponse]                                                                                                     = ???
  def getDefaultPatchBaseline: F[GetDefaultPatchBaselineResponse]                                                                                                                            = ???
  def getDefaultPatchBaseline(a: GetDefaultPatchBaselineRequest): F[GetDefaultPatchBaselineResponse]                                                                                         = ???
  def getDeployablePatchSnapshotForInstance(a: GetDeployablePatchSnapshotForInstanceRequest): F[GetDeployablePatchSnapshotForInstanceResponse]                                               = ???
  def getDocument(a: GetDocumentRequest): F[GetDocumentResponse]                                                                                                                             = ???
  def getExecutionPreview(a: GetExecutionPreviewRequest): F[GetExecutionPreviewResponse]                                                                                                     = ???
  def getInventory: F[GetInventoryResponse]                                                                                                                                                  = ???
  def getInventory(a: GetInventoryRequest): F[GetInventoryResponse]                                                                                                                          = ???
  def getInventoryPaginator: F[GetInventoryPublisher]                                                                                                                                        = ???
  def getInventoryPaginator(a: GetInventoryRequest): F[GetInventoryPublisher]                                                                                                                = ???
  def getInventorySchema: F[GetInventorySchemaResponse]                                                                                                                                      = ???
  def getInventorySchema(a: GetInventorySchemaRequest): F[GetInventorySchemaResponse]                                                                                                        = ???
  def getInventorySchemaPaginator: F[GetInventorySchemaPublisher]                                                                                                                            = ???
  def getInventorySchemaPaginator(a: GetInventorySchemaRequest): F[GetInventorySchemaPublisher]                                                                                              = ???
  def getMaintenanceWindow(a: GetMaintenanceWindowRequest): F[GetMaintenanceWindowResponse]                                                                                                  = ???
  def getMaintenanceWindowExecution(a: GetMaintenanceWindowExecutionRequest): F[GetMaintenanceWindowExecutionResponse]                                                                       = ???
  def getMaintenanceWindowExecutionTask(a: GetMaintenanceWindowExecutionTaskRequest): F[GetMaintenanceWindowExecutionTaskResponse]                                                           = ???
  def getMaintenanceWindowExecutionTaskInvocation(a: GetMaintenanceWindowExecutionTaskInvocationRequest): F[GetMaintenanceWindowExecutionTaskInvocationResponse]                             = ???
  def getMaintenanceWindowTask(a: GetMaintenanceWindowTaskRequest): F[GetMaintenanceWindowTaskResponse]                                                                                      = ???
  def getOpsItem(a: GetOpsItemRequest): F[GetOpsItemResponse]                                                                                                                                = ???
  def getOpsMetadata(a: GetOpsMetadataRequest): F[GetOpsMetadataResponse]                                                                                                                    = ???
  def getOpsSummary(a: GetOpsSummaryRequest): F[GetOpsSummaryResponse]                                                                                                                       = ???
  def getOpsSummaryPaginator(a: GetOpsSummaryRequest): F[GetOpsSummaryPublisher]                                                                                                             = ???
  def getParameter(a: GetParameterRequest): F[GetParameterResponse]                                                                                                                          = ???
  def getParameterHistory(a: GetParameterHistoryRequest): F[GetParameterHistoryResponse]                                                                                                     = ???
  def getParameterHistoryPaginator(a: GetParameterHistoryRequest): F[GetParameterHistoryPublisher]                                                                                           = ???
  def getParameters(a: GetParametersRequest): F[GetParametersResponse]                                                                                                                       = ???
  def getParametersByPath(a: GetParametersByPathRequest): F[GetParametersByPathResponse]                                                                                                     = ???
  def getParametersByPathPaginator(a: GetParametersByPathRequest): F[GetParametersByPathPublisher]                                                                                           = ???
  def getPatchBaseline(a: GetPatchBaselineRequest): F[GetPatchBaselineResponse]                                                                                                              = ???
  def getPatchBaselineForPatchGroup(a: GetPatchBaselineForPatchGroupRequest): F[GetPatchBaselineForPatchGroupResponse]                                                                       = ???
  def getResourcePolicies(a: GetResourcePoliciesRequest): F[GetResourcePoliciesResponse]                                                                                                     = ???
  def getResourcePoliciesPaginator(a: GetResourcePoliciesRequest): F[GetResourcePoliciesPublisher]                                                                                           = ???
  def getServiceSetting(a: GetServiceSettingRequest): F[GetServiceSettingResponse]                                                                                                           = ???
  def labelParameterVersion(a: LabelParameterVersionRequest): F[LabelParameterVersionResponse]                                                                                               = ???
  def listAssociationVersions(a: ListAssociationVersionsRequest): F[ListAssociationVersionsResponse]                                                                                         = ???
  def listAssociationVersionsPaginator(a: ListAssociationVersionsRequest): F[ListAssociationVersionsPublisher]                                                                               = ???
  def listAssociations: F[ListAssociationsResponse]                                                                                                                                          = ???
  def listAssociations(a: ListAssociationsRequest): F[ListAssociationsResponse]                                                                                                              = ???
  def listAssociationsPaginator: F[ListAssociationsPublisher]                                                                                                                                = ???
  def listAssociationsPaginator(a: ListAssociationsRequest): F[ListAssociationsPublisher]                                                                                                    = ???
  def listCloudConnectors(a: ListCloudConnectorsRequest): F[ListCloudConnectorsResponse]                                                                                                     = ???
  def listCloudConnectorsPaginator(a: ListCloudConnectorsRequest): F[ListCloudConnectorsPublisher]                                                                                           = ???
  def listCommandInvocations: F[ListCommandInvocationsResponse]                                                                                                                              = ???
  def listCommandInvocations(a: ListCommandInvocationsRequest): F[ListCommandInvocationsResponse]                                                                                            = ???
  def listCommandInvocationsPaginator: F[ListCommandInvocationsPublisher]                                                                                                                    = ???
  def listCommandInvocationsPaginator(a: ListCommandInvocationsRequest): F[ListCommandInvocationsPublisher]                                                                                  = ???
  def listCommands: F[ListCommandsResponse]                                                                                                                                                  = ???
  def listCommands(a: ListCommandsRequest): F[ListCommandsResponse]                                                                                                                          = ???
  def listCommandsPaginator: F[ListCommandsPublisher]                                                                                                                                        = ???
  def listCommandsPaginator(a: ListCommandsRequest): F[ListCommandsPublisher]                                                                                                                = ???
  def listComplianceItems(a: ListComplianceItemsRequest): F[ListComplianceItemsResponse]                                                                                                     = ???
  def listComplianceItemsPaginator(a: ListComplianceItemsRequest): F[ListComplianceItemsPublisher]                                                                                           = ???
  def listComplianceSummaries: F[ListComplianceSummariesResponse]                                                                                                                            = ???
  def listComplianceSummaries(a: ListComplianceSummariesRequest): F[ListComplianceSummariesResponse]                                                                                         = ???
  def listComplianceSummariesPaginator: F[ListComplianceSummariesPublisher]                                                                                                                  = ???
  def listComplianceSummariesPaginator(a: ListComplianceSummariesRequest): F[ListComplianceSummariesPublisher]                                                                               = ???
  def listDocumentMetadataHistory(a: ListDocumentMetadataHistoryRequest): F[ListDocumentMetadataHistoryResponse]                                                                             = ???
  def listDocumentVersions(a: ListDocumentVersionsRequest): F[ListDocumentVersionsResponse]                                                                                                  = ???
  def listDocumentVersionsPaginator(a: ListDocumentVersionsRequest): F[ListDocumentVersionsPublisher]                                                                                        = ???
  def listDocuments: F[ListDocumentsResponse]                                                                                                                                                = ???
  def listDocuments(a: ListDocumentsRequest): F[ListDocumentsResponse]                                                                                                                       = ???
  def listDocumentsPaginator: F[ListDocumentsPublisher]                                                                                                                                      = ???
  def listDocumentsPaginator(a: ListDocumentsRequest): F[ListDocumentsPublisher]                                                                                                             = ???
  def listInventoryEntries(a: ListInventoryEntriesRequest): F[ListInventoryEntriesResponse]                                                                                                  = ???
  def listNodes(a: ListNodesRequest): F[ListNodesResponse]                                                                                                                                   = ???
  def listNodesPaginator(a: ListNodesRequest): F[ListNodesPublisher]                                                                                                                         = ???
  def listNodesSummary(a: ListNodesSummaryRequest): F[ListNodesSummaryResponse]                                                                                                              = ???
  def listNodesSummaryPaginator(a: ListNodesSummaryRequest): F[ListNodesSummaryPublisher]                                                                                                    = ???
  def listOpsItemEvents(a: ListOpsItemEventsRequest): F[ListOpsItemEventsResponse]                                                                                                           = ???
  def listOpsItemEventsPaginator(a: ListOpsItemEventsRequest): F[ListOpsItemEventsPublisher]                                                                                                 = ???
  def listOpsItemRelatedItems(a: ListOpsItemRelatedItemsRequest): F[ListOpsItemRelatedItemsResponse]                                                                                         = ???
  def listOpsItemRelatedItemsPaginator(a: ListOpsItemRelatedItemsRequest): F[ListOpsItemRelatedItemsPublisher]                                                                               = ???
  def listOpsMetadata(a: ListOpsMetadataRequest): F[ListOpsMetadataResponse]                                                                                                                 = ???
  def listOpsMetadataPaginator(a: ListOpsMetadataRequest): F[ListOpsMetadataPublisher]                                                                                                       = ???
  def listResourceComplianceSummaries: F[ListResourceComplianceSummariesResponse]                                                                                                            = ???
  def listResourceComplianceSummaries(a: ListResourceComplianceSummariesRequest): F[ListResourceComplianceSummariesResponse]                                                                 = ???
  def listResourceComplianceSummariesPaginator: F[ListResourceComplianceSummariesPublisher]                                                                                                  = ???
  def listResourceComplianceSummariesPaginator(a: ListResourceComplianceSummariesRequest): F[ListResourceComplianceSummariesPublisher]                                                       = ???
  def listResourceDataSync: F[ListResourceDataSyncResponse]                                                                                                                                  = ???
  def listResourceDataSync(a: ListResourceDataSyncRequest): F[ListResourceDataSyncResponse]                                                                                                  = ???
  def listResourceDataSyncPaginator: F[ListResourceDataSyncPublisher]                                                                                                                        = ???
  def listResourceDataSyncPaginator(a: ListResourceDataSyncRequest): F[ListResourceDataSyncPublisher]                                                                                        = ???
  def listTagsForResource(a: ListTagsForResourceRequest): F[ListTagsForResourceResponse]                                                                                                     = ???
  def modifyDocumentPermission(a: ModifyDocumentPermissionRequest): F[ModifyDocumentPermissionResponse]                                                                                      = ???
  def putComplianceItems(a: PutComplianceItemsRequest): F[PutComplianceItemsResponse]                                                                                                        = ???
  def putInventory(a: PutInventoryRequest): F[PutInventoryResponse]                                                                                                                          = ???
  def putParameter(a: PutParameterRequest): F[PutParameterResponse]                                                                                                                          = ???
  def putResourcePolicy(a: PutResourcePolicyRequest): F[PutResourcePolicyResponse]                                                                                                           = ???
  def registerDefaultPatchBaseline(a: RegisterDefaultPatchBaselineRequest): F[RegisterDefaultPatchBaselineResponse]                                                                          = ???
  def registerPatchBaselineForPatchGroup(a: RegisterPatchBaselineForPatchGroupRequest): F[RegisterPatchBaselineForPatchGroupResponse]                                                        = ???
  def registerTargetWithMaintenanceWindow(a: RegisterTargetWithMaintenanceWindowRequest): F[RegisterTargetWithMaintenanceWindowResponse]                                                     = ???
  def registerTaskWithMaintenanceWindow(a: RegisterTaskWithMaintenanceWindowRequest): F[RegisterTaskWithMaintenanceWindowResponse]                                                           = ???
  def removeTagsFromResource(a: RemoveTagsFromResourceRequest): F[RemoveTagsFromResourceResponse]                                                                                            = ???
  def resetServiceSetting(a: ResetServiceSettingRequest): F[ResetServiceSettingResponse]                                                                                                     = ???
  def resumeSession(a: ResumeSessionRequest): F[ResumeSessionResponse]                                                                                                                       = ???
  def sendAutomationSignal(a: SendAutomationSignalRequest): F[SendAutomationSignalResponse]                                                                                                  = ???
  def sendCommand(a: SendCommandRequest): F[SendCommandResponse]                                                                                                                             = ???
  def serviceClientConfiguration: F[SsmServiceClientConfiguration]                                                                                                                           = ???
  def serviceName: F[String]                                                                                                                                                                 = ???
  def startAccessRequest(a: StartAccessRequestRequest): F[StartAccessRequestResponse]                                                                                                        = ???
  def startAssociationsOnce(a: StartAssociationsOnceRequest): F[StartAssociationsOnceResponse]                                                                                               = ???
  def startAutomationExecution(a: StartAutomationExecutionRequest): F[StartAutomationExecutionResponse]                                                                                      = ???
  def startChangeRequestExecution(a: StartChangeRequestExecutionRequest): F[StartChangeRequestExecutionResponse]                                                                             = ???
  def startExecutionPreview(a: StartExecutionPreviewRequest): F[StartExecutionPreviewResponse]                                                                                               = ???
  def startSession(a: StartSessionRequest): F[StartSessionResponse]                                                                                                                          = ???
  def stopAutomationExecution(a: StopAutomationExecutionRequest): F[StopAutomationExecutionResponse]                                                                                         = ???
  def terminateSession(a: TerminateSessionRequest): F[TerminateSessionResponse]                                                                                                              = ???
  def unlabelParameterVersion(a: UnlabelParameterVersionRequest): F[UnlabelParameterVersionResponse]                                                                                         = ???
  def updateAssociation(a: UpdateAssociationRequest): F[UpdateAssociationResponse]                                                                                                           = ???
  def updateAssociationStatus(a: UpdateAssociationStatusRequest): F[UpdateAssociationStatusResponse]                                                                                         = ???
  def updateCloudConnector(a: UpdateCloudConnectorRequest): F[UpdateCloudConnectorResponse]                                                                                                  = ???
  def updateDocument(a: UpdateDocumentRequest): F[UpdateDocumentResponse]                                                                                                                    = ???
  def updateDocumentDefaultVersion(a: UpdateDocumentDefaultVersionRequest): F[UpdateDocumentDefaultVersionResponse]                                                                          = ???
  def updateDocumentMetadata(a: UpdateDocumentMetadataRequest): F[UpdateDocumentMetadataResponse]                                                                                            = ???
  def updateMaintenanceWindow(a: UpdateMaintenanceWindowRequest): F[UpdateMaintenanceWindowResponse]                                                                                         = ???
  def updateMaintenanceWindowTarget(a: UpdateMaintenanceWindowTargetRequest): F[UpdateMaintenanceWindowTargetResponse]                                                                       = ???
  def updateMaintenanceWindowTask(a: UpdateMaintenanceWindowTaskRequest): F[UpdateMaintenanceWindowTaskResponse]                                                                             = ???
  def updateManagedInstanceRole(a: UpdateManagedInstanceRoleRequest): F[UpdateManagedInstanceRoleResponse]                                                                                   = ???
  def updateOpsItem(a: UpdateOpsItemRequest): F[UpdateOpsItemResponse]                                                                                                                       = ???
  def updateOpsMetadata(a: UpdateOpsMetadataRequest): F[UpdateOpsMetadataResponse]                                                                                                           = ???
  def updatePatchBaseline(a: UpdatePatchBaselineRequest): F[UpdatePatchBaselineResponse]                                                                                                     = ???
  def updateResourceDataSync(a: UpdateResourceDataSyncRequest): F[UpdateResourceDataSyncResponse]                                                                                            = ???
  def updateServiceSetting(a: UpdateServiceSettingRequest): F[UpdateServiceSettingResponse]                                                                                                  = ???
  def validateCloudConnector(a: ValidateCloudConnectorRequest): F[ValidateCloudConnectorResponse]                                                                                            = ???
  def validateCloudConnectorPaginator(a: ValidateCloudConnectorRequest): F[ValidateCloudConnectorPublisher]                                                                                  = ???
  def waiter: F[SsmAsyncWaiter]                                                                                                                                                              = ???

}
