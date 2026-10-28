package io.laserdisc.pure.secretsmanager.tagless

import software.amazon.awssdk.services.secretsmanager.model.*

import software.amazon.awssdk.services.secretsmanager.SecretsManagerServiceClientConfiguration
import software.amazon.awssdk.services.secretsmanager.paginators.BatchGetSecretValuePublisher
import software.amazon.awssdk.services.secretsmanager.paginators.ListSecretVersionIdsPublisher
import software.amazon.awssdk.services.secretsmanager.paginators.ListSecretsPublisher

/** A stub implementation of [[SecretsManagerAsyncClientOp]], where all methods throw a NotImplementedError.  Intended to
  * be overridden in tests with mock implementations of the methods under test.
  */
class MockSecretsManagerAsyncClientOp[F[_]] extends SecretsManagerAsyncClientOp[F] {

  def batchGetSecretValue(a: BatchGetSecretValueRequest): F[BatchGetSecretValueResponse]                            = ???
  def batchGetSecretValuePaginator(a: BatchGetSecretValueRequest): F[BatchGetSecretValuePublisher]                  = ???
  def cancelRotateSecret(a: CancelRotateSecretRequest): F[CancelRotateSecretResponse]                               = ???
  def close: F[Unit]                                                                                                = ???
  def createSecret(a: CreateSecretRequest): F[CreateSecretResponse]                                                 = ???
  def deleteResourcePolicy(a: DeleteResourcePolicyRequest): F[DeleteResourcePolicyResponse]                         = ???
  def deleteSecret(a: DeleteSecretRequest): F[DeleteSecretResponse]                                                 = ???
  def describeSecret(a: DescribeSecretRequest): F[DescribeSecretResponse]                                           = ???
  def getRandomPassword: F[GetRandomPasswordResponse]                                                               = ???
  def getRandomPassword(a: GetRandomPasswordRequest): F[GetRandomPasswordResponse]                                  = ???
  def getResourcePolicy(a: GetResourcePolicyRequest): F[GetResourcePolicyResponse]                                  = ???
  def getSecretValue(a: GetSecretValueRequest): F[GetSecretValueResponse]                                           = ???
  def listSecretVersionIds(a: ListSecretVersionIdsRequest): F[ListSecretVersionIdsResponse]                         = ???
  def listSecretVersionIdsPaginator(a: ListSecretVersionIdsRequest): F[ListSecretVersionIdsPublisher]               = ???
  def listSecrets: F[ListSecretsResponse]                                                                           = ???
  def listSecrets(a: ListSecretsRequest): F[ListSecretsResponse]                                                    = ???
  def listSecretsPaginator: F[ListSecretsPublisher]                                                                 = ???
  def listSecretsPaginator(a: ListSecretsRequest): F[ListSecretsPublisher]                                          = ???
  def putResourcePolicy(a: PutResourcePolicyRequest): F[PutResourcePolicyResponse]                                  = ???
  def putSecretValue(a: PutSecretValueRequest): F[PutSecretValueResponse]                                           = ???
  def removeRegionsFromReplication(a: RemoveRegionsFromReplicationRequest): F[RemoveRegionsFromReplicationResponse] = ???
  def replicateSecretToRegions(a: ReplicateSecretToRegionsRequest): F[ReplicateSecretToRegionsResponse]             = ???
  def restoreSecret(a: RestoreSecretRequest): F[RestoreSecretResponse]                                              = ???
  def rotateSecret(a: RotateSecretRequest): F[RotateSecretResponse]                                                 = ???
  def serviceClientConfiguration: F[SecretsManagerServiceClientConfiguration]                                       = ???
  def serviceName: F[String]                                                                                        = ???
  def stopReplicationToReplica(a: StopReplicationToReplicaRequest): F[StopReplicationToReplicaResponse]             = ???
  def tagResource(a: TagResourceRequest): F[TagResourceResponse]                                                    = ???
  def untagResource(a: UntagResourceRequest): F[UntagResourceResponse]                                              = ???
  def updateSecret(a: UpdateSecretRequest): F[UpdateSecretResponse]                                                 = ???
  def updateSecretVersionStage(a: UpdateSecretVersionStageRequest): F[UpdateSecretVersionStageResponse]             = ???
  def validateResourcePolicy(a: ValidateResourcePolicyRequest): F[ValidateResourcePolicyResponse]                   = ???

}
