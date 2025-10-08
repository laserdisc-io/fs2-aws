package io.laserdisc.pure.sns.tagless

// Library imports
import cats.data.Kleisli
import cats.effect.{Async, Resource}

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

  lazy val SnsAsyncClientInterpreter: SnsAsyncClientInterpreter = new SnsAsyncClientInterpreter {}
  // Some methods are common to all interpreters and can be overridden to change behavior globally.

  private def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(j => asyncM.blocking(f(j)))
  private def primitive1[J, A](f: => A): M[A]              = asyncM.blocking(f)

  private def eff[J, A](fut: J => CompletableFuture[A]): Kleisli[M, J, A] = Kleisli { j =>
    asyncM.fromCompletableFuture(asyncM.delay(fut(j)))
  }
  private def eff1[J, A](fut: => CompletableFuture[A]): M[A] =
    asyncM.fromCompletableFuture(asyncM.delay(fut))

  // Interpreters // scalafmt: off
  trait SnsAsyncClientInterpreter extends SnsAsyncClientOp[Kleisli[M, SnsAsyncClient, *]] {

    // domain-specific operations are implemented in terms of `primitive`
    override def addPermission(a: AddPermissionRequest): Kleisli[M, SnsAsyncClient, AddPermissionResponse]                                                                          = eff(_.addPermission(a))
    override def checkIfPhoneNumberIsOptedOut(a: CheckIfPhoneNumberIsOptedOutRequest): Kleisli[M, SnsAsyncClient, CheckIfPhoneNumberIsOptedOutResponse]                             = eff(_.checkIfPhoneNumberIsOptedOut(a))
    override def close: Kleisli[M, SnsAsyncClient, Unit]                                                                                                                            = primitive(_.close)
    override def confirmSubscription(a: ConfirmSubscriptionRequest): Kleisli[M, SnsAsyncClient, ConfirmSubscriptionResponse]                                                        = eff(_.confirmSubscription(a))
    override def createPlatformApplication(a: CreatePlatformApplicationRequest): Kleisli[M, SnsAsyncClient, CreatePlatformApplicationResponse]                                      = eff(_.createPlatformApplication(a))
    override def createPlatformEndpoint(a: CreatePlatformEndpointRequest): Kleisli[M, SnsAsyncClient, CreatePlatformEndpointResponse]                                               = eff(_.createPlatformEndpoint(a))
    override def createSMSSandboxPhoneNumber(a: CreateSmsSandboxPhoneNumberRequest): Kleisli[M, SnsAsyncClient, CreateSmsSandboxPhoneNumberResponse]                                = eff(_.createSMSSandboxPhoneNumber(a))
    override def createTopic(a: CreateTopicRequest): Kleisli[M, SnsAsyncClient, CreateTopicResponse]                                                                                = eff(_.createTopic(a))
    override def deleteEndpoint(a: DeleteEndpointRequest): Kleisli[M, SnsAsyncClient, DeleteEndpointResponse]                                                                       = eff(_.deleteEndpoint(a))
    override def deletePlatformApplication(a: DeletePlatformApplicationRequest): Kleisli[M, SnsAsyncClient, DeletePlatformApplicationResponse]                                      = eff(_.deletePlatformApplication(a))
    override def deleteSMSSandboxPhoneNumber(a: DeleteSmsSandboxPhoneNumberRequest): Kleisli[M, SnsAsyncClient, DeleteSmsSandboxPhoneNumberResponse]                                = eff(_.deleteSMSSandboxPhoneNumber(a))
    override def deleteTopic(a: DeleteTopicRequest): Kleisli[M, SnsAsyncClient, DeleteTopicResponse]                                                                                = eff(_.deleteTopic(a))
    override def getDataProtectionPolicy(a: GetDataProtectionPolicyRequest): Kleisli[M, SnsAsyncClient, GetDataProtectionPolicyResponse]                                            = eff(_.getDataProtectionPolicy(a))
    override def getEndpointAttributes(a: GetEndpointAttributesRequest): Kleisli[M, SnsAsyncClient, GetEndpointAttributesResponse]                                                  = eff(_.getEndpointAttributes(a))
    override def getPlatformApplicationAttributes(a: GetPlatformApplicationAttributesRequest): Kleisli[M, SnsAsyncClient, GetPlatformApplicationAttributesResponse]                 = eff(_.getPlatformApplicationAttributes(a))
    override def getSMSAttributes: Kleisli[M, SnsAsyncClient, GetSmsAttributesResponse]                                                                                             = eff(_.getSMSAttributes)
    override def getSMSAttributes(a: GetSmsAttributesRequest): Kleisli[M, SnsAsyncClient, GetSmsAttributesResponse]                                                                 = eff(_.getSMSAttributes(a))
    override def getSMSSandboxAccountStatus(a: GetSmsSandboxAccountStatusRequest): Kleisli[M, SnsAsyncClient, GetSmsSandboxAccountStatusResponse]                                   = eff(_.getSMSSandboxAccountStatus(a))
    override def getSubscriptionAttributes(a: GetSubscriptionAttributesRequest): Kleisli[M, SnsAsyncClient, GetSubscriptionAttributesResponse]                                      = eff(_.getSubscriptionAttributes(a))
    override def getTopicAttributes(a: GetTopicAttributesRequest): Kleisli[M, SnsAsyncClient, GetTopicAttributesResponse]                                                           = eff(_.getTopicAttributes(a))
    override def listEndpointsByPlatformApplication(a: ListEndpointsByPlatformApplicationRequest): Kleisli[M, SnsAsyncClient, ListEndpointsByPlatformApplicationResponse]           = eff(_.listEndpointsByPlatformApplication(a))
    override def listEndpointsByPlatformApplicationPaginator(a: ListEndpointsByPlatformApplicationRequest): Kleisli[M, SnsAsyncClient, ListEndpointsByPlatformApplicationPublisher] = primitive(_.listEndpointsByPlatformApplicationPaginator(a))
    override def listOriginationNumbers(a: ListOriginationNumbersRequest): Kleisli[M, SnsAsyncClient, ListOriginationNumbersResponse]                                               = eff(_.listOriginationNumbers(a))
    override def listOriginationNumbersPaginator(a: ListOriginationNumbersRequest): Kleisli[M, SnsAsyncClient, ListOriginationNumbersPublisher]                                     = primitive(_.listOriginationNumbersPaginator(a))
    override def listPhoneNumbersOptedOut: Kleisli[M, SnsAsyncClient, ListPhoneNumbersOptedOutResponse]                                                                             = eff(_.listPhoneNumbersOptedOut)
    override def listPhoneNumbersOptedOut(a: ListPhoneNumbersOptedOutRequest): Kleisli[M, SnsAsyncClient, ListPhoneNumbersOptedOutResponse]                                         = eff(_.listPhoneNumbersOptedOut(a))
    override def listPhoneNumbersOptedOutPaginator: Kleisli[M, SnsAsyncClient, ListPhoneNumbersOptedOutPublisher]                                                                   = primitive(_.listPhoneNumbersOptedOutPaginator)
    override def listPhoneNumbersOptedOutPaginator(a: ListPhoneNumbersOptedOutRequest): Kleisli[M, SnsAsyncClient, ListPhoneNumbersOptedOutPublisher]                               = primitive(_.listPhoneNumbersOptedOutPaginator(a))
    override def listPlatformApplications: Kleisli[M, SnsAsyncClient, ListPlatformApplicationsResponse]                                                                             = eff(_.listPlatformApplications)
    override def listPlatformApplications(a: ListPlatformApplicationsRequest): Kleisli[M, SnsAsyncClient, ListPlatformApplicationsResponse]                                         = eff(_.listPlatformApplications(a))
    override def listPlatformApplicationsPaginator: Kleisli[M, SnsAsyncClient, ListPlatformApplicationsPublisher]                                                                   = primitive(_.listPlatformApplicationsPaginator)
    override def listPlatformApplicationsPaginator(a: ListPlatformApplicationsRequest): Kleisli[M, SnsAsyncClient, ListPlatformApplicationsPublisher]                               = primitive(_.listPlatformApplicationsPaginator(a))
    override def listSMSSandboxPhoneNumbers(a: ListSmsSandboxPhoneNumbersRequest): Kleisli[M, SnsAsyncClient, ListSmsSandboxPhoneNumbersResponse]                                   = eff(_.listSMSSandboxPhoneNumbers(a))
    override def listSMSSandboxPhoneNumbersPaginator(a: ListSmsSandboxPhoneNumbersRequest): Kleisli[M, SnsAsyncClient, ListSMSSandboxPhoneNumbersPublisher]                         = primitive(_.listSMSSandboxPhoneNumbersPaginator(a))
    override def listSubscriptions: Kleisli[M, SnsAsyncClient, ListSubscriptionsResponse]                                                                                           = eff(_.listSubscriptions)
    override def listSubscriptions(a: ListSubscriptionsRequest): Kleisli[M, SnsAsyncClient, ListSubscriptionsResponse]                                                              = eff(_.listSubscriptions(a))
    override def listSubscriptionsByTopic(a: ListSubscriptionsByTopicRequest): Kleisli[M, SnsAsyncClient, ListSubscriptionsByTopicResponse]                                         = eff(_.listSubscriptionsByTopic(a))
    override def listSubscriptionsByTopicPaginator(a: ListSubscriptionsByTopicRequest): Kleisli[M, SnsAsyncClient, ListSubscriptionsByTopicPublisher]                               = primitive(_.listSubscriptionsByTopicPaginator(a))
    override def listSubscriptionsPaginator: Kleisli[M, SnsAsyncClient, ListSubscriptionsPublisher]                                                                                 = primitive(_.listSubscriptionsPaginator)
    override def listSubscriptionsPaginator(a: ListSubscriptionsRequest): Kleisli[M, SnsAsyncClient, ListSubscriptionsPublisher]                                                    = primitive(_.listSubscriptionsPaginator(a))
    override def listTagsForResource(a: ListTagsForResourceRequest): Kleisli[M, SnsAsyncClient, ListTagsForResourceResponse]                                                        = eff(_.listTagsForResource(a))
    override def listTopics: Kleisli[M, SnsAsyncClient, ListTopicsResponse]                                                                                                         = eff(_.listTopics)
    override def listTopics(a: ListTopicsRequest): Kleisli[M, SnsAsyncClient, ListTopicsResponse]                                                                                   = eff(_.listTopics(a))
    override def listTopicsPaginator: Kleisli[M, SnsAsyncClient, ListTopicsPublisher]                                                                                               = primitive(_.listTopicsPaginator)
    override def listTopicsPaginator(a: ListTopicsRequest): Kleisli[M, SnsAsyncClient, ListTopicsPublisher]                                                                         = primitive(_.listTopicsPaginator(a))
    override def optInPhoneNumber(a: OptInPhoneNumberRequest): Kleisli[M, SnsAsyncClient, OptInPhoneNumberResponse]                                                                 = eff(_.optInPhoneNumber(a))
    override def publish(a: PublishRequest): Kleisli[M, SnsAsyncClient, PublishResponse]                                                                                            = eff(_.publish(a))
    override def publishBatch(a: PublishBatchRequest): Kleisli[M, SnsAsyncClient, PublishBatchResponse]                                                                             = eff(_.publishBatch(a))
    override def putDataProtectionPolicy(a: PutDataProtectionPolicyRequest): Kleisli[M, SnsAsyncClient, PutDataProtectionPolicyResponse]                                            = eff(_.putDataProtectionPolicy(a))
    override def removePermission(a: RemovePermissionRequest): Kleisli[M, SnsAsyncClient, RemovePermissionResponse]                                                                 = eff(_.removePermission(a))
    override def serviceName: Kleisli[M, SnsAsyncClient, String]                                                                                                                    = primitive(_.serviceName)
    override def setEndpointAttributes(a: SetEndpointAttributesRequest): Kleisli[M, SnsAsyncClient, SetEndpointAttributesResponse]                                                  = eff(_.setEndpointAttributes(a))
    override def setPlatformApplicationAttributes(a: SetPlatformApplicationAttributesRequest): Kleisli[M, SnsAsyncClient, SetPlatformApplicationAttributesResponse]                 = eff(_.setPlatformApplicationAttributes(a))
    override def setSMSAttributes(a: SetSmsAttributesRequest): Kleisli[M, SnsAsyncClient, SetSmsAttributesResponse]                                                                 = eff(_.setSMSAttributes(a))
    override def setSubscriptionAttributes(a: SetSubscriptionAttributesRequest): Kleisli[M, SnsAsyncClient, SetSubscriptionAttributesResponse]                                      = eff(_.setSubscriptionAttributes(a))
    override def setTopicAttributes(a: SetTopicAttributesRequest): Kleisli[M, SnsAsyncClient, SetTopicAttributesResponse]                                                           = eff(_.setTopicAttributes(a))
    override def subscribe(a: SubscribeRequest): Kleisli[M, SnsAsyncClient, SubscribeResponse]                                                                                      = eff(_.subscribe(a))
    override def tagResource(a: TagResourceRequest): Kleisli[M, SnsAsyncClient, TagResourceResponse]                                                                                = eff(_.tagResource(a))
    override def unsubscribe(a: UnsubscribeRequest): Kleisli[M, SnsAsyncClient, UnsubscribeResponse]                                                                                = eff(_.unsubscribe(a))
    override def untagResource(a: UntagResourceRequest): Kleisli[M, SnsAsyncClient, UntagResourceResponse]                                                                          = eff(_.untagResource(a))
    override def verifySMSSandboxPhoneNumber(a: VerifySmsSandboxPhoneNumberRequest): Kleisli[M, SnsAsyncClient, VerifySmsSandboxPhoneNumberResponse]                                = eff(_.verifySMSSandboxPhoneNumber(a))

    def lens[E](f: E => SnsAsyncClient): SnsAsyncClientOp[Kleisli[M, E, *]] =
      new SnsAsyncClientOp[Kleisli[M, E, *]] {
        override def addPermission(a: AddPermissionRequest): Kleisli[M, E, AddPermissionResponse]                                                                          = Kleisli(e => eff1(f(e).addPermission(a)))
        override def checkIfPhoneNumberIsOptedOut(a: CheckIfPhoneNumberIsOptedOutRequest): Kleisli[M, E, CheckIfPhoneNumberIsOptedOutResponse]                             = Kleisli(e => eff1(f(e).checkIfPhoneNumberIsOptedOut(a)))
        override def close: Kleisli[M, E, Unit]                                                                                                                            = Kleisli(e => primitive1(f(e).close))
        override def confirmSubscription(a: ConfirmSubscriptionRequest): Kleisli[M, E, ConfirmSubscriptionResponse]                                                        = Kleisli(e => eff1(f(e).confirmSubscription(a)))
        override def createPlatformApplication(a: CreatePlatformApplicationRequest): Kleisli[M, E, CreatePlatformApplicationResponse]                                      = Kleisli(e => eff1(f(e).createPlatformApplication(a)))
        override def createPlatformEndpoint(a: CreatePlatformEndpointRequest): Kleisli[M, E, CreatePlatformEndpointResponse]                                               = Kleisli(e => eff1(f(e).createPlatformEndpoint(a)))
        override def createSMSSandboxPhoneNumber(a: CreateSmsSandboxPhoneNumberRequest): Kleisli[M, E, CreateSmsSandboxPhoneNumberResponse]                                = Kleisli(e => eff1(f(e).createSMSSandboxPhoneNumber(a)))
        override def createTopic(a: CreateTopicRequest): Kleisli[M, E, CreateTopicResponse]                                                                                = Kleisli(e => eff1(f(e).createTopic(a)))
        override def deleteEndpoint(a: DeleteEndpointRequest): Kleisli[M, E, DeleteEndpointResponse]                                                                       = Kleisli(e => eff1(f(e).deleteEndpoint(a)))
        override def deletePlatformApplication(a: DeletePlatformApplicationRequest): Kleisli[M, E, DeletePlatformApplicationResponse]                                      = Kleisli(e => eff1(f(e).deletePlatformApplication(a)))
        override def deleteSMSSandboxPhoneNumber(a: DeleteSmsSandboxPhoneNumberRequest): Kleisli[M, E, DeleteSmsSandboxPhoneNumberResponse]                                = Kleisli(e => eff1(f(e).deleteSMSSandboxPhoneNumber(a)))
        override def deleteTopic(a: DeleteTopicRequest): Kleisli[M, E, DeleteTopicResponse]                                                                                = Kleisli(e => eff1(f(e).deleteTopic(a)))
        override def getDataProtectionPolicy(a: GetDataProtectionPolicyRequest): Kleisli[M, E, GetDataProtectionPolicyResponse]                                            = Kleisli(e => eff1(f(e).getDataProtectionPolicy(a)))
        override def getEndpointAttributes(a: GetEndpointAttributesRequest): Kleisli[M, E, GetEndpointAttributesResponse]                                                  = Kleisli(e => eff1(f(e).getEndpointAttributes(a)))
        override def getPlatformApplicationAttributes(a: GetPlatformApplicationAttributesRequest): Kleisli[M, E, GetPlatformApplicationAttributesResponse]                 = Kleisli(e => eff1(f(e).getPlatformApplicationAttributes(a)))
        override def getSMSAttributes: Kleisli[M, E, GetSmsAttributesResponse]                                                                                             = Kleisli(e => eff1(f(e).getSMSAttributes))
        override def getSMSAttributes(a: GetSmsAttributesRequest): Kleisli[M, E, GetSmsAttributesResponse]                                                                 = Kleisli(e => eff1(f(e).getSMSAttributes(a)))
        override def getSMSSandboxAccountStatus(a: GetSmsSandboxAccountStatusRequest): Kleisli[M, E, GetSmsSandboxAccountStatusResponse]                                   = Kleisli(e => eff1(f(e).getSMSSandboxAccountStatus(a)))
        override def getSubscriptionAttributes(a: GetSubscriptionAttributesRequest): Kleisli[M, E, GetSubscriptionAttributesResponse]                                      = Kleisli(e => eff1(f(e).getSubscriptionAttributes(a)))
        override def getTopicAttributes(a: GetTopicAttributesRequest): Kleisli[M, E, GetTopicAttributesResponse]                                                           = Kleisli(e => eff1(f(e).getTopicAttributes(a)))
        override def listEndpointsByPlatformApplication(a: ListEndpointsByPlatformApplicationRequest): Kleisli[M, E, ListEndpointsByPlatformApplicationResponse]           = Kleisli(e => eff1(f(e).listEndpointsByPlatformApplication(a)))
        override def listEndpointsByPlatformApplicationPaginator(a: ListEndpointsByPlatformApplicationRequest): Kleisli[M, E, ListEndpointsByPlatformApplicationPublisher] = Kleisli(e => primitive1(f(e).listEndpointsByPlatformApplicationPaginator(a)))
        override def listOriginationNumbers(a: ListOriginationNumbersRequest): Kleisli[M, E, ListOriginationNumbersResponse]                                               = Kleisli(e => eff1(f(e).listOriginationNumbers(a)))
        override def listOriginationNumbersPaginator(a: ListOriginationNumbersRequest): Kleisli[M, E, ListOriginationNumbersPublisher]                                     = Kleisli(e => primitive1(f(e).listOriginationNumbersPaginator(a)))
        override def listPhoneNumbersOptedOut: Kleisli[M, E, ListPhoneNumbersOptedOutResponse]                                                                             = Kleisli(e => eff1(f(e).listPhoneNumbersOptedOut))
        override def listPhoneNumbersOptedOut(a: ListPhoneNumbersOptedOutRequest): Kleisli[M, E, ListPhoneNumbersOptedOutResponse]                                         = Kleisli(e => eff1(f(e).listPhoneNumbersOptedOut(a)))
        override def listPhoneNumbersOptedOutPaginator: Kleisli[M, E, ListPhoneNumbersOptedOutPublisher]                                                                   = Kleisli(e => primitive1(f(e).listPhoneNumbersOptedOutPaginator))
        override def listPhoneNumbersOptedOutPaginator(a: ListPhoneNumbersOptedOutRequest): Kleisli[M, E, ListPhoneNumbersOptedOutPublisher]                               = Kleisli(e => primitive1(f(e).listPhoneNumbersOptedOutPaginator(a)))
        override def listPlatformApplications: Kleisli[M, E, ListPlatformApplicationsResponse]                                                                             = Kleisli(e => eff1(f(e).listPlatformApplications))
        override def listPlatformApplications(a: ListPlatformApplicationsRequest): Kleisli[M, E, ListPlatformApplicationsResponse]                                         = Kleisli(e => eff1(f(e).listPlatformApplications(a)))
        override def listPlatformApplicationsPaginator: Kleisli[M, E, ListPlatformApplicationsPublisher]                                                                   = Kleisli(e => primitive1(f(e).listPlatformApplicationsPaginator))
        override def listPlatformApplicationsPaginator(a: ListPlatformApplicationsRequest): Kleisli[M, E, ListPlatformApplicationsPublisher]                               = Kleisli(e => primitive1(f(e).listPlatformApplicationsPaginator(a)))
        override def listSMSSandboxPhoneNumbers(a: ListSmsSandboxPhoneNumbersRequest): Kleisli[M, E, ListSmsSandboxPhoneNumbersResponse]                                   = Kleisli(e => eff1(f(e).listSMSSandboxPhoneNumbers(a)))
        override def listSMSSandboxPhoneNumbersPaginator(a: ListSmsSandboxPhoneNumbersRequest): Kleisli[M, E, ListSMSSandboxPhoneNumbersPublisher]                         = Kleisli(e => primitive1(f(e).listSMSSandboxPhoneNumbersPaginator(a)))
        override def listSubscriptions: Kleisli[M, E, ListSubscriptionsResponse]                                                                                           = Kleisli(e => eff1(f(e).listSubscriptions))
        override def listSubscriptions(a: ListSubscriptionsRequest): Kleisli[M, E, ListSubscriptionsResponse]                                                              = Kleisli(e => eff1(f(e).listSubscriptions(a)))
        override def listSubscriptionsByTopic(a: ListSubscriptionsByTopicRequest): Kleisli[M, E, ListSubscriptionsByTopicResponse]                                         = Kleisli(e => eff1(f(e).listSubscriptionsByTopic(a)))
        override def listSubscriptionsByTopicPaginator(a: ListSubscriptionsByTopicRequest): Kleisli[M, E, ListSubscriptionsByTopicPublisher]                               = Kleisli(e => primitive1(f(e).listSubscriptionsByTopicPaginator(a)))
        override def listSubscriptionsPaginator: Kleisli[M, E, ListSubscriptionsPublisher]                                                                                 = Kleisli(e => primitive1(f(e).listSubscriptionsPaginator))
        override def listSubscriptionsPaginator(a: ListSubscriptionsRequest): Kleisli[M, E, ListSubscriptionsPublisher]                                                    = Kleisli(e => primitive1(f(e).listSubscriptionsPaginator(a)))
        override def listTagsForResource(a: ListTagsForResourceRequest): Kleisli[M, E, ListTagsForResourceResponse]                                                        = Kleisli(e => eff1(f(e).listTagsForResource(a)))
        override def listTopics: Kleisli[M, E, ListTopicsResponse]                                                                                                         = Kleisli(e => eff1(f(e).listTopics))
        override def listTopics(a: ListTopicsRequest): Kleisli[M, E, ListTopicsResponse]                                                                                   = Kleisli(e => eff1(f(e).listTopics(a)))
        override def listTopicsPaginator: Kleisli[M, E, ListTopicsPublisher]                                                                                               = Kleisli(e => primitive1(f(e).listTopicsPaginator))
        override def listTopicsPaginator(a: ListTopicsRequest): Kleisli[M, E, ListTopicsPublisher]                                                                         = Kleisli(e => primitive1(f(e).listTopicsPaginator(a)))
        override def optInPhoneNumber(a: OptInPhoneNumberRequest): Kleisli[M, E, OptInPhoneNumberResponse]                                                                 = Kleisli(e => eff1(f(e).optInPhoneNumber(a)))
        override def publish(a: PublishRequest): Kleisli[M, E, PublishResponse]                                                                                            = Kleisli(e => eff1(f(e).publish(a)))
        override def publishBatch(a: PublishBatchRequest): Kleisli[M, E, PublishBatchResponse]                                                                             = Kleisli(e => eff1(f(e).publishBatch(a)))
        override def putDataProtectionPolicy(a: PutDataProtectionPolicyRequest): Kleisli[M, E, PutDataProtectionPolicyResponse]                                            = Kleisli(e => eff1(f(e).putDataProtectionPolicy(a)))
        override def removePermission(a: RemovePermissionRequest): Kleisli[M, E, RemovePermissionResponse]                                                                 = Kleisli(e => eff1(f(e).removePermission(a)))
        override def serviceName: Kleisli[M, E, String]                                                                                                                    = Kleisli(e => primitive1(f(e).serviceName))
        override def setEndpointAttributes(a: SetEndpointAttributesRequest): Kleisli[M, E, SetEndpointAttributesResponse]                                                  = Kleisli(e => eff1(f(e).setEndpointAttributes(a)))
        override def setPlatformApplicationAttributes(a: SetPlatformApplicationAttributesRequest): Kleisli[M, E, SetPlatformApplicationAttributesResponse]                 = Kleisli(e => eff1(f(e).setPlatformApplicationAttributes(a)))
        override def setSMSAttributes(a: SetSmsAttributesRequest): Kleisli[M, E, SetSmsAttributesResponse]                                                                 = Kleisli(e => eff1(f(e).setSMSAttributes(a)))
        override def setSubscriptionAttributes(a: SetSubscriptionAttributesRequest): Kleisli[M, E, SetSubscriptionAttributesResponse]                                      = Kleisli(e => eff1(f(e).setSubscriptionAttributes(a)))
        override def setTopicAttributes(a: SetTopicAttributesRequest): Kleisli[M, E, SetTopicAttributesResponse]                                                           = Kleisli(e => eff1(f(e).setTopicAttributes(a)))
        override def subscribe(a: SubscribeRequest): Kleisli[M, E, SubscribeResponse]                                                                                      = Kleisli(e => eff1(f(e).subscribe(a)))
        override def tagResource(a: TagResourceRequest): Kleisli[M, E, TagResourceResponse]                                                                                = Kleisli(e => eff1(f(e).tagResource(a)))
        override def unsubscribe(a: UnsubscribeRequest): Kleisli[M, E, UnsubscribeResponse]                                                                                = Kleisli(e => eff1(f(e).unsubscribe(a)))
        override def untagResource(a: UntagResourceRequest): Kleisli[M, E, UntagResourceResponse]                                                                          = Kleisli(e => eff1(f(e).untagResource(a)))
        override def verifySMSSandboxPhoneNumber(a: VerifySmsSandboxPhoneNumberRequest): Kleisli[M, E, VerifySmsSandboxPhoneNumberResponse]                                = Kleisli(e => eff1(f(e).verifySMSSandboxPhoneNumber(a)))
      }
  }
  // end interpreters

  def SnsAsyncClientResource(builder: SnsAsyncClientBuilder): Resource[M, SnsAsyncClient]        = Resource.fromAutoCloseable(asyncM.delay(builder.build()))
  def SnsAsyncClientOpResource(builder: SnsAsyncClientBuilder): Resource[M, SnsAsyncClientOp[M]] = SnsAsyncClientResource(builder).map(create)

  def create(client: SnsAsyncClient): SnsAsyncClientOp[M] = new SnsAsyncClientOp[M] {

    // domain-specific operations are implemented in terms of `primitive`
    override def addPermission(a: AddPermissionRequest): M[AddPermissionResponse]                                                                          = eff1(client.addPermission(a))
    override def checkIfPhoneNumberIsOptedOut(a: CheckIfPhoneNumberIsOptedOutRequest): M[CheckIfPhoneNumberIsOptedOutResponse]                             = eff1(client.checkIfPhoneNumberIsOptedOut(a))
    override def close: M[Unit]                                                                                                                            = primitive1(client.close)
    override def confirmSubscription(a: ConfirmSubscriptionRequest): M[ConfirmSubscriptionResponse]                                                        = eff1(client.confirmSubscription(a))
    override def createPlatformApplication(a: CreatePlatformApplicationRequest): M[CreatePlatformApplicationResponse]                                      = eff1(client.createPlatformApplication(a))
    override def createPlatformEndpoint(a: CreatePlatformEndpointRequest): M[CreatePlatformEndpointResponse]                                               = eff1(client.createPlatformEndpoint(a))
    override def createSMSSandboxPhoneNumber(a: CreateSmsSandboxPhoneNumberRequest): M[CreateSmsSandboxPhoneNumberResponse]                                = eff1(client.createSMSSandboxPhoneNumber(a))
    override def createTopic(a: CreateTopicRequest): M[CreateTopicResponse]                                                                                = eff1(client.createTopic(a))
    override def deleteEndpoint(a: DeleteEndpointRequest): M[DeleteEndpointResponse]                                                                       = eff1(client.deleteEndpoint(a))
    override def deletePlatformApplication(a: DeletePlatformApplicationRequest): M[DeletePlatformApplicationResponse]                                      = eff1(client.deletePlatformApplication(a))
    override def deleteSMSSandboxPhoneNumber(a: DeleteSmsSandboxPhoneNumberRequest): M[DeleteSmsSandboxPhoneNumberResponse]                                = eff1(client.deleteSMSSandboxPhoneNumber(a))
    override def deleteTopic(a: DeleteTopicRequest): M[DeleteTopicResponse]                                                                                = eff1(client.deleteTopic(a))
    override def getDataProtectionPolicy(a: GetDataProtectionPolicyRequest): M[GetDataProtectionPolicyResponse]                                            = eff1(client.getDataProtectionPolicy(a))
    override def getEndpointAttributes(a: GetEndpointAttributesRequest): M[GetEndpointAttributesResponse]                                                  = eff1(client.getEndpointAttributes(a))
    override def getPlatformApplicationAttributes(a: GetPlatformApplicationAttributesRequest): M[GetPlatformApplicationAttributesResponse]                 = eff1(client.getPlatformApplicationAttributes(a))
    override def getSMSAttributes: M[GetSmsAttributesResponse]                                                                                             = eff1(client.getSMSAttributes)
    override def getSMSAttributes(a: GetSmsAttributesRequest): M[GetSmsAttributesResponse]                                                                 = eff1(client.getSMSAttributes(a))
    override def getSMSSandboxAccountStatus(a: GetSmsSandboxAccountStatusRequest): M[GetSmsSandboxAccountStatusResponse]                                   = eff1(client.getSMSSandboxAccountStatus(a))
    override def getSubscriptionAttributes(a: GetSubscriptionAttributesRequest): M[GetSubscriptionAttributesResponse]                                      = eff1(client.getSubscriptionAttributes(a))
    override def getTopicAttributes(a: GetTopicAttributesRequest): M[GetTopicAttributesResponse]                                                           = eff1(client.getTopicAttributes(a))
    override def listEndpointsByPlatformApplication(a: ListEndpointsByPlatformApplicationRequest): M[ListEndpointsByPlatformApplicationResponse]           = eff1(client.listEndpointsByPlatformApplication(a))
    override def listEndpointsByPlatformApplicationPaginator(a: ListEndpointsByPlatformApplicationRequest): M[ListEndpointsByPlatformApplicationPublisher] = primitive1(client.listEndpointsByPlatformApplicationPaginator(a))
    override def listOriginationNumbers(a: ListOriginationNumbersRequest): M[ListOriginationNumbersResponse]                                               = eff1(client.listOriginationNumbers(a))
    override def listOriginationNumbersPaginator(a: ListOriginationNumbersRequest): M[ListOriginationNumbersPublisher]                                     = primitive1(client.listOriginationNumbersPaginator(a))
    override def listPhoneNumbersOptedOut: M[ListPhoneNumbersOptedOutResponse]                                                                             = eff1(client.listPhoneNumbersOptedOut)
    override def listPhoneNumbersOptedOut(a: ListPhoneNumbersOptedOutRequest): M[ListPhoneNumbersOptedOutResponse]                                         = eff1(client.listPhoneNumbersOptedOut(a))
    override def listPhoneNumbersOptedOutPaginator: M[ListPhoneNumbersOptedOutPublisher]                                                                   = primitive1(client.listPhoneNumbersOptedOutPaginator)
    override def listPhoneNumbersOptedOutPaginator(a: ListPhoneNumbersOptedOutRequest): M[ListPhoneNumbersOptedOutPublisher]                               = primitive1(client.listPhoneNumbersOptedOutPaginator(a))
    override def listPlatformApplications: M[ListPlatformApplicationsResponse]                                                                             = eff1(client.listPlatformApplications)
    override def listPlatformApplications(a: ListPlatformApplicationsRequest): M[ListPlatformApplicationsResponse]                                         = eff1(client.listPlatformApplications(a))
    override def listPlatformApplicationsPaginator: M[ListPlatformApplicationsPublisher]                                                                   = primitive1(client.listPlatformApplicationsPaginator)
    override def listPlatformApplicationsPaginator(a: ListPlatformApplicationsRequest): M[ListPlatformApplicationsPublisher]                               = primitive1(client.listPlatformApplicationsPaginator(a))
    override def listSMSSandboxPhoneNumbers(a: ListSmsSandboxPhoneNumbersRequest): M[ListSmsSandboxPhoneNumbersResponse]                                   = eff1(client.listSMSSandboxPhoneNumbers(a))
    override def listSMSSandboxPhoneNumbersPaginator(a: ListSmsSandboxPhoneNumbersRequest): M[ListSMSSandboxPhoneNumbersPublisher]                         = primitive1(client.listSMSSandboxPhoneNumbersPaginator(a))
    override def listSubscriptions: M[ListSubscriptionsResponse]                                                                                           = eff1(client.listSubscriptions)
    override def listSubscriptions(a: ListSubscriptionsRequest): M[ListSubscriptionsResponse]                                                              = eff1(client.listSubscriptions(a))
    override def listSubscriptionsByTopic(a: ListSubscriptionsByTopicRequest): M[ListSubscriptionsByTopicResponse]                                         = eff1(client.listSubscriptionsByTopic(a))
    override def listSubscriptionsByTopicPaginator(a: ListSubscriptionsByTopicRequest): M[ListSubscriptionsByTopicPublisher]                               = primitive1(client.listSubscriptionsByTopicPaginator(a))
    override def listSubscriptionsPaginator: M[ListSubscriptionsPublisher]                                                                                 = primitive1(client.listSubscriptionsPaginator)
    override def listSubscriptionsPaginator(a: ListSubscriptionsRequest): M[ListSubscriptionsPublisher]                                                    = primitive1(client.listSubscriptionsPaginator(a))
    override def listTagsForResource(a: ListTagsForResourceRequest): M[ListTagsForResourceResponse]                                                        = eff1(client.listTagsForResource(a))
    override def listTopics: M[ListTopicsResponse]                                                                                                         = eff1(client.listTopics)
    override def listTopics(a: ListTopicsRequest): M[ListTopicsResponse]                                                                                   = eff1(client.listTopics(a))
    override def listTopicsPaginator: M[ListTopicsPublisher]                                                                                               = primitive1(client.listTopicsPaginator)
    override def listTopicsPaginator(a: ListTopicsRequest): M[ListTopicsPublisher]                                                                         = primitive1(client.listTopicsPaginator(a))
    override def optInPhoneNumber(a: OptInPhoneNumberRequest): M[OptInPhoneNumberResponse]                                                                 = eff1(client.optInPhoneNumber(a))
    override def publish(a: PublishRequest): M[PublishResponse]                                                                                            = eff1(client.publish(a))
    override def publishBatch(a: PublishBatchRequest): M[PublishBatchResponse]                                                                             = eff1(client.publishBatch(a))
    override def putDataProtectionPolicy(a: PutDataProtectionPolicyRequest): M[PutDataProtectionPolicyResponse]                                            = eff1(client.putDataProtectionPolicy(a))
    override def removePermission(a: RemovePermissionRequest): M[RemovePermissionResponse]                                                                 = eff1(client.removePermission(a))
    override def serviceName: M[String]                                                                                                                    = primitive1(client.serviceName)
    override def setEndpointAttributes(a: SetEndpointAttributesRequest): M[SetEndpointAttributesResponse]                                                  = eff1(client.setEndpointAttributes(a))
    override def setPlatformApplicationAttributes(a: SetPlatformApplicationAttributesRequest): M[SetPlatformApplicationAttributesResponse]                 = eff1(client.setPlatformApplicationAttributes(a))
    override def setSMSAttributes(a: SetSmsAttributesRequest): M[SetSmsAttributesResponse]                                                                 = eff1(client.setSMSAttributes(a))
    override def setSubscriptionAttributes(a: SetSubscriptionAttributesRequest): M[SetSubscriptionAttributesResponse]                                      = eff1(client.setSubscriptionAttributes(a))
    override def setTopicAttributes(a: SetTopicAttributesRequest): M[SetTopicAttributesResponse]                                                           = eff1(client.setTopicAttributes(a))
    override def subscribe(a: SubscribeRequest): M[SubscribeResponse]                                                                                      = eff1(client.subscribe(a))
    override def tagResource(a: TagResourceRequest): M[TagResourceResponse]                                                                                = eff1(client.tagResource(a))
    override def unsubscribe(a: UnsubscribeRequest): M[UnsubscribeResponse]                                                                                = eff1(client.unsubscribe(a))
    override def untagResource(a: UntagResourceRequest): M[UntagResourceResponse]                                                                          = eff1(client.untagResource(a))
    override def verifySMSSandboxPhoneNumber(a: VerifySmsSandboxPhoneNumberRequest): M[VerifySmsSandboxPhoneNumberResponse]                                = eff1(client.verifySMSSandboxPhoneNumber(a))

  }

}
