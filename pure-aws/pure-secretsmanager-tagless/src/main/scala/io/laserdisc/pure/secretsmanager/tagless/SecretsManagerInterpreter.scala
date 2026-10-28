package io.laserdisc.pure.secretsmanager.tagless

// Library imports
import cats.data.Kleisli
import cats.effect.{Async, Resource}

import software.amazon.awssdk.services.secretsmanager.*
import software.amazon.awssdk.services.secretsmanager.model.*

// Types referenced
import software.amazon.awssdk.services.secretsmanager.SecretsManagerServiceClientConfiguration
import software.amazon.awssdk.services.secretsmanager.paginators.BatchGetSecretValuePublisher
import software.amazon.awssdk.services.secretsmanager.paginators.ListSecretVersionIdsPublisher
import software.amazon.awssdk.services.secretsmanager.paginators.ListSecretsPublisher

object SecretsManagerInterpreter {

  def apply[M[_]](
      implicit am: Async[M]
  ): SecretsManagerInterpreter[M] =
    new SecretsManagerInterpreter[M] {
      val asyncM: Async[M] = am
    }

}

// the interpreter was previously named `Interpreter`; kept (as both a type and a term) so 6.x code still compiles
@deprecated("use SecretsManagerInterpreter", "7.0.0")
trait Interpreter[M[_]] extends SecretsManagerInterpreter[M]

@deprecated("use SecretsManagerInterpreter", "7.0.0")
object Interpreter {
  def apply[M[_]](implicit am: Async[M]): Interpreter[M] =
    new Interpreter[M] {
      val asyncM: Async[M] = am
    }
}

// Family of interpreters into Kleisli arrows for some monad M.
trait SecretsManagerInterpreter[M[_]] { outer =>

  import java.util.concurrent.CompletableFuture

  implicit val asyncM: Async[M]

  lazy val SecretsManagerAsyncClientInterpreter: SecretsManagerAsyncClientInterpreter =
    new SecretsManagerAsyncClientInterpreter {}
  // Some methods are common to all interpreters and can be overridden to change behavior globally.

  private def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(j => asyncM.blocking(f(j)))
  private def primitive1[J, A](f: => A): M[A]              = asyncM.blocking(f)

  private def eff[J, A](fut: J => CompletableFuture[A]): Kleisli[M, J, A] = Kleisli { j =>
    asyncM.fromCompletableFuture(asyncM.delay(fut(j)))
  }
  private def eff1[J, A](fut: => CompletableFuture[A]): M[A] =
    asyncM.fromCompletableFuture(asyncM.delay(fut))

