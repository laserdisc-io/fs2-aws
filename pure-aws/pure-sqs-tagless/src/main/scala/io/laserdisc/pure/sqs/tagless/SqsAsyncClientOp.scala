package io.laserdisc.pure.sqs.tagless

import software.amazon.awssdk.services.sqs.SqsServiceClientConfiguration
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager
import software.amazon.awssdk.services.sqs.model.*
import software.amazon.awssdk.services.sqs.paginators.*

trait SqsAsyncClientOp[F[_]] {
  // SqsAsyncClient
  def addPermission(a: AddPermissionRequest): F[AddPermissionResponse]
  def batchManager: F[SqsAsyncBatchManager]
  def cancelMessageMoveTask(a: CancelMessageMoveTaskRequest): F[CancelMessageMoveTaskResponse]
  def changeMessageVisibility(a: ChangeMessageVisibilityRequest): F[ChangeMessageVisibilityResponse]
  def changeMessageVisibilityBatch(a: ChangeMessageVisibilityBatchRequest): F[ChangeMessageVisibilityBatchResponse]
  def close: F[Unit]
  def createQueue(a: CreateQueueRequest): F[CreateQueueResponse]
  def deleteMessage(a: DeleteMessageRequest): F[DeleteMessageResponse]
  def deleteMessageBatch(a: DeleteMessageBatchRequest): F[DeleteMessageBatchResponse]
  def deleteQueue(a: DeleteQueueRequest): F[DeleteQueueResponse]
  def getQueueAttributes(a: GetQueueAttributesRequest): F[GetQueueAttributesResponse]
  def getQueueUrl(a: GetQueueUrlRequest): F[GetQueueUrlResponse]
  def listDeadLetterSourceQueues(a: ListDeadLetterSourceQueuesRequest): F[ListDeadLetterSourceQueuesResponse]
  def listDeadLetterSourceQueuesPaginator(a: ListDeadLetterSourceQueuesRequest): F[ListDeadLetterSourceQueuesPublisher]
  def listMessageMoveTasks(a: ListMessageMoveTasksRequest): F[ListMessageMoveTasksResponse]
  def listQueueTags(a: ListQueueTagsRequest): F[ListQueueTagsResponse]
  def listQueues: F[ListQueuesResponse]
  def listQueues(a: ListQueuesRequest): F[ListQueuesResponse]
  def listQueuesPaginator: F[ListQueuesPublisher]
  def listQueuesPaginator(a: ListQueuesRequest): F[ListQueuesPublisher]
  def purgeQueue(a: PurgeQueueRequest): F[PurgeQueueResponse]
  def receiveMessage(a: ReceiveMessageRequest): F[ReceiveMessageResponse]
  def removePermission(a: RemovePermissionRequest): F[RemovePermissionResponse]
  def sendMessage(a: SendMessageRequest): F[SendMessageResponse]
  def sendMessageBatch(a: SendMessageBatchRequest): F[SendMessageBatchResponse]
  def serviceClientConfiguration: F[SqsServiceClientConfiguration]
  def serviceName: F[String]
  def setQueueAttributes(a: SetQueueAttributesRequest): F[SetQueueAttributesResponse]
  def startMessageMoveTask(a: StartMessageMoveTaskRequest): F[StartMessageMoveTaskResponse]
  def tagQueue(a: TagQueueRequest): F[TagQueueResponse]
  def untagQueue(a: UntagQueueRequest): F[UntagQueueResponse]

}
