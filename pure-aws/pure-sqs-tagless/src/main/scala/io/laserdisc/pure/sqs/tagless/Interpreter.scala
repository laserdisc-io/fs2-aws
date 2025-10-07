package io.laserdisc.pure.sqs.tagless

// Library imports
import cats.data.Kleisli
import cats.effect.{ Async,  Resource }

import software.amazon.awssdk.services.sqs.*
import software.amazon.awssdk.services.sqs.model.*

// Types referenced
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager
import software.amazon.awssdk.services.sqs.paginators.ListDeadLetterSourceQueuesPublisher
import software.amazon.awssdk.services.sqs.paginators.ListQueuesPublisher


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

  lazy val SqsAsyncClientInterpreter: SqsAsyncClientInterpreter = new SqsAsyncClientInterpreter { }
  // Some methods are common to all interpreters and can be overridden to change behavior globally.

  def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(j => asyncM.blocking(f(j)))
  def primitive1[J, A](f: => A): M[A]              = asyncM.blocking(f)

  def eff[J, A](fut: J => CompletableFuture[A]): Kleisli[M, J, A] = Kleisli { j =>
    asyncM.fromCompletableFuture(asyncM.delay(fut(j)))
  }
  def eff1[J, A](fut: => CompletableFuture[A]): M[A] =
    asyncM.fromCompletableFuture(asyncM.delay(fut))

  // Interpreters
  trait SqsAsyncClientInterpreter extends SqsAsyncClientOp[Kleisli[M, SqsAsyncClient, *]] {
  
    // domain-specific operations are implemented in terms of `primitive`
    override def addPermission(a: AddPermissionRequest): Kleisli[M, SqsAsyncClient, AddPermissionResponse] = eff(_.addPermission(a)) // B
    override def batchManager : Kleisli[M, SqsAsyncClient, SqsAsyncBatchManager] = primitive(_.batchManager) // A
    override def cancelMessageMoveTask(a: CancelMessageMoveTaskRequest): Kleisli[M, SqsAsyncClient, CancelMessageMoveTaskResponse] = eff(_.cancelMessageMoveTask(a)) // B
    override def changeMessageVisibility(a: ChangeMessageVisibilityRequest): Kleisli[M, SqsAsyncClient, ChangeMessageVisibilityResponse] = eff(_.changeMessageVisibility(a)) // B
    override def changeMessageVisibilityBatch(a: ChangeMessageVisibilityBatchRequest): Kleisli[M, SqsAsyncClient, ChangeMessageVisibilityBatchResponse] = eff(_.changeMessageVisibilityBatch(a)) // B
    override def close : Kleisli[M, SqsAsyncClient, Unit] = primitive(_.close) // A
    override def createQueue(a: CreateQueueRequest): Kleisli[M, SqsAsyncClient, CreateQueueResponse] = eff(_.createQueue(a)) // B
    override def deleteMessage(a: DeleteMessageRequest): Kleisli[M, SqsAsyncClient, DeleteMessageResponse] = eff(_.deleteMessage(a)) // B
    override def deleteMessageBatch(a: DeleteMessageBatchRequest): Kleisli[M, SqsAsyncClient, DeleteMessageBatchResponse] = eff(_.deleteMessageBatch(a)) // B
    override def deleteQueue(a: DeleteQueueRequest): Kleisli[M, SqsAsyncClient, DeleteQueueResponse] = eff(_.deleteQueue(a)) // B
    override def getQueueAttributes(a: GetQueueAttributesRequest): Kleisli[M, SqsAsyncClient, GetQueueAttributesResponse] = eff(_.getQueueAttributes(a)) // B
    override def getQueueUrl(a: GetQueueUrlRequest): Kleisli[M, SqsAsyncClient, GetQueueUrlResponse] = eff(_.getQueueUrl(a)) // B
    override def listDeadLetterSourceQueues(a: ListDeadLetterSourceQueuesRequest): Kleisli[M, SqsAsyncClient, ListDeadLetterSourceQueuesResponse] = eff(_.listDeadLetterSourceQueues(a)) // B
    override def listDeadLetterSourceQueuesPaginator(a: ListDeadLetterSourceQueuesRequest): Kleisli[M, SqsAsyncClient, ListDeadLetterSourceQueuesPublisher] = primitive(_.listDeadLetterSourceQueuesPaginator(a)) // B
    override def listMessageMoveTasks(a: ListMessageMoveTasksRequest): Kleisli[M, SqsAsyncClient, ListMessageMoveTasksResponse] = eff(_.listMessageMoveTasks(a)) // B
    override def listQueueTags(a: ListQueueTagsRequest): Kleisli[M, SqsAsyncClient, ListQueueTagsResponse] = eff(_.listQueueTags(a)) // B
    override def listQueues : Kleisli[M, SqsAsyncClient, ListQueuesResponse] = eff(_.listQueues) // A
    override def listQueues(a: ListQueuesRequest): Kleisli[M, SqsAsyncClient, ListQueuesResponse] = eff(_.listQueues(a)) // B
    override def listQueuesPaginator : Kleisli[M, SqsAsyncClient, ListQueuesPublisher] = primitive(_.listQueuesPaginator) // A
    override def listQueuesPaginator(a: ListQueuesRequest): Kleisli[M, SqsAsyncClient, ListQueuesPublisher] = primitive(_.listQueuesPaginator(a)) // B
    override def purgeQueue(a: PurgeQueueRequest): Kleisli[M, SqsAsyncClient, PurgeQueueResponse] = eff(_.purgeQueue(a)) // B
    override def receiveMessage(a: ReceiveMessageRequest): Kleisli[M, SqsAsyncClient, ReceiveMessageResponse] = eff(_.receiveMessage(a)) // B
    override def removePermission(a: RemovePermissionRequest): Kleisli[M, SqsAsyncClient, RemovePermissionResponse] = eff(_.removePermission(a)) // B
    override def sendMessage(a: SendMessageRequest): Kleisli[M, SqsAsyncClient, SendMessageResponse] = eff(_.sendMessage(a)) // B
    override def sendMessageBatch(a: SendMessageBatchRequest): Kleisli[M, SqsAsyncClient, SendMessageBatchResponse] = eff(_.sendMessageBatch(a)) // B
    override def serviceName : Kleisli[M, SqsAsyncClient, String] = primitive(_.serviceName) // A
    override def setQueueAttributes(a: SetQueueAttributesRequest): Kleisli[M, SqsAsyncClient, SetQueueAttributesResponse] = eff(_.setQueueAttributes(a)) // B
    override def startMessageMoveTask(a: StartMessageMoveTaskRequest): Kleisli[M, SqsAsyncClient, StartMessageMoveTaskResponse] = eff(_.startMessageMoveTask(a)) // B
    override def tagQueue(a: TagQueueRequest): Kleisli[M, SqsAsyncClient, TagQueueResponse] = eff(_.tagQueue(a)) // B
    override def untagQueue(a: UntagQueueRequest): Kleisli[M, SqsAsyncClient, UntagQueueResponse] = eff(_.untagQueue(a)) // B
  
  
    def lens[E](f: E => SqsAsyncClient): SqsAsyncClientOp[Kleisli[M, E, *]] =
      new SqsAsyncClientOp[Kleisli[M, E, *]] {
      override def addPermission(a: AddPermissionRequest) = Kleisli(e => eff1(f(e).addPermission(a)))
      override def batchManager = Kleisli(e => primitive1(f(e).batchManager))
      override def cancelMessageMoveTask(a: CancelMessageMoveTaskRequest) = Kleisli(e => eff1(f(e).cancelMessageMoveTask(a)))
      override def changeMessageVisibility(a: ChangeMessageVisibilityRequest) = Kleisli(e => eff1(f(e).changeMessageVisibility(a)))
      override def changeMessageVisibilityBatch(a: ChangeMessageVisibilityBatchRequest) = Kleisli(e => eff1(f(e).changeMessageVisibilityBatch(a)))
      override def close = Kleisli(e => primitive1(f(e).close))
      override def createQueue(a: CreateQueueRequest) = Kleisli(e => eff1(f(e).createQueue(a)))
      override def deleteMessage(a: DeleteMessageRequest) = Kleisli(e => eff1(f(e).deleteMessage(a)))
      override def deleteMessageBatch(a: DeleteMessageBatchRequest) = Kleisli(e => eff1(f(e).deleteMessageBatch(a)))
      override def deleteQueue(a: DeleteQueueRequest) = Kleisli(e => eff1(f(e).deleteQueue(a)))
      override def getQueueAttributes(a: GetQueueAttributesRequest) = Kleisli(e => eff1(f(e).getQueueAttributes(a)))
      override def getQueueUrl(a: GetQueueUrlRequest) = Kleisli(e => eff1(f(e).getQueueUrl(a)))
      override def listDeadLetterSourceQueues(a: ListDeadLetterSourceQueuesRequest) = Kleisli(e => eff1(f(e).listDeadLetterSourceQueues(a)))
      override def listDeadLetterSourceQueuesPaginator(a: ListDeadLetterSourceQueuesRequest) = Kleisli(e => primitive1(f(e).listDeadLetterSourceQueuesPaginator(a)))
      override def listMessageMoveTasks(a: ListMessageMoveTasksRequest) = Kleisli(e => eff1(f(e).listMessageMoveTasks(a)))
      override def listQueueTags(a: ListQueueTagsRequest) = Kleisli(e => eff1(f(e).listQueueTags(a)))
      override def listQueues = Kleisli(e => eff1(f(e).listQueues))
      override def listQueues(a: ListQueuesRequest) = Kleisli(e => eff1(f(e).listQueues(a)))
      override def listQueuesPaginator = Kleisli(e => primitive1(f(e).listQueuesPaginator))
      override def listQueuesPaginator(a: ListQueuesRequest) = Kleisli(e => primitive1(f(e).listQueuesPaginator(a)))
      override def purgeQueue(a: PurgeQueueRequest) = Kleisli(e => eff1(f(e).purgeQueue(a)))
      override def receiveMessage(a: ReceiveMessageRequest) = Kleisli(e => eff1(f(e).receiveMessage(a)))
      override def removePermission(a: RemovePermissionRequest) = Kleisli(e => eff1(f(e).removePermission(a)))
      override def sendMessage(a: SendMessageRequest) = Kleisli(e => eff1(f(e).sendMessage(a)))
      override def sendMessageBatch(a: SendMessageBatchRequest) = Kleisli(e => eff1(f(e).sendMessageBatch(a)))
      override def serviceName = Kleisli(e => primitive1(f(e).serviceName))
      override def setQueueAttributes(a: SetQueueAttributesRequest) = Kleisli(e => eff1(f(e).setQueueAttributes(a)))
      override def startMessageMoveTask(a: StartMessageMoveTaskRequest) = Kleisli(e => eff1(f(e).startMessageMoveTask(a)))
      override def tagQueue(a: TagQueueRequest) = Kleisli(e => eff1(f(e).tagQueue(a)))
      override def untagQueue(a: UntagQueueRequest) = Kleisli(e => eff1(f(e).untagQueue(a)))
  
      }
    }

  // end interpreters

  def SqsAsyncClientResource(builder : SqsAsyncClientBuilder) : Resource[M, SqsAsyncClient] = Resource.fromAutoCloseable(asyncM.delay(builder.build()))
  def SqsAsyncClientOpResource(builder: SqsAsyncClientBuilder) = SqsAsyncClientResource(builder).map(create)

  def create(client : SqsAsyncClient) : SqsAsyncClientOp[M] = new SqsAsyncClientOp[M] {

    // domain-specific operations are implemented in terms of `primitive`
    override def addPermission(a: AddPermissionRequest) = eff1(client.addPermission(a))
    override def batchManager = primitive1(client.batchManager)
    override def cancelMessageMoveTask(a: CancelMessageMoveTaskRequest) = eff1(client.cancelMessageMoveTask(a))
    override def changeMessageVisibility(a: ChangeMessageVisibilityRequest) = eff1(client.changeMessageVisibility(a))
    override def changeMessageVisibilityBatch(a: ChangeMessageVisibilityBatchRequest) = eff1(client.changeMessageVisibilityBatch(a))
    override def close = primitive1(client.close)
    override def createQueue(a: CreateQueueRequest) = eff1(client.createQueue(a))
    override def deleteMessage(a: DeleteMessageRequest) = eff1(client.deleteMessage(a))
    override def deleteMessageBatch(a: DeleteMessageBatchRequest) = eff1(client.deleteMessageBatch(a))
    override def deleteQueue(a: DeleteQueueRequest) = eff1(client.deleteQueue(a))
    override def getQueueAttributes(a: GetQueueAttributesRequest) = eff1(client.getQueueAttributes(a))
    override def getQueueUrl(a: GetQueueUrlRequest) = eff1(client.getQueueUrl(a))
    override def listDeadLetterSourceQueues(a: ListDeadLetterSourceQueuesRequest) = eff1(client.listDeadLetterSourceQueues(a))
    override def listDeadLetterSourceQueuesPaginator(a: ListDeadLetterSourceQueuesRequest) = primitive1(client.listDeadLetterSourceQueuesPaginator(a))
    override def listMessageMoveTasks(a: ListMessageMoveTasksRequest) = eff1(client.listMessageMoveTasks(a))
    override def listQueueTags(a: ListQueueTagsRequest) = eff1(client.listQueueTags(a))
    override def listQueues = eff1(client.listQueues)
    override def listQueues(a: ListQueuesRequest) = eff1(client.listQueues(a))
    override def listQueuesPaginator = primitive1(client.listQueuesPaginator)
    override def listQueuesPaginator(a: ListQueuesRequest) = primitive1(client.listQueuesPaginator(a))
    override def purgeQueue(a: PurgeQueueRequest) = eff1(client.purgeQueue(a))
    override def receiveMessage(a: ReceiveMessageRequest) = eff1(client.receiveMessage(a))
    override def removePermission(a: RemovePermissionRequest) = eff1(client.removePermission(a))
    override def sendMessage(a: SendMessageRequest) = eff1(client.sendMessage(a))
    override def sendMessageBatch(a: SendMessageBatchRequest) = eff1(client.sendMessageBatch(a))
    override def serviceName = primitive1(client.serviceName)
    override def setQueueAttributes(a: SetQueueAttributesRequest) = eff1(client.setQueueAttributes(a))
    override def startMessageMoveTask(a: StartMessageMoveTaskRequest) = eff1(client.startMessageMoveTask(a))
    override def tagQueue(a: TagQueueRequest) = eff1(client.tagQueue(a))
    override def untagQueue(a: UntagQueueRequest) = eff1(client.untagQueue(a))


  }


}

