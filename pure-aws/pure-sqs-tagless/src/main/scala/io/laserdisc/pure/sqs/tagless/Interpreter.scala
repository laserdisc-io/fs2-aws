package io.laserdisc.pure.sqs.tagless

// Library imports
import cats.data.Kleisli
import cats.effect.{ Async, Blocker, ContextShift }

import java.util.concurrent.CompletionException

// Types referenced
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model._

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

  lazy val SqsAsyncClientInterpreter: SqsAsyncClientInterpreter = new SqsAsyncClientInterpreter {}
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
  trait SqsAsyncClientInterpreter extends SqsAsyncClientOp[Kleisli[M, SqsAsyncClient, *]] {

    // domain-specific operations are implemented in terms of `primitive`
    override def addPermission(a: AddPermissionRequest) = eff(_.addPermission(a))
    override def changeMessageVisibility(a: ChangeMessageVisibilityRequest) =
      eff(_.changeMessageVisibility(a))
    override def changeMessageVisibilityBatch(a: ChangeMessageVisibilityBatchRequest) =
      eff(_.changeMessageVisibilityBatch(a))
    override def close                                            = primitive(_.close)
    override def createQueue(a: CreateQueueRequest)               = eff(_.createQueue(a))
    override def deleteMessage(a: DeleteMessageRequest)           = eff(_.deleteMessage(a))
    override def deleteMessageBatch(a: DeleteMessageBatchRequest) = eff(_.deleteMessageBatch(a))
    override def deleteQueue(a: DeleteQueueRequest)               = eff(_.deleteQueue(a))
    override def getQueueAttributes(a: GetQueueAttributesRequest) = eff(_.getQueueAttributes(a))
    override def getQueueUrl(a: GetQueueUrlRequest)               = eff(_.getQueueUrl(a))
    override def listDeadLetterSourceQueues(a: ListDeadLetterSourceQueuesRequest) =
      eff(_.listDeadLetterSourceQueues(a))
    override def listDeadLetterSourceQueuesPaginator(a: ListDeadLetterSourceQueuesRequest) =
      primitive(_.listDeadLetterSourceQueuesPaginator(a))
    override def listQueueTags(a: ListQueueTagsRequest)           = eff(_.listQueueTags(a))
    override def listQueues                                       = eff(_.listQueues)
    override def listQueues(a: ListQueuesRequest)                 = eff(_.listQueues(a))
    override def listQueuesPaginator                              = primitive(_.listQueuesPaginator)
    override def listQueuesPaginator(a: ListQueuesRequest)        = primitive(_.listQueuesPaginator(a))
    override def purgeQueue(a: PurgeQueueRequest)                 = eff(_.purgeQueue(a))
    override def receiveMessage(a: ReceiveMessageRequest)         = eff(_.receiveMessage(a))
    override def removePermission(a: RemovePermissionRequest)     = eff(_.removePermission(a))
    override def sendMessage(a: SendMessageRequest)               = eff(_.sendMessage(a))
    override def sendMessageBatch(a: SendMessageBatchRequest)     = eff(_.sendMessageBatch(a))
    override def serviceName                                      = primitive(_.serviceName)
    override def setQueueAttributes(a: SetQueueAttributesRequest) = eff(_.setQueueAttributes(a))
    override def tagQueue(a: TagQueueRequest)                     = eff(_.tagQueue(a))
    override def untagQueue(a: UntagQueueRequest)                 = eff(_.untagQueue(a))

  }

  def create(client: SqsAsyncClient): SqsAsyncClientOp[M] = new SqsAsyncClientOp[M] {

    // domain-specific operations are implemented in terms of `primitive`
    override def addPermission(a: AddPermissionRequest) = eff1(client.addPermission(a))
    override def changeMessageVisibility(a: ChangeMessageVisibilityRequest) =
      eff1(client.changeMessageVisibility(a))
    override def changeMessageVisibilityBatch(a: ChangeMessageVisibilityBatchRequest) =
      eff1(client.changeMessageVisibilityBatch(a))
    override def close                                  = primitive1(client.close)
    override def createQueue(a: CreateQueueRequest)     = eff1(client.createQueue(a))
    override def deleteMessage(a: DeleteMessageRequest) = eff1(client.deleteMessage(a))
    override def deleteMessageBatch(a: DeleteMessageBatchRequest) =
      eff1(client.deleteMessageBatch(a))
    override def deleteQueue(a: DeleteQueueRequest) = eff1(client.deleteQueue(a))
    override def getQueueAttributes(a: GetQueueAttributesRequest) =
      eff1(client.getQueueAttributes(a))
    override def getQueueUrl(a: GetQueueUrlRequest) = eff1(client.getQueueUrl(a))
    override def listDeadLetterSourceQueues(a: ListDeadLetterSourceQueuesRequest) =
      eff1(client.listDeadLetterSourceQueues(a))
    override def listDeadLetterSourceQueuesPaginator(a: ListDeadLetterSourceQueuesRequest) =
      primitive1(client.listDeadLetterSourceQueuesPaginator(a))
    override def listQueueTags(a: ListQueueTagsRequest) = eff1(client.listQueueTags(a))
    override def listQueues                             = eff1(client.listQueues)
    override def listQueues(a: ListQueuesRequest)       = eff1(client.listQueues(a))
    override def listQueuesPaginator                    = primitive1(client.listQueuesPaginator)
    override def listQueuesPaginator(a: ListQueuesRequest) =
      primitive1(client.listQueuesPaginator(a))
    override def purgeQueue(a: PurgeQueueRequest)             = eff1(client.purgeQueue(a))
    override def receiveMessage(a: ReceiveMessageRequest)     = eff1(client.receiveMessage(a))
    override def removePermission(a: RemovePermissionRequest) = eff1(client.removePermission(a))
    override def sendMessage(a: SendMessageRequest)           = eff1(client.sendMessage(a))
    override def sendMessageBatch(a: SendMessageBatchRequest) = eff1(client.sendMessageBatch(a))
    override def serviceName                                  = primitive1(client.serviceName)
    override def setQueueAttributes(a: SetQueueAttributesRequest) =
      eff1(client.setQueueAttributes(a))
    override def tagQueue(a: TagQueueRequest)     = eff1(client.tagQueue(a))
    override def untagQueue(a: UntagQueueRequest) = eff1(client.untagQueue(a))

  }

}
