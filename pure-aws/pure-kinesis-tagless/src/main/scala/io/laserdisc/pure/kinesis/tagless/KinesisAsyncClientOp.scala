package io.laserdisc.pure.kinesis.tagless

import software.amazon.awssdk.services.kinesis.KinesisServiceClientConfiguration
import software.amazon.awssdk.services.kinesis.model.*
import software.amazon.awssdk.services.kinesis.paginators.{ListStreamConsumersPublisher, ListStreamsPublisher}
import software.amazon.awssdk.services.kinesis.waiters.KinesisAsyncWaiter

trait KinesisAsyncClientOp[F[_]] {
  // KinesisAsyncClient
  def addTagsToStream(a: AddTagsToStreamRequest): F[AddTagsToStreamResponse]
  def close: F[Unit]
  def createStream(a: CreateStreamRequest): F[CreateStreamResponse]
  def decreaseStreamRetentionPeriod(a: DecreaseStreamRetentionPeriodRequest): F[DecreaseStreamRetentionPeriodResponse]
  def deleteResourcePolicy(a: DeleteResourcePolicyRequest): F[DeleteResourcePolicyResponse]
  def deleteStream(a: DeleteStreamRequest): F[DeleteStreamResponse]
  def deregisterStreamConsumer(a: DeregisterStreamConsumerRequest): F[DeregisterStreamConsumerResponse]
  def describeLimits: F[DescribeLimitsResponse]
  def describeLimits(a: DescribeLimitsRequest): F[DescribeLimitsResponse]
  def describeStream(a: DescribeStreamRequest): F[DescribeStreamResponse]
  def describeStreamConsumer(a: DescribeStreamConsumerRequest): F[DescribeStreamConsumerResponse]
  def describeStreamSummary(a: DescribeStreamSummaryRequest): F[DescribeStreamSummaryResponse]
  def disableEnhancedMonitoring(a: DisableEnhancedMonitoringRequest): F[DisableEnhancedMonitoringResponse]
  def enableEnhancedMonitoring(a: EnableEnhancedMonitoringRequest): F[EnableEnhancedMonitoringResponse]
  def getRecords(a: GetRecordsRequest): F[GetRecordsResponse]
  def getResourcePolicy(a: GetResourcePolicyRequest): F[GetResourcePolicyResponse]
  def getShardIterator(a: GetShardIteratorRequest): F[GetShardIteratorResponse]
  def increaseStreamRetentionPeriod(a: IncreaseStreamRetentionPeriodRequest): F[IncreaseStreamRetentionPeriodResponse]
  def listShards(a: ListShardsRequest): F[ListShardsResponse]
  def listStreamConsumers(a: ListStreamConsumersRequest): F[ListStreamConsumersResponse]
  def listStreamConsumersPaginator(a: ListStreamConsumersRequest): F[ListStreamConsumersPublisher]
  def listStreams: F[ListStreamsResponse]
  def listStreams(a: ListStreamsRequest): F[ListStreamsResponse]
  def listStreamsPaginator: F[ListStreamsPublisher]
  def listStreamsPaginator(a: ListStreamsRequest): F[ListStreamsPublisher]
  def listTagsForStream(a: ListTagsForStreamRequest): F[ListTagsForStreamResponse]
  def mergeShards(a: MergeShardsRequest): F[MergeShardsResponse]
  def putRecord(a: PutRecordRequest): F[PutRecordResponse]
  def putRecords(a: PutRecordsRequest): F[PutRecordsResponse]
  def putResourcePolicy(a: PutResourcePolicyRequest): F[PutResourcePolicyResponse]
  def registerStreamConsumer(a: RegisterStreamConsumerRequest): F[RegisterStreamConsumerResponse]
  def removeTagsFromStream(a: RemoveTagsFromStreamRequest): F[RemoveTagsFromStreamResponse]
  def serviceClientConfiguration: F[KinesisServiceClientConfiguration]
  def serviceName: F[String]
  def splitShard(a: SplitShardRequest): F[SplitShardResponse]
  def startStreamEncryption(a: StartStreamEncryptionRequest): F[StartStreamEncryptionResponse]
  def stopStreamEncryption(a: StopStreamEncryptionRequest): F[StopStreamEncryptionResponse]
  def subscribeToShard(a: SubscribeToShardRequest, b: SubscribeToShardResponseHandler): F[Void]
  def updateShardCount(a: UpdateShardCountRequest): F[UpdateShardCountResponse]
  def updateStreamMode(a: UpdateStreamModeRequest): F[UpdateStreamModeResponse]
  def waiter: F[KinesisAsyncWaiter]

}