  // Interpreters
  trait SecretsManagerAsyncClientInterpreter
      extends SecretsManagerAsyncClientOp[Kleisli[M, SecretsManagerAsyncClient, *]] {

    // domain-specific operations are implemented in terms of `primitive`
    override def batchGetSecretValue(
        a: BatchGetSecretValueRequest
    ): Kleisli[M, SecretsManagerAsyncClient, BatchGetSecretValueResponse] = eff(_.batchGetSecretValue(a))
    override def batchGetSecretValuePaginator(
        a: BatchGetSecretValueRequest
    ): Kleisli[M, SecretsManagerAsyncClient, BatchGetSecretValuePublisher] = primitive(
      _.batchGetSecretValuePaginator(a)
    )
    override def cancelRotateSecret(
        a: CancelRotateSecretRequest
    ): Kleisli[M, SecretsManagerAsyncClient, CancelRotateSecretResponse] = eff(_.cancelRotateSecret(a))
    override def close: Kleisli[M, SecretsManagerAsyncClient, Unit]      = primitive(_.close)
    override def createSecret(a: CreateSecretRequest): Kleisli[M, SecretsManagerAsyncClient, CreateSecretResponse] =
      eff(_.createSecret(a))
    override def deleteResourcePolicy(
        a: DeleteResourcePolicyRequest
    ): Kleisli[M, SecretsManagerAsyncClient, DeleteResourcePolicyResponse] = eff(_.deleteResourcePolicy(a))
    override def deleteSecret(a: DeleteSecretRequest): Kleisli[M, SecretsManagerAsyncClient, DeleteSecretResponse] =
      eff(_.deleteSecret(a))
    override def describeSecret(
        a: DescribeSecretRequest
    ): Kleisli[M, SecretsManagerAsyncClient, DescribeSecretResponse] = eff(_.describeSecret(a))
    override def getRandomPassword: Kleisli[M, SecretsManagerAsyncClient, GetRandomPasswordResponse] = eff(
      _.getRandomPassword
    )
    override def getRandomPassword(
        a: GetRandomPasswordRequest
    ): Kleisli[M, SecretsManagerAsyncClient, GetRandomPasswordResponse] = eff(_.getRandomPassword(a))
    override def getResourcePolicy(
        a: GetResourcePolicyRequest
    ): Kleisli[M, SecretsManagerAsyncClient, GetResourcePolicyResponse] = eff(_.getResourcePolicy(a))
    override def getSecretValue(
        a: GetSecretValueRequest
    ): Kleisli[M, SecretsManagerAsyncClient, GetSecretValueResponse] = eff(_.getSecretValue(a))
    override def listSecretVersionIds(
        a: ListSecretVersionIdsRequest
    ): Kleisli[M, SecretsManagerAsyncClient, ListSecretVersionIdsResponse] = eff(_.listSecretVersionIds(a))
    override def listSecretVersionIdsPaginator(
        a: ListSecretVersionIdsRequest
    ): Kleisli[M, SecretsManagerAsyncClient, ListSecretVersionIdsPublisher] = primitive(
      _.listSecretVersionIdsPaginator(a)
    )
    override def listSecrets: Kleisli[M, SecretsManagerAsyncClient, ListSecretsResponse] = eff(_.listSecrets)
    override def listSecrets(a: ListSecretsRequest): Kleisli[M, SecretsManagerAsyncClient, ListSecretsResponse] = eff(
      _.listSecrets(a)
    )
    override def listSecretsPaginator: Kleisli[M, SecretsManagerAsyncClient, ListSecretsPublisher] = primitive(
      _.listSecretsPaginator
    )
    override def listSecretsPaginator(
        a: ListSecretsRequest
    ): Kleisli[M, SecretsManagerAsyncClient, ListSecretsPublisher] = primitive(_.listSecretsPaginator(a))
    override def putResourcePolicy(
        a: PutResourcePolicyRequest
    ): Kleisli[M, SecretsManagerAsyncClient, PutResourcePolicyResponse] = eff(_.putResourcePolicy(a))
    override def putSecretValue(
        a: PutSecretValueRequest
    ): Kleisli[M, SecretsManagerAsyncClient, PutSecretValueResponse] = eff(_.putSecretValue(a))
    override def removeRegionsFromReplication(
        a: RemoveRegionsFromReplicationRequest
    ): Kleisli[M, SecretsManagerAsyncClient, RemoveRegionsFromReplicationResponse] = eff(
      _.removeRegionsFromReplication(a)
    )
    override def replicateSecretToRegions(
        a: ReplicateSecretToRegionsRequest
    ): Kleisli[M, SecretsManagerAsyncClient, ReplicateSecretToRegionsResponse] = eff(_.replicateSecretToRegions(a))
    override def restoreSecret(a: RestoreSecretRequest): Kleisli[M, SecretsManagerAsyncClient, RestoreSecretResponse] =
      eff(_.restoreSecret(a))
    override def rotateSecret(a: RotateSecretRequest): Kleisli[M, SecretsManagerAsyncClient, RotateSecretResponse] =
      eff(_.rotateSecret(a))
    override def serviceClientConfiguration
        : Kleisli[M, SecretsManagerAsyncClient, SecretsManagerServiceClientConfiguration] = primitive(
      _.serviceClientConfiguration
    )
    override def serviceName: Kleisli[M, SecretsManagerAsyncClient, String] = primitive(_.serviceName)
    override def stopReplicationToReplica(
        a: StopReplicationToReplicaRequest
    ): Kleisli[M, SecretsManagerAsyncClient, StopReplicationToReplicaResponse] = eff(_.stopReplicationToReplica(a))
    override def tagResource(a: TagResourceRequest): Kleisli[M, SecretsManagerAsyncClient, TagResourceResponse] = eff(
      _.tagResource(a)
    )
    override def untagResource(a: UntagResourceRequest): Kleisli[M, SecretsManagerAsyncClient, UntagResourceResponse] =
      eff(_.untagResource(a))
    override def updateSecret(a: UpdateSecretRequest): Kleisli[M, SecretsManagerAsyncClient, UpdateSecretResponse] =
      eff(_.updateSecret(a))
    override def updateSecretVersionStage(
        a: UpdateSecretVersionStageRequest
    ): Kleisli[M, SecretsManagerAsyncClient, UpdateSecretVersionStageResponse] = eff(_.updateSecretVersionStage(a))
    override def validateResourcePolicy(
        a: ValidateResourcePolicyRequest
    ): Kleisli[M, SecretsManagerAsyncClient, ValidateResourcePolicyResponse] = eff(_.validateResourcePolicy(a))

    def lens[E](f: E => SecretsManagerAsyncClient): SecretsManagerAsyncClientOp[Kleisli[M, E, *]] =
      new SecretsManagerAsyncClientOp[Kleisli[M, E, *]] {
        override def batchGetSecretValue(a: BatchGetSecretValueRequest): Kleisli[M, E, BatchGetSecretValueResponse] =
          Kleisli(e => eff1(f(e).batchGetSecretValue(a)))
        override def batchGetSecretValuePaginator(
            a: BatchGetSecretValueRequest
        ): Kleisli[M, E, BatchGetSecretValuePublisher] = Kleisli(e => primitive1(f(e).batchGetSecretValuePaginator(a)))
        override def cancelRotateSecret(a: CancelRotateSecretRequest): Kleisli[M, E, CancelRotateSecretResponse] =
          Kleisli(e => eff1(f(e).cancelRotateSecret(a)))
        override def close: Kleisli[M, E, Unit] = Kleisli(e => primitive1(f(e).close))
        override def createSecret(a: CreateSecretRequest): Kleisli[M, E, CreateSecretResponse] =
          Kleisli(e => eff1(f(e).createSecret(a)))
        override def deleteResourcePolicy(a: DeleteResourcePolicyRequest): Kleisli[M, E, DeleteResourcePolicyResponse] =
          Kleisli(e => eff1(f(e).deleteResourcePolicy(a)))
        override def deleteSecret(a: DeleteSecretRequest): Kleisli[M, E, DeleteSecretResponse] =
          Kleisli(e => eff1(f(e).deleteSecret(a)))
        override def describeSecret(a: DescribeSecretRequest): Kleisli[M, E, DescribeSecretResponse] =
          Kleisli(e => eff1(f(e).describeSecret(a)))
        override def getRandomPassword: Kleisli[M, E, GetRandomPasswordResponse] =
          Kleisli(e => eff1(f(e).getRandomPassword))
        override def getRandomPassword(a: GetRandomPasswordRequest): Kleisli[M, E, GetRandomPasswordResponse] =
          Kleisli(e => eff1(f(e).getRandomPassword(a)))
        override def getResourcePolicy(a: GetResourcePolicyRequest): Kleisli[M, E, GetResourcePolicyResponse] =
          Kleisli(e => eff1(f(e).getResourcePolicy(a)))
        override def getSecretValue(a: GetSecretValueRequest): Kleisli[M, E, GetSecretValueResponse] =
          Kleisli(e => eff1(f(e).getSecretValue(a)))
        override def listSecretVersionIds(a: ListSecretVersionIdsRequest): Kleisli[M, E, ListSecretVersionIdsResponse] =
          Kleisli(e => eff1(f(e).listSecretVersionIds(a)))
        override def listSecretVersionIdsPaginator(
            a: ListSecretVersionIdsRequest
        ): Kleisli[M, E, ListSecretVersionIdsPublisher] =
          Kleisli(e => primitive1(f(e).listSecretVersionIdsPaginator(a)))
        override def listSecrets: Kleisli[M, E, ListSecretsResponse] = Kleisli(e => eff1(f(e).listSecrets))
        override def listSecrets(a: ListSecretsRequest): Kleisli[M, E, ListSecretsResponse] =
          Kleisli(e => eff1(f(e).listSecrets(a)))
        override def listSecretsPaginator: Kleisli[M, E, ListSecretsPublisher] =
          Kleisli(e => primitive1(f(e).listSecretsPaginator))
        override def listSecretsPaginator(a: ListSecretsRequest): Kleisli[M, E, ListSecretsPublisher] =
          Kleisli(e => primitive1(f(e).listSecretsPaginator(a)))
        override def putResourcePolicy(a: PutResourcePolicyRequest): Kleisli[M, E, PutResourcePolicyResponse] =
          Kleisli(e => eff1(f(e).putResourcePolicy(a)))
        override def putSecretValue(a: PutSecretValueRequest): Kleisli[M, E, PutSecretValueResponse] =
          Kleisli(e => eff1(f(e).putSecretValue(a)))
        override def removeRegionsFromReplication(
            a: RemoveRegionsFromReplicationRequest
        ): Kleisli[M, E, RemoveRegionsFromReplicationResponse] =
          Kleisli(e => eff1(f(e).removeRegionsFromReplication(a)))
        override def replicateSecretToRegions(
            a: ReplicateSecretToRegionsRequest
        ): Kleisli[M, E, ReplicateSecretToRegionsResponse] = Kleisli(e => eff1(f(e).replicateSecretToRegions(a)))
        override def restoreSecret(a: RestoreSecretRequest): Kleisli[M, E, RestoreSecretResponse] =
          Kleisli(e => eff1(f(e).restoreSecret(a)))
        override def rotateSecret(a: RotateSecretRequest): Kleisli[M, E, RotateSecretResponse] =
          Kleisli(e => eff1(f(e).rotateSecret(a)))
        override def serviceClientConfiguration: Kleisli[M, E, SecretsManagerServiceClientConfiguration] =
          Kleisli(e => primitive1(f(e).serviceClientConfiguration))
        override def serviceName: Kleisli[M, E, String] = Kleisli(e => primitive1(f(e).serviceName))
        override def stopReplicationToReplica(
            a: StopReplicationToReplicaRequest
        ): Kleisli[M, E, StopReplicationToReplicaResponse] = Kleisli(e => eff1(f(e).stopReplicationToReplica(a)))
        override def tagResource(a: TagResourceRequest): Kleisli[M, E, TagResourceResponse] =
          Kleisli(e => eff1(f(e).tagResource(a)))
        override def untagResource(a: UntagResourceRequest): Kleisli[M, E, UntagResourceResponse] =
          Kleisli(e => eff1(f(e).untagResource(a)))
        override def updateSecret(a: UpdateSecretRequest): Kleisli[M, E, UpdateSecretResponse] =
          Kleisli(e => eff1(f(e).updateSecret(a)))
        override def updateSecretVersionStage(
            a: UpdateSecretVersionStageRequest
        ): Kleisli[M, E, UpdateSecretVersionStageResponse] = Kleisli(e => eff1(f(e).updateSecretVersionStage(a)))
        override def validateResourcePolicy(
            a: ValidateResourcePolicyRequest
        ): Kleisli[M, E, ValidateResourcePolicyResponse] = Kleisli(e => eff1(f(e).validateResourcePolicy(a)))
      }
  }
  // end interpreters

