package io.laserdisc.pure.sns.tagless

// Library imports
import cats.data.Kleisli
import cats.effect.{ Async,  Resource }

import software.amazon.awssdk.services.sns.*
import software.amazon.awssdk.services.sns.model.*

// Types referenced
import software.amazon.awssdk.services.sns.paginators.ListEndpointsByPlatformApplicationPublisher
import software.amazon.awssdk.services.sns.paginators.ListOriginationNumbersPublisher
import software.amazon.awssdk.services.sns.paginators.ListPhoneNumbersOptedOutPublisher
import software.amazon.awssdk.services.sns.paginators.ListPlatformApplicationsPublisher
import software.amazon.awssdk.services.sns.paginators.ListSMSSandboxPhoneNumbersPublisher
import software.amazon.awssdk.services.sns.paginators.ListSubscriptionsByTopicPublisher
import software.amazon.awssdk.services.sns.paginators.ListSubscriptionsPublisher
import software.amazon.awssdk.services.sns.paginators.ListTopicsPublisher


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

  lazy val SnsAsyncClientInterpreter: SnsAsyncClientInterpreter = new SnsAsyncClientInterpreter { }
  // Some methods are common to all interpreters and can be overridden to change behavior globally.

  def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(j => asyncM.blocking(f(j)))
  def primitive1[J, A](f: => A): M[A]              = asyncM.blocking(f)

  def eff[J, A](fut: J => CompletableFuture[A]): Kleisli[M, J, A] = Kleisli { j =>
    asyncM.fromCompletableFuture(asyncM.delay(fut(j)))
  }
  def eff1[J, A](fut: => CompletableFuture[A]): M[A] =
    asyncM.fromCompletableFuture(asyncM.delay(fut))

  // Interpreters
  trait SnsAsyncClientInterpreter extends SnsAsyncClientOp[Kleisli[M, SnsAsyncClient, *]] {
  
    // domain-specific operations are implemented in terms of `primitive`
    override def addPermission(a: AddPermissionRequest): Kleisli[M, SnsAsyncClient, AddPermissionResponse] = eff(_.addPermission(a)) // B
    override def checkIfPhoneNumberIsOptedOut(a: CheckIfPhoneNumberIsOptedOutRequest): Kleisli[M, SnsAsyncClient, CheckIfPhoneNumberIsOptedOutResponse] = eff(_.checkIfPhoneNumberIsOptedOut(a)) // B
    override def close : Kleisli[M, SnsAsyncClient, Unit] = primitive(_.close) // A
    override def confirmSubscription(a: ConfirmSubscriptionRequest): Kleisli[M, SnsAsyncClient, ConfirmSubscriptionResponse] = eff(_.confirmSubscription(a)) // B
    override def createPlatformApplication(a: CreatePlatformApplicationRequest): Kleisli[M, SnsAsyncClient, CreatePlatformApplicationResponse] = eff(_.createPlatformApplication(a)) // B
    override def createPlatformEndpoint(a: CreatePlatformEndpointRequest): Kleisli[M, SnsAsyncClient, CreatePlatformEndpointResponse] = eff(_.createPlatformEndpoint(a)) // B
    override def createSMSSandboxPhoneNumber(a: CreateSmsSandboxPhoneNumberRequest): Kleisli[M, SnsAsyncClient, CreateSmsSandboxPhoneNumberResponse] = eff(_.createSMSSandboxPhoneNumber(a)) // B
    override def createTopic(a: CreateTopicRequest): Kleisli[M, SnsAsyncClient, CreateTopicResponse] = eff(_.createTopic(a)) // B
    override def deleteEndpoint(a: DeleteEndpointRequest): Kleisli[M, SnsAsyncClient, DeleteEndpointResponse] = eff(_.deleteEndpoint(a)) // B
    override def deletePlatformApplication(a: DeletePlatformApplicationRequest): Kleisli[M, SnsAsyncClient, DeletePlatformApplicationResponse] = eff(_.deletePlatformApplication(a)) // B
    override def deleteSMSSandboxPhoneNumber(a: DeleteSmsSandboxPhoneNumberRequest): Kleisli[M, SnsAsyncClient, DeleteSmsSandboxPhoneNumberResponse] = eff(_.deleteSMSSandboxPhoneNumber(a)) // B
    override def deleteTopic(a: DeleteTopicRequest): Kleisli[M, SnsAsyncClient, DeleteTopicResponse] = eff(_.deleteTopic(a)) // B
    override def getDataProtectionPolicy(a: GetDataProtectionPolicyRequest): Kleisli[M, SnsAsyncClient, GetDataProtectionPolicyResponse] = eff(_.getDataProtectionPolicy(a)) // B
    override def getEndpointAttributes(a: GetEndpointAttributesRequest): Kleisli[M, SnsAsyncClient, GetEndpointAttributesResponse] = eff(_.getEndpointAttributes(a)) // B
    override def getPlatformApplicationAttributes(a: GetPlatformApplicationAttributesRequest): Kleisli[M, SnsAsyncClient, GetPlatformApplicationAttributesResponse] = eff(_.getPlatformApplicationAttributes(a)) // B
    override def getSMSAttributes : Kleisli[M, SnsAsyncClient, GetSmsAttributesResponse] = eff(_.getSMSAttributes) // A
    override def getSMSAttributes(a: GetSmsAttributesRequest): Kleisli[M, SnsAsyncClient, GetSmsAttributesResponse] = eff(_.getSMSAttributes(a)) // B
    override def getSMSSandboxAccountStatus(a: GetSmsSandboxAccountStatusRequest): Kleisli[M, SnsAsyncClient, GetSmsSandboxAccountStatusResponse] = eff(_.getSMSSandboxAccountStatus(a)) // B
    override def getSubscriptionAttributes(a: GetSubscriptionAttributesRequest): Kleisli[M, SnsAsyncClient, GetSubscriptionAttributesResponse] = eff(_.getSubscriptionAttributes(a)) // B
    override def getTopicAttributes(a: GetTopicAttributesRequest): Kleisli[M, SnsAsyncClient, GetTopicAttributesResponse] = eff(_.getTopicAttributes(a)) // B
    override def listEndpointsByPlatformApplication(a: ListEndpointsByPlatformApplicationRequest): Kleisli[M, SnsAsyncClient, ListEndpointsByPlatformApplicationResponse] = eff(_.listEndpointsByPlatformApplication(a)) // B
    override def listEndpointsByPlatformApplicationPaginator(a: ListEndpointsByPlatformApplicationRequest): Kleisli[M, SnsAsyncClient, ListEndpointsByPlatformApplicationPublisher] = primitive(_.listEndpointsByPlatformApplicationPaginator(a)) // B
    override def listOriginationNumbers(a: ListOriginationNumbersRequest): Kleisli[M, SnsAsyncClient, ListOriginationNumbersResponse] = eff(_.listOriginationNumbers(a)) // B
    override def listOriginationNumbersPaginator(a: ListOriginationNumbersRequest): Kleisli[M, SnsAsyncClient, ListOriginationNumbersPublisher] = primitive(_.listOriginationNumbersPaginator(a)) // B
    override def listPhoneNumbersOptedOut : Kleisli[M, SnsAsyncClient, ListPhoneNumbersOptedOutResponse] = eff(_.listPhoneNumbersOptedOut) // A
    override def listPhoneNumbersOptedOut(a: ListPhoneNumbersOptedOutRequest): Kleisli[M, SnsAsyncClient, ListPhoneNumbersOptedOutResponse] = eff(_.listPhoneNumbersOptedOut(a)) // B
    override def listPhoneNumbersOptedOutPaginator : Kleisli[M, SnsAsyncClient, ListPhoneNumbersOptedOutPublisher] = primitive(_.listPhoneNumbersOptedOutPaginator) // A
    override def listPhoneNumbersOptedOutPaginator(a: ListPhoneNumbersOptedOutRequest): Kleisli[M, SnsAsyncClient, ListPhoneNumbersOptedOutPublisher] = primitive(_.listPhoneNumbersOptedOutPaginator(a)) // B
    override def listPlatformApplications : Kleisli[M, SnsAsyncClient, ListPlatformApplicationsResponse] = eff(_.listPlatformApplications) // A
    override def listPlatformApplications(a: ListPlatformApplicationsRequest): Kleisli[M, SnsAsyncClient, ListPlatformApplicationsResponse] = eff(_.listPlatformApplications(a)) // B
    override def listPlatformApplicationsPaginator : Kleisli[M, SnsAsyncClient, ListPlatformApplicationsPublisher] = primitive(_.listPlatformApplicationsPaginator) // A
    override def listPlatformApplicationsPaginator(a: ListPlatformApplicationsRequest): Kleisli[M, SnsAsyncClient, ListPlatformApplicationsPublisher] = primitive(_.listPlatformApplicationsPaginator(a)) // B
    override def listSMSSandboxPhoneNumbers(a: ListSmsSandboxPhoneNumbersRequest): Kleisli[M, SnsAsyncClient, ListSmsSandboxPhoneNumbersResponse] = eff(_.listSMSSandboxPhoneNumbers(a)) // B
    override def listSMSSandboxPhoneNumbersPaginator(a: ListSmsSandboxPhoneNumbersRequest): Kleisli[M, SnsAsyncClient, ListSMSSandboxPhoneNumbersPublisher] = primitive(_.listSMSSandboxPhoneNumbersPaginator(a)) // B
    override def listSubscriptions : Kleisli[M, SnsAsyncClient, ListSubscriptionsResponse] = eff(_.listSubscriptions) // A
    override def listSubscriptions(a: ListSubscriptionsRequest): Kleisli[M, SnsAsyncClient, ListSubscriptionsResponse] = eff(_.listSubscriptions(a)) // B
    override def listSubscriptionsByTopic(a: ListSubscriptionsByTopicRequest): Kleisli[M, SnsAsyncClient, ListSubscriptionsByTopicResponse] = eff(_.listSubscriptionsByTopic(a)) // B
    override def listSubscriptionsByTopicPaginator(a: ListSubscriptionsByTopicRequest): Kleisli[M, SnsAsyncClient, ListSubscriptionsByTopicPublisher] = primitive(_.listSubscriptionsByTopicPaginator(a)) // B
    override def listSubscriptionsPaginator : Kleisli[M, SnsAsyncClient, ListSubscriptionsPublisher] = primitive(_.listSubscriptionsPaginator) // A
    override def listSubscriptionsPaginator(a: ListSubscriptionsRequest): Kleisli[M, SnsAsyncClient, ListSubscriptionsPublisher] = primitive(_.listSubscriptionsPaginator(a)) // B
    override def listTagsForResource(a: ListTagsForResourceRequest): Kleisli[M, SnsAsyncClient, ListTagsForResourceResponse] = eff(_.listTagsForResource(a)) // B
    override def listTopics : Kleisli[M, SnsAsyncClient, ListTopicsResponse] = eff(_.listTopics) // A
    override def listTopics(a: ListTopicsRequest): Kleisli[M, SnsAsyncClient, ListTopicsResponse] = eff(_.listTopics(a)) // B
    override def listTopicsPaginator : Kleisli[M, SnsAsyncClient, ListTopicsPublisher] = primitive(_.listTopicsPaginator) // A
    override def listTopicsPaginator(a: ListTopicsRequest): Kleisli[M, SnsAsyncClient, ListTopicsPublisher] = primitive(_.listTopicsPaginator(a)) // B
    override def optInPhoneNumber(a: OptInPhoneNumberRequest): Kleisli[M, SnsAsyncClient, OptInPhoneNumberResponse] = eff(_.optInPhoneNumber(a)) // B
    override def publish(a: PublishRequest): Kleisli[M, SnsAsyncClient, PublishResponse] = eff(_.publish(a)) // B
    override def publishBatch(a: PublishBatchRequest): Kleisli[M, SnsAsyncClient, PublishBatchResponse] = eff(_.publishBatch(a)) // B
    override def putDataProtectionPolicy(a: PutDataProtectionPolicyRequest): Kleisli[M, SnsAsyncClient, PutDataProtectionPolicyResponse] = eff(_.putDataProtectionPolicy(a)) // B
    override def removePermission(a: RemovePermissionRequest): Kleisli[M, SnsAsyncClient, RemovePermissionResponse] = eff(_.removePermission(a)) // B
    override def serviceName : Kleisli[M, SnsAsyncClient, String] = primitive(_.serviceName) // A
    override def setEndpointAttributes(a: SetEndpointAttributesRequest): Kleisli[M, SnsAsyncClient, SetEndpointAttributesResponse] = eff(_.setEndpointAttributes(a)) // B
    override def setPlatformApplicationAttributes(a: SetPlatformApplicationAttributesRequest): Kleisli[M, SnsAsyncClient, SetPlatformApplicationAttributesResponse] = eff(_.setPlatformApplicationAttributes(a)) // B
    override def setSMSAttributes(a: SetSmsAttributesRequest): Kleisli[M, SnsAsyncClient, SetSmsAttributesResponse] = eff(_.setSMSAttributes(a)) // B
    override def setSubscriptionAttributes(a: SetSubscriptionAttributesRequest): Kleisli[M, SnsAsyncClient, SetSubscriptionAttributesResponse] = eff(_.setSubscriptionAttributes(a)) // B
    override def setTopicAttributes(a: SetTopicAttributesRequest): Kleisli[M, SnsAsyncClient, SetTopicAttributesResponse] = eff(_.setTopicAttributes(a)) // B
    override def subscribe(a: SubscribeRequest): Kleisli[M, SnsAsyncClient, SubscribeResponse] = eff(_.subscribe(a)) // B
    override def tagResource(a: TagResourceRequest): Kleisli[M, SnsAsyncClient, TagResourceResponse] = eff(_.tagResource(a)) // B
    override def unsubscribe(a: UnsubscribeRequest): Kleisli[M, SnsAsyncClient, UnsubscribeResponse] = eff(_.unsubscribe(a)) // B
    override def untagResource(a: UntagResourceRequest): Kleisli[M, SnsAsyncClient, UntagResourceResponse] = eff(_.untagResource(a)) // B
    override def verifySMSSandboxPhoneNumber(a: VerifySmsSandboxPhoneNumberRequest): Kleisli[M, SnsAsyncClient, VerifySmsSandboxPhoneNumberResponse] = eff(_.verifySMSSandboxPhoneNumber(a)) // B
  
  
    def lens[E](f: E => SnsAsyncClient): SnsAsyncClientOp[Kleisli[M, E, *]] =
      new SnsAsyncClientOp[Kleisli[M, E, *]] {
      override def addPermission(a: AddPermissionRequest) = Kleisli(e => eff1(f(e).addPermission(a)))
      override def checkIfPhoneNumberIsOptedOut(a: CheckIfPhoneNumberIsOptedOutRequest) = Kleisli(e => eff1(f(e).checkIfPhoneNumberIsOptedOut(a)))
      override def close = Kleisli(e => primitive1(f(e).close))
      override def confirmSubscription(a: ConfirmSubscriptionRequest) = Kleisli(e => eff1(f(e).confirmSubscription(a)))
      override def createPlatformApplication(a: CreatePlatformApplicationRequest) = Kleisli(e => eff1(f(e).createPlatformApplication(a)))
      override def createPlatformEndpoint(a: CreatePlatformEndpointRequest) = Kleisli(e => eff1(f(e).createPlatformEndpoint(a)))
      override def createSMSSandboxPhoneNumber(a: CreateSmsSandboxPhoneNumberRequest) = Kleisli(e => eff1(f(e).createSMSSandboxPhoneNumber(a)))
      override def createTopic(a: CreateTopicRequest) = Kleisli(e => eff1(f(e).createTopic(a)))
      override def deleteEndpoint(a: DeleteEndpointRequest) = Kleisli(e => eff1(f(e).deleteEndpoint(a)))
      override def deletePlatformApplication(a: DeletePlatformApplicationRequest) = Kleisli(e => eff1(f(e).deletePlatformApplication(a)))
      override def deleteSMSSandboxPhoneNumber(a: DeleteSmsSandboxPhoneNumberRequest) = Kleisli(e => eff1(f(e).deleteSMSSandboxPhoneNumber(a)))
      override def deleteTopic(a: DeleteTopicRequest) = Kleisli(e => eff1(f(e).deleteTopic(a)))
      override def getDataProtectionPolicy(a: GetDataProtectionPolicyRequest) = Kleisli(e => eff1(f(e).getDataProtectionPolicy(a)))
      override def getEndpointAttributes(a: GetEndpointAttributesRequest) = Kleisli(e => eff1(f(e).getEndpointAttributes(a)))
      override def getPlatformApplicationAttributes(a: GetPlatformApplicationAttributesRequest) = Kleisli(e => eff1(f(e).getPlatformApplicationAttributes(a)))
      override def getSMSAttributes = Kleisli(e => eff1(f(e).getSMSAttributes))
      override def getSMSAttributes(a: GetSmsAttributesRequest) = Kleisli(e => eff1(f(e).getSMSAttributes(a)))
      override def getSMSSandboxAccountStatus(a: GetSmsSandboxAccountStatusRequest) = Kleisli(e => eff1(f(e).getSMSSandboxAccountStatus(a)))
      override def getSubscriptionAttributes(a: GetSubscriptionAttributesRequest) = Kleisli(e => eff1(f(e).getSubscriptionAttributes(a)))
      override def getTopicAttributes(a: GetTopicAttributesRequest) = Kleisli(e => eff1(f(e).getTopicAttributes(a)))
      override def listEndpointsByPlatformApplication(a: ListEndpointsByPlatformApplicationRequest) = Kleisli(e => eff1(f(e).listEndpointsByPlatformApplication(a)))
      override def listEndpointsByPlatformApplicationPaginator(a: ListEndpointsByPlatformApplicationRequest) = Kleisli(e => primitive1(f(e).listEndpointsByPlatformApplicationPaginator(a)))
      override def listOriginationNumbers(a: ListOriginationNumbersRequest) = Kleisli(e => eff1(f(e).listOriginationNumbers(a)))
      override def listOriginationNumbersPaginator(a: ListOriginationNumbersRequest) = Kleisli(e => primitive1(f(e).listOriginationNumbersPaginator(a)))
      override def listPhoneNumbersOptedOut = Kleisli(e => eff1(f(e).listPhoneNumbersOptedOut))
      override def listPhoneNumbersOptedOut(a: ListPhoneNumbersOptedOutRequest) = Kleisli(e => eff1(f(e).listPhoneNumbersOptedOut(a)))
      override def listPhoneNumbersOptedOutPaginator = Kleisli(e => primitive1(f(e).listPhoneNumbersOptedOutPaginator))
      override def listPhoneNumbersOptedOutPaginator(a: ListPhoneNumbersOptedOutRequest) = Kleisli(e => primitive1(f(e).listPhoneNumbersOptedOutPaginator(a)))
      override def listPlatformApplications = Kleisli(e => eff1(f(e).listPlatformApplications))
      override def listPlatformApplications(a: ListPlatformApplicationsRequest) = Kleisli(e => eff1(f(e).listPlatformApplications(a)))
      override def listPlatformApplicationsPaginator = Kleisli(e => primitive1(f(e).listPlatformApplicationsPaginator))
      override def listPlatformApplicationsPaginator(a: ListPlatformApplicationsRequest) = Kleisli(e => primitive1(f(e).listPlatformApplicationsPaginator(a)))
      override def listSMSSandboxPhoneNumbers(a: ListSmsSandboxPhoneNumbersRequest) = Kleisli(e => eff1(f(e).listSMSSandboxPhoneNumbers(a)))
      override def listSMSSandboxPhoneNumbersPaginator(a: ListSmsSandboxPhoneNumbersRequest) = Kleisli(e => primitive1(f(e).listSMSSandboxPhoneNumbersPaginator(a)))
      override def listSubscriptions = Kleisli(e => eff1(f(e).listSubscriptions))
      override def listSubscriptions(a: ListSubscriptionsRequest) = Kleisli(e => eff1(f(e).listSubscriptions(a)))
      override def listSubscriptionsByTopic(a: ListSubscriptionsByTopicRequest) = Kleisli(e => eff1(f(e).listSubscriptionsByTopic(a)))
      override def listSubscriptionsByTopicPaginator(a: ListSubscriptionsByTopicRequest) = Kleisli(e => primitive1(f(e).listSubscriptionsByTopicPaginator(a)))
      override def listSubscriptionsPaginator = Kleisli(e => primitive1(f(e).listSubscriptionsPaginator))
      override def listSubscriptionsPaginator(a: ListSubscriptionsRequest) = Kleisli(e => primitive1(f(e).listSubscriptionsPaginator(a)))
      override def listTagsForResource(a: ListTagsForResourceRequest) = Kleisli(e => eff1(f(e).listTagsForResource(a)))
      override def listTopics = Kleisli(e => eff1(f(e).listTopics))
      override def listTopics(a: ListTopicsRequest) = Kleisli(e => eff1(f(e).listTopics(a)))
      override def listTopicsPaginator = Kleisli(e => primitive1(f(e).listTopicsPaginator))
      override def listTopicsPaginator(a: ListTopicsRequest) = Kleisli(e => primitive1(f(e).listTopicsPaginator(a)))
      override def optInPhoneNumber(a: OptInPhoneNumberRequest) = Kleisli(e => eff1(f(e).optInPhoneNumber(a)))
      override def publish(a: PublishRequest) = Kleisli(e => eff1(f(e).publish(a)))
      override def publishBatch(a: PublishBatchRequest) = Kleisli(e => eff1(f(e).publishBatch(a)))
      override def putDataProtectionPolicy(a: PutDataProtectionPolicyRequest) = Kleisli(e => eff1(f(e).putDataProtectionPolicy(a)))
      override def removePermission(a: RemovePermissionRequest) = Kleisli(e => eff1(f(e).removePermission(a)))
      override def serviceName = Kleisli(e => primitive1(f(e).serviceName))
      override def setEndpointAttributes(a: SetEndpointAttributesRequest) = Kleisli(e => eff1(f(e).setEndpointAttributes(a)))
      override def setPlatformApplicationAttributes(a: SetPlatformApplicationAttributesRequest) = Kleisli(e => eff1(f(e).setPlatformApplicationAttributes(a)))
      override def setSMSAttributes(a: SetSmsAttributesRequest) = Kleisli(e => eff1(f(e).setSMSAttributes(a)))
      override def setSubscriptionAttributes(a: SetSubscriptionAttributesRequest) = Kleisli(e => eff1(f(e).setSubscriptionAttributes(a)))
      override def setTopicAttributes(a: SetTopicAttributesRequest) = Kleisli(e => eff1(f(e).setTopicAttributes(a)))
      override def subscribe(a: SubscribeRequest) = Kleisli(e => eff1(f(e).subscribe(a)))
      override def tagResource(a: TagResourceRequest) = Kleisli(e => eff1(f(e).tagResource(a)))
      override def unsubscribe(a: UnsubscribeRequest) = Kleisli(e => eff1(f(e).unsubscribe(a)))
      override def untagResource(a: UntagResourceRequest) = Kleisli(e => eff1(f(e).untagResource(a)))
      override def verifySMSSandboxPhoneNumber(a: VerifySmsSandboxPhoneNumberRequest) = Kleisli(e => eff1(f(e).verifySMSSandboxPhoneNumber(a)))
  
      }
    }

  // end interpreters

  def SnsAsyncClientResource(builder : SnsAsyncClientBuilder) : Resource[M, SnsAsyncClient] = Resource.fromAutoCloseable(asyncM.delay(builder.build()))
  def SnsAsyncClientOpResource(builder: SnsAsyncClientBuilder) = SnsAsyncClientResource(builder).map(create)

  def create(client : SnsAsyncClient) : SnsAsyncClientOp[M] = new SnsAsyncClientOp[M] {

    // domain-specific operations are implemented in terms of `primitive`
    override def addPermission(a: AddPermissionRequest) = eff1(client.addPermission(a))
    override def checkIfPhoneNumberIsOptedOut(a: CheckIfPhoneNumberIsOptedOutRequest) = eff1(client.checkIfPhoneNumberIsOptedOut(a))
    override def close = primitive1(client.close)
    override def confirmSubscription(a: ConfirmSubscriptionRequest) = eff1(client.confirmSubscription(a))
    override def createPlatformApplication(a: CreatePlatformApplicationRequest) = eff1(client.createPlatformApplication(a))
    override def createPlatformEndpoint(a: CreatePlatformEndpointRequest) = eff1(client.createPlatformEndpoint(a))
    override def createSMSSandboxPhoneNumber(a: CreateSmsSandboxPhoneNumberRequest) = eff1(client.createSMSSandboxPhoneNumber(a))
    override def createTopic(a: CreateTopicRequest) = eff1(client.createTopic(a))
    override def deleteEndpoint(a: DeleteEndpointRequest) = eff1(client.deleteEndpoint(a))
    override def deletePlatformApplication(a: DeletePlatformApplicationRequest) = eff1(client.deletePlatformApplication(a))
    override def deleteSMSSandboxPhoneNumber(a: DeleteSmsSandboxPhoneNumberRequest) = eff1(client.deleteSMSSandboxPhoneNumber(a))
    override def deleteTopic(a: DeleteTopicRequest) = eff1(client.deleteTopic(a))
    override def getDataProtectionPolicy(a: GetDataProtectionPolicyRequest) = eff1(client.getDataProtectionPolicy(a))
    override def getEndpointAttributes(a: GetEndpointAttributesRequest) = eff1(client.getEndpointAttributes(a))
    override def getPlatformApplicationAttributes(a: GetPlatformApplicationAttributesRequest) = eff1(client.getPlatformApplicationAttributes(a))
    override def getSMSAttributes = eff1(client.getSMSAttributes)
    override def getSMSAttributes(a: GetSmsAttributesRequest) = eff1(client.getSMSAttributes(a))
    override def getSMSSandboxAccountStatus(a: GetSmsSandboxAccountStatusRequest) = eff1(client.getSMSSandboxAccountStatus(a))
    override def getSubscriptionAttributes(a: GetSubscriptionAttributesRequest) = eff1(client.getSubscriptionAttributes(a))
    override def getTopicAttributes(a: GetTopicAttributesRequest) = eff1(client.getTopicAttributes(a))
    override def listEndpointsByPlatformApplication(a: ListEndpointsByPlatformApplicationRequest) = eff1(client.listEndpointsByPlatformApplication(a))
    override def listEndpointsByPlatformApplicationPaginator(a: ListEndpointsByPlatformApplicationRequest) = primitive1(client.listEndpointsByPlatformApplicationPaginator(a))
    override def listOriginationNumbers(a: ListOriginationNumbersRequest) = eff1(client.listOriginationNumbers(a))
    override def listOriginationNumbersPaginator(a: ListOriginationNumbersRequest) = primitive1(client.listOriginationNumbersPaginator(a))
    override def listPhoneNumbersOptedOut = eff1(client.listPhoneNumbersOptedOut)
    override def listPhoneNumbersOptedOut(a: ListPhoneNumbersOptedOutRequest) = eff1(client.listPhoneNumbersOptedOut(a))
    override def listPhoneNumbersOptedOutPaginator = primitive1(client.listPhoneNumbersOptedOutPaginator)
    override def listPhoneNumbersOptedOutPaginator(a: ListPhoneNumbersOptedOutRequest) = primitive1(client.listPhoneNumbersOptedOutPaginator(a))
    override def listPlatformApplications = eff1(client.listPlatformApplications)
    override def listPlatformApplications(a: ListPlatformApplicationsRequest) = eff1(client.listPlatformApplications(a))
    override def listPlatformApplicationsPaginator = primitive1(client.listPlatformApplicationsPaginator)
    override def listPlatformApplicationsPaginator(a: ListPlatformApplicationsRequest) = primitive1(client.listPlatformApplicationsPaginator(a))
    override def listSMSSandboxPhoneNumbers(a: ListSmsSandboxPhoneNumbersRequest) = eff1(client.listSMSSandboxPhoneNumbers(a))
    override def listSMSSandboxPhoneNumbersPaginator(a: ListSmsSandboxPhoneNumbersRequest) = primitive1(client.listSMSSandboxPhoneNumbersPaginator(a))
    override def listSubscriptions = eff1(client.listSubscriptions)
    override def listSubscriptions(a: ListSubscriptionsRequest) = eff1(client.listSubscriptions(a))
    override def listSubscriptionsByTopic(a: ListSubscriptionsByTopicRequest) = eff1(client.listSubscriptionsByTopic(a))
    override def listSubscriptionsByTopicPaginator(a: ListSubscriptionsByTopicRequest) = primitive1(client.listSubscriptionsByTopicPaginator(a))
    override def listSubscriptionsPaginator = primitive1(client.listSubscriptionsPaginator)
    override def listSubscriptionsPaginator(a: ListSubscriptionsRequest) = primitive1(client.listSubscriptionsPaginator(a))
    override def listTagsForResource(a: ListTagsForResourceRequest) = eff1(client.listTagsForResource(a))
    override def listTopics = eff1(client.listTopics)
    override def listTopics(a: ListTopicsRequest) = eff1(client.listTopics(a))
    override def listTopicsPaginator = primitive1(client.listTopicsPaginator)
    override def listTopicsPaginator(a: ListTopicsRequest) = primitive1(client.listTopicsPaginator(a))
    override def optInPhoneNumber(a: OptInPhoneNumberRequest) = eff1(client.optInPhoneNumber(a))
    override def publish(a: PublishRequest) = eff1(client.publish(a))
    override def publishBatch(a: PublishBatchRequest) = eff1(client.publishBatch(a))
    override def putDataProtectionPolicy(a: PutDataProtectionPolicyRequest) = eff1(client.putDataProtectionPolicy(a))
    override def removePermission(a: RemovePermissionRequest) = eff1(client.removePermission(a))
    override def serviceName = primitive1(client.serviceName)
    override def setEndpointAttributes(a: SetEndpointAttributesRequest) = eff1(client.setEndpointAttributes(a))
    override def setPlatformApplicationAttributes(a: SetPlatformApplicationAttributesRequest) = eff1(client.setPlatformApplicationAttributes(a))
    override def setSMSAttributes(a: SetSmsAttributesRequest) = eff1(client.setSMSAttributes(a))
    override def setSubscriptionAttributes(a: SetSubscriptionAttributesRequest) = eff1(client.setSubscriptionAttributes(a))
    override def setTopicAttributes(a: SetTopicAttributesRequest) = eff1(client.setTopicAttributes(a))
    override def subscribe(a: SubscribeRequest) = eff1(client.subscribe(a))
    override def tagResource(a: TagResourceRequest) = eff1(client.tagResource(a))
    override def unsubscribe(a: UnsubscribeRequest) = eff1(client.unsubscribe(a))
    override def untagResource(a: UntagResourceRequest) = eff1(client.untagResource(a))
    override def verifySMSSandboxPhoneNumber(a: VerifySmsSandboxPhoneNumberRequest) = eff1(client.verifySMSSandboxPhoneNumber(a))


  }


}

