package io.laserdisc.pure.sqs.tagless

// Library imports
import cats.data.Kleisli
import cats.effect.{Async, Resource}

import software.amazon.awssdk.services.sqs.*
import software.amazon.awssdk.services.sqs.model.*

// Types referenced
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager
import software.amazon.awssdk.services.sqs.paginators.ListDeadLetterSourceQueuesPublisher
import software.amazon.awssdk.services.sqs.paginators.ListQueuesPublisher

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

  lazy val SqsAsyncClientInterpreter: SqsAsyncClientInterpreter = new SqsAsyncClientInterpreter {}
  // Some methods are common to all interpreters and can be overridden to change behavior globally.

  private def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(j => asyncM.blocking(f(j)))
  private def primitive1[J, A](f: => A): M[A]              = asyncM.blocking(f)

  private def eff[J, A](fut: J => CompletableFuture[A]): Kleisli[M, J, A] = Kleisli { j =>
    asyncM.fromCompletableFuture(asyncM.delay(fut(j)))
  }
  private def eff1[J, A](fut: => CompletableFuture[A]): M[A] =
    asyncM.fromCompletableFuture(asyncM.delay(fut))

  // Interpreters // scalafmt: off
  trait SqsAsyncClientInterpreter extends SqsAsyncClientOp[Kleisli[M, SqsAsyncClient, *]] {

    // domain-specific operations are implemented in terms of `primitive`
    override def addPermission(a: AddPermissionRequest): Kleisli[M, SqsAsyncClient, AddPermissionResponse]                                                  = eff(_.addPermission(a))
    override def batchManager: Kleisli[M, SqsAsyncClient, SqsAsyncBatchManager]                                                                             = primitive(_.batchManager)
    override def cancelMessageMoveTask(a: CancelMessageMoveTaskRequest): Kleisli[M, SqsAsyncClient, CancelMessageMoveTaskResponse]                          = eff(_.cancelMessageMoveTask(a))
    override def changeMessageVisibility(a: ChangeMessageVisibilityRequest): Kleisli[M, SqsAsyncClient, ChangeMessageVisibilityResponse]                    = eff(_.changeMessageVisibility(a))
    override def changeMessageVisibilityBatch(a: ChangeMessageVisibilityBatchRequest): Kleisli[M, SqsAsyncClient, ChangeMessageVisibilityBatchResponse]     = eff(_.changeMessageVisibilityBatch(a))
    override def close: Kleisli[M, SqsAsyncClient, Unit]                                                                                                    = primitive(_.close)
    override def createQueue(a: CreateQueueRequest): Kleisli[M, SqsAsyncClient, CreateQueueResponse]                                                        = eff(_.createQueue(a))
    override def deleteMessage(a: DeleteMessageRequest): Kleisli[M, SqsAsyncClient, DeleteMessageResponse]                                                  = eff(_.deleteMessage(a))
    override def deleteMessageBatch(a: DeleteMessageBatchRequest): Kleisli[M, SqsAsyncClient, DeleteMessageBatchResponse]                                   = eff(_.deleteMessageBatch(a))
    override def deleteQueue(a: DeleteQueueRequest): Kleisli[M, SqsAsyncClient, DeleteQueueResponse]                                                        = eff(_.deleteQueue(a))
    override def getQueueAttributes(a: GetQueueAttributesRequest): Kleisli[M, SqsAsyncClient, GetQueueAttributesResponse]                                   = eff(_.getQueueAttributes(a))
    override def getQueueUrl(a: GetQueueUrlRequest): Kleisli[M, SqsAsyncClient, GetQueueUrlResponse]                                                        = eff(_.getQueueUrl(a))
    override def listDeadLetterSourceQueues(a: ListDeadLetterSourceQueuesRequest): Kleisli[M, SqsAsyncClient, ListDeadLetterSourceQueuesResponse]           = eff(_.listDeadLetterSourceQueues(a))
    override def listDeadLetterSourceQueuesPaginator(a: ListDeadLetterSourceQueuesRequest): Kleisli[M, SqsAsyncClient, ListDeadLetterSourceQueuesPublisher] = primitive(_.listDeadLetterSourceQueuesPaginator(a))
    override def listMessageMoveTasks(a: ListMessageMoveTasksRequest): Kleisli[M, SqsAsyncClient, ListMessageMoveTasksResponse]                             = eff(_.listMessageMoveTasks(a))
    override def listQueueTags(a: ListQueueTagsRequest): Kleisli[M, SqsAsyncClient, ListQueueTagsResponse]                                                  = eff(_.listQueueTags(a))
    override def listQueues: Kleisli[M, SqsAsyncClient, ListQueuesResponse]                                                                                 = eff(_.listQueues)
    override def listQueues(a: ListQueuesRequest): Kleisli[M, SqsAsyncClient, ListQueuesResponse]                                                           = eff(_.listQueues(a))
    override def listQueuesPaginator: Kleisli[M, SqsAsyncClient, ListQueuesPublisher]                                                                       = primitive(_.listQueuesPaginator)
    override def listQueuesPaginator(a: ListQueuesRequest): Kleisli[M, SqsAsyncClient, ListQueuesPublisher]                                                 = primitive(_.listQueuesPaginator(a))
    override def purgeQueue(a: PurgeQueueRequest): Kleisli[M, SqsAsyncClient, PurgeQueueResponse]                                                           = eff(_.purgeQueue(a))
    override def receiveMessage(a: ReceiveMessageRequest): Kleisli[M, SqsAsyncClient, ReceiveMessageResponse]                                               = eff(_.receiveMessage(a))
    override def removePermission(a: RemovePermissionRequest): Kleisli[M, SqsAsyncClient, RemovePermissionResponse]                                         = eff(_.removePermission(a))
    override def sendMessage(a: SendMessageRequest): Kleisli[M, SqsAsyncClient, SendMessageResponse]                                                        = eff(_.sendMessage(a))
    override def sendMessageBatch(a: SendMessageBatchRequest): Kleisli[M, SqsAsyncClient, SendMessageBatchResponse]                                         = eff(_.sendMessageBatch(a))
    override def serviceName: Kleisli[M, SqsAsyncClient, String]                                                                                            = primitive(_.serviceName)
    override def setQueueAttributes(a: SetQueueAttributesRequest): Kleisli[M, SqsAsyncClient, SetQueueAttributesResponse]                                   = eff(_.setQueueAttributes(a))
    override def startMessageMoveTask(a: StartMessageMoveTaskRequest): Kleisli[M, SqsAsyncClient, StartMessageMoveTaskResponse]                             = eff(_.startMessageMoveTask(a))
    override def tagQueue(a: TagQueueRequest): Kleisli[M, SqsAsyncClient, TagQueueResponse]                                                                 = eff(_.tagQueue(a))
    override def untagQueue(a: UntagQueueRequest): Kleisli[M, SqsAsyncClient, UntagQueueResponse]                                                           = eff(_.untagQueue(a))

    def lens[E](f: E => SqsAsyncClient): SqsAsyncClientOp[Kleisli[M, E, *]] =
      new SqsAsyncClientOp[Kleisli[M, E, *]] {
        override def addPermission(a: AddPermissionRequest): Kleisli[M, E, AddPermissionResponse]                                                  = Kleisli(e => eff1(f(e).addPermission(a)))
        override def batchManager: Kleisli[M, E, SqsAsyncBatchManager]                                                                             = Kleisli(e => primitive1(f(e).batchManager))
        override def cancelMessageMoveTask(a: CancelMessageMoveTaskRequest): Kleisli[M, E, CancelMessageMoveTaskResponse]                          = Kleisli(e => eff1(f(e).cancelMessageMoveTask(a)))
        override def changeMessageVisibility(a: ChangeMessageVisibilityRequest): Kleisli[M, E, ChangeMessageVisibilityResponse]                    = Kleisli(e => eff1(f(e).changeMessageVisibility(a)))
        override def changeMessageVisibilityBatch(a: ChangeMessageVisibilityBatchRequest): Kleisli[M, E, ChangeMessageVisibilityBatchResponse]     = Kleisli(e => eff1(f(e).changeMessageVisibilityBatch(a)))
        override def close: Kleisli[M, E, Unit]                                                                                                    = Kleisli(e => primitive1(f(e).close))
        override def createQueue(a: CreateQueueRequest): Kleisli[M, E, CreateQueueResponse]                                                        = Kleisli(e => eff1(f(e).createQueue(a)))
        override def deleteMessage(a: DeleteMessageRequest): Kleisli[M, E, DeleteMessageResponse]                                                  = Kleisli(e => eff1(f(e).deleteMessage(a)))
        override def deleteMessageBatch(a: DeleteMessageBatchRequest): Kleisli[M, E, DeleteMessageBatchResponse]                                   = Kleisli(e => eff1(f(e).deleteMessageBatch(a)))
        override def deleteQueue(a: DeleteQueueRequest): Kleisli[M, E, DeleteQueueResponse]                                                        = Kleisli(e => eff1(f(e).deleteQueue(a)))
        override def getQueueAttributes(a: GetQueueAttributesRequest): Kleisli[M, E, GetQueueAttributesResponse]                                   = Kleisli(e => eff1(f(e).getQueueAttributes(a)))
        override def getQueueUrl(a: GetQueueUrlRequest): Kleisli[M, E, GetQueueUrlResponse]                                                        = Kleisli(e => eff1(f(e).getQueueUrl(a)))
        override def listDeadLetterSourceQueues(a: ListDeadLetterSourceQueuesRequest): Kleisli[M, E, ListDeadLetterSourceQueuesResponse]           = Kleisli(e => eff1(f(e).listDeadLetterSourceQueues(a)))
        override def listDeadLetterSourceQueuesPaginator(a: ListDeadLetterSourceQueuesRequest): Kleisli[M, E, ListDeadLetterSourceQueuesPublisher] = Kleisli(e => primitive1(f(e).listDeadLetterSourceQueuesPaginator(a)))
        override def listMessageMoveTasks(a: ListMessageMoveTasksRequest): Kleisli[M, E, ListMessageMoveTasksResponse]                             = Kleisli(e => eff1(f(e).listMessageMoveTasks(a)))
        override def listQueueTags(a: ListQueueTagsRequest): Kleisli[M, E, ListQueueTagsResponse]                                                  = Kleisli(e => eff1(f(e).listQueueTags(a)))
        override def listQueues: Kleisli[M, E, ListQueuesResponse]                                                                                 = Kleisli(e => eff1(f(e).listQueues))
        override def listQueues(a: ListQueuesRequest): Kleisli[M, E, ListQueuesResponse]                                                           = Kleisli(e => eff1(f(e).listQueues(a)))
        override def listQueuesPaginator: Kleisli[M, E, ListQueuesPublisher]                                                                       = Kleisli(e => primitive1(f(e).listQueuesPaginator))
        override def listQueuesPaginator(a: ListQueuesRequest): Kleisli[M, E, ListQueuesPublisher]                                                 = Kleisli(e => primitive1(f(e).listQueuesPaginator(a)))
        override def purgeQueue(a: PurgeQueueRequest): Kleisli[M, E, PurgeQueueResponse]                                                           = Kleisli(e => eff1(f(e).purgeQueue(a)))
        override def receiveMessage(a: ReceiveMessageRequest): Kleisli[M, E, ReceiveMessageResponse]                                               = Kleisli(e => eff1(f(e).receiveMessage(a)))
        override def removePermission(a: RemovePermissionRequest): Kleisli[M, E, RemovePermissionResponse]                                         = Kleisli(e => eff1(f(e).removePermission(a)))
        override def sendMessage(a: SendMessageRequest): Kleisli[M, E, SendMessageResponse]                                                        = Kleisli(e => eff1(f(e).sendMessage(a)))
        override def sendMessageBatch(a: SendMessageBatchRequest): Kleisli[M, E, SendMessageBatchResponse]                                         = Kleisli(e => eff1(f(e).sendMessageBatch(a)))
        override def serviceName: Kleisli[M, E, String]                                                                                            = Kleisli(e => primitive1(f(e).serviceName))
        override def setQueueAttributes(a: SetQueueAttributesRequest): Kleisli[M, E, SetQueueAttributesResponse]                                   = Kleisli(e => eff1(f(e).setQueueAttributes(a)))
        override def startMessageMoveTask(a: StartMessageMoveTaskRequest): Kleisli[M, E, StartMessageMoveTaskResponse]                             = Kleisli(e => eff1(f(e).startMessageMoveTask(a)))
        override def tagQueue(a: TagQueueRequest): Kleisli[M, E, TagQueueResponse]                                                                 = Kleisli(e => eff1(f(e).tagQueue(a)))
        override def untagQueue(a: UntagQueueRequest): Kleisli[M, E, UntagQueueResponse]                                                           = Kleisli(e => eff1(f(e).untagQueue(a)))
      }
  }
  // end interpreters

  def SqsAsyncClientResource(builder: SqsAsyncClientBuilder): Resource[M, SqsAsyncClient]        = Resource.fromAutoCloseable(asyncM.delay(builder.build()))
  def SqsAsyncClientOpResource(builder: SqsAsyncClientBuilder): Resource[M, SqsAsyncClientOp[M]] = SqsAsyncClientResource(builder).map(create)

  def create(client: SqsAsyncClient): SqsAsyncClientOp[M] = new SqsAsyncClientOp[M] {

    // domain-specific operations are implemented in terms of `primitive`
    override def addPermission(a: AddPermissionRequest): M[AddPermissionResponse]                                                  = eff1(client.addPermission(a))
    override def batchManager: M[SqsAsyncBatchManager]                                                                             = primitive1(client.batchManager)
    override def cancelMessageMoveTask(a: CancelMessageMoveTaskRequest): M[CancelMessageMoveTaskResponse]                          = eff1(client.cancelMessageMoveTask(a))
    override def changeMessageVisibility(a: ChangeMessageVisibilityRequest): M[ChangeMessageVisibilityResponse]                    = eff1(client.changeMessageVisibility(a))
    override def changeMessageVisibilityBatch(a: ChangeMessageVisibilityBatchRequest): M[ChangeMessageVisibilityBatchResponse]     = eff1(client.changeMessageVisibilityBatch(a))
    override def close: M[Unit]                                                                                                    = primitive1(client.close)
    override def createQueue(a: CreateQueueRequest): M[CreateQueueResponse]                                                        = eff1(client.createQueue(a))
    override def deleteMessage(a: DeleteMessageRequest): M[DeleteMessageResponse]                                                  = eff1(client.deleteMessage(a))
    override def deleteMessageBatch(a: DeleteMessageBatchRequest): M[DeleteMessageBatchResponse]                                   = eff1(client.deleteMessageBatch(a))
    override def deleteQueue(a: DeleteQueueRequest): M[DeleteQueueResponse]                                                        = eff1(client.deleteQueue(a))
    override def getQueueAttributes(a: GetQueueAttributesRequest): M[GetQueueAttributesResponse]                                   = eff1(client.getQueueAttributes(a))
    override def getQueueUrl(a: GetQueueUrlRequest): M[GetQueueUrlResponse]                                                        = eff1(client.getQueueUrl(a))
    override def listDeadLetterSourceQueues(a: ListDeadLetterSourceQueuesRequest): M[ListDeadLetterSourceQueuesResponse]           = eff1(client.listDeadLetterSourceQueues(a))
    override def listDeadLetterSourceQueuesPaginator(a: ListDeadLetterSourceQueuesRequest): M[ListDeadLetterSourceQueuesPublisher] = primitive1(client.listDeadLetterSourceQueuesPaginator(a))
    override def listMessageMoveTasks(a: ListMessageMoveTasksRequest): M[ListMessageMoveTasksResponse]                             = eff1(client.listMessageMoveTasks(a))
    override def listQueueTags(a: ListQueueTagsRequest): M[ListQueueTagsResponse]                                                  = eff1(client.listQueueTags(a))
    override def listQueues: M[ListQueuesResponse]                                                                                 = eff1(client.listQueues)
    override def listQueues(a: ListQueuesRequest): M[ListQueuesResponse]                                                           = eff1(client.listQueues(a))
    override def listQueuesPaginator: M[ListQueuesPublisher]                                                                       = primitive1(client.listQueuesPaginator)
    override def listQueuesPaginator(a: ListQueuesRequest): M[ListQueuesPublisher]                                                 = primitive1(client.listQueuesPaginator(a))
    override def purgeQueue(a: PurgeQueueRequest): M[PurgeQueueResponse]                                                           = eff1(client.purgeQueue(a))
    override def receiveMessage(a: ReceiveMessageRequest): M[ReceiveMessageResponse]                                               = eff1(client.receiveMessage(a))
    override def removePermission(a: RemovePermissionRequest): M[RemovePermissionResponse]                                         = eff1(client.removePermission(a))
    override def sendMessage(a: SendMessageRequest): M[SendMessageResponse]                                                        = eff1(client.sendMessage(a))
    override def sendMessageBatch(a: SendMessageBatchRequest): M[SendMessageBatchResponse]                                         = eff1(client.sendMessageBatch(a))
    override def serviceName: M[String]                                                                                            = primitive1(client.serviceName)
    override def setQueueAttributes(a: SetQueueAttributesRequest): M[SetQueueAttributesResponse]                                   = eff1(client.setQueueAttributes(a))
    override def startMessageMoveTask(a: StartMessageMoveTaskRequest): M[StartMessageMoveTaskResponse]                             = eff1(client.startMessageMoveTask(a))
    override def tagQueue(a: TagQueueRequest): M[TagQueueResponse]                                                                 = eff1(client.tagQueue(a))
    override def untagQueue(a: UntagQueueRequest): M[UntagQueueResponse]                                                           = eff1(client.untagQueue(a))

  }

}