  /** Builds a `SecretsManagerAsyncClient` from the given builder, exposed as the [[SecretsManagerAsyncClientOp]] algebra in a managed `Resource`. */
  def resource(builder: SecretsManagerAsyncClientBuilder): Resource[M, SecretsManagerAsyncClientOp[M]] =
    clientResource(builder).map(create)

  /** Like `resource(builder)`, but using a default client builder. */
  def resource: Resource[M, SecretsManagerAsyncClientOp[M]] = resource(SecretsManagerAsyncClient.builder())

  /** Builds the underlying `SecretsManagerAsyncClient` from the given builder as a managed `Resource`. */
  def clientResource(builder: SecretsManagerAsyncClientBuilder): Resource[M, SecretsManagerAsyncClient] =
    Resource.fromAutoCloseable(asyncM.delay(builder.build()))

  /** Like `clientResource(builder)`, but using a default client builder. */
  def clientResource: Resource[M, SecretsManagerAsyncClient] = clientResource(SecretsManagerAsyncClient.builder())

  @deprecated("use clientResource(builder)", "7.0.0")
  def SecretsManagerAsyncClientResource(
      builder: SecretsManagerAsyncClientBuilder
  ): Resource[M, SecretsManagerAsyncClient] = clientResource(builder)

  @deprecated("use resource(builder)", "7.0.0")
  def SecretsManagerAsyncClientOpResource(
      builder: SecretsManagerAsyncClientBuilder
  ): Resource[M, SecretsManagerAsyncClientOp[M]] = resource(builder)

  def create(client: SecretsManagerAsyncClient): SecretsManagerAsyncClientOp[M] = new SecretsManagerAsyncClientOp[M] {

    // domain-specific operations are implemented in terms of `primitive`
    override def batchGetSecretValue(a: BatchGetSecretValueRequest): M[BatchGetSecretValueResponse] = eff1(
      client.batchGetSecretValue(a)
    )
    override def batchGetSecretValuePaginator(a: BatchGetSecretValueRequest): M[BatchGetSecretValuePublisher] =
      primitive1(client.batchGetSecretValuePaginator(a))
    override def cancelRotateSecret(a: CancelRotateSecretRequest): M[CancelRotateSecretResponse] = eff1(
      client.cancelRotateSecret(a)
    )
    override def close: M[Unit]                                                = primitive1(client.close)
    override def createSecret(a: CreateSecretRequest): M[CreateSecretResponse] = eff1(client.createSecret(a))
    override def deleteResourcePolicy(a: DeleteResourcePolicyRequest): M[DeleteResourcePolicyResponse] = eff1(
      client.deleteResourcePolicy(a)
    )
    override def deleteSecret(a: DeleteSecretRequest): M[DeleteSecretResponse]       = eff1(client.deleteSecret(a))
    override def describeSecret(a: DescribeSecretRequest): M[DescribeSecretResponse] = eff1(client.describeSecret(a))
    override def getRandomPassword: M[GetRandomPasswordResponse]                     = eff1(client.getRandomPassword)
    override def getRandomPassword(a: GetRandomPasswordRequest): M[GetRandomPasswordResponse] = eff1(
      client.getRandomPassword(a)
    )
    override def getResourcePolicy(a: GetResourcePolicyRequest): M[GetResourcePolicyResponse] = eff1(
      client.getResourcePolicy(a)
    )
    override def getSecretValue(a: GetSecretValueRequest): M[GetSecretValueResponse] = eff1(client.getSecretValue(a))
    override def listSecretVersionIds(a: ListSecretVersionIdsRequest): M[ListSecretVersionIdsResponse] = eff1(
      client.listSecretVersionIds(a)
    )
    override def listSecretVersionIdsPaginator(a: ListSecretVersionIdsRequest): M[ListSecretVersionIdsPublisher] =
      primitive1(client.listSecretVersionIdsPaginator(a))
    override def listSecrets: M[ListSecretsResponse]                        = eff1(client.listSecrets)
    override def listSecrets(a: ListSecretsRequest): M[ListSecretsResponse] = eff1(client.listSecrets(a))
    override def listSecretsPaginator: M[ListSecretsPublisher]              = primitive1(client.listSecretsPaginator)
    override def listSecretsPaginator(a: ListSecretsRequest): M[ListSecretsPublisher] = primitive1(
      client.listSecretsPaginator(a)
    )
    override def putResourcePolicy(a: PutResourcePolicyRequest): M[PutResourcePolicyResponse] = eff1(
      client.putResourcePolicy(a)
    )
    override def putSecretValue(a: PutSecretValueRequest): M[PutSecretValueResponse] = eff1(client.putSecretValue(a))
    override def removeRegionsFromReplication(
        a: RemoveRegionsFromReplicationRequest
    ): M[RemoveRegionsFromReplicationResponse] = eff1(client.removeRegionsFromReplication(a))
    override def replicateSecretToRegions(a: ReplicateSecretToRegionsRequest): M[ReplicateSecretToRegionsResponse] =
      eff1(client.replicateSecretToRegions(a))
    override def restoreSecret(a: RestoreSecretRequest): M[RestoreSecretResponse]        = eff1(client.restoreSecret(a))
    override def rotateSecret(a: RotateSecretRequest): M[RotateSecretResponse]           = eff1(client.rotateSecret(a))
    override def serviceClientConfiguration: M[SecretsManagerServiceClientConfiguration] = primitive1(
      client.serviceClientConfiguration
    )
    override def serviceName: M[String] = primitive1(client.serviceName)
    override def stopReplicationToReplica(a: StopReplicationToReplicaRequest): M[StopReplicationToReplicaResponse] =
      eff1(client.stopReplicationToReplica(a))
    override def tagResource(a: TagResourceRequest): M[TagResourceResponse]       = eff1(client.tagResource(a))
    override def untagResource(a: UntagResourceRequest): M[UntagResourceResponse] = eff1(client.untagResource(a))
    override def updateSecret(a: UpdateSecretRequest): M[UpdateSecretResponse]    = eff1(client.updateSecret(a))
    override def updateSecretVersionStage(a: UpdateSecretVersionStageRequest): M[UpdateSecretVersionStageResponse] =
      eff1(client.updateSecretVersionStage(a))
    override def validateResourcePolicy(a: ValidateResourcePolicyRequest): M[ValidateResourcePolicyResponse] = eff1(
      client.validateResourcePolicy(a)
    )

  }

}
