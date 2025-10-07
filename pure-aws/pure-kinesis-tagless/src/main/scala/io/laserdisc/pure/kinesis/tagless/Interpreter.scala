package io.laserdisc.pure.kinesis.tagless

// Library imports
import cats.data.Kleisli
import cats.effect.{ Async,  Resource }

import software.amazon.awssdk.services.kinesis.*
import software.amazon.awssdk.services.kinesis.model.*

// Types referenced
import software.amazon.awssdk.services.kinesis.paginators.ListStreamConsumersPublisher
import software.amazon.awssdk.services.kinesis.paginators.ListStreamsPublisher
import software.amazon.awssdk.services.kinesis.waiters.KinesisAsyncWaiter


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

  lazy val KinesisAsyncClientInterpreter: KinesisAsyncClientInterpreter = new KinesisAsyncClientInterpreter { }
  // Some methods are common to all interpreters and can be overridden to change behavior globally.

  def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(j => asyncM.blocking(f(j)))
  def primitive1[J, A](f: => A): M[A]              = asyncM.blocking(f)

  def eff[J, A](fut: J => CompletableFuture[A]): Kleisli[M, J, A] = Kleisli { j =>
    asyncM.fromCompletableFuture(asyncM.delay(fut(j)))
  }
  def eff1[J, A](fut: => CompletableFuture[A]): M[A] =
    asyncM.fromCompletableFuture(asyncM.delay(fut))

  // Interpreters
  trait KinesisAsyncClientInterpreter extends KinesisAsyncClientOp[Kleisli[M, KinesisAsyncClient, *]] {
  
    // domain-specific operations are implemented in terms of `primitive`
    override def addTagsToStream(a: AddTagsToStreamRequest): Kleisli[M, KinesisAsyncClient, AddTagsToStreamResponse] = eff(_.addTagsToStream(a)) // B
    override def close : Kleisli[M, KinesisAsyncClient, Unit] = primitive(_.close) // A
    override def createStream(a: CreateStreamRequest): Kleisli[M, KinesisAsyncClient, CreateStreamResponse] = eff(_.createStream(a)) // B
    override def decreaseStreamRetentionPeriod(a: DecreaseStreamRetentionPeriodRequest): Kleisli[M, KinesisAsyncClient, DecreaseStreamRetentionPeriodResponse] = eff(_.decreaseStreamRetentionPeriod(a)) // B
    override def deleteResourcePolicy(a: DeleteResourcePolicyRequest): Kleisli[M, KinesisAsyncClient, DeleteResourcePolicyResponse] = eff(_.deleteResourcePolicy(a)) // B
    override def deleteStream(a: DeleteStreamRequest): Kleisli[M, KinesisAsyncClient, DeleteStreamResponse] = eff(_.deleteStream(a)) // B
    override def deregisterStreamConsumer(a: DeregisterStreamConsumerRequest): Kleisli[M, KinesisAsyncClient, DeregisterStreamConsumerResponse] = eff(_.deregisterStreamConsumer(a)) // B
    override def describeLimits : Kleisli[M, KinesisAsyncClient, DescribeLimitsResponse] = eff(_.describeLimits) // A
    override def describeLimits(a: DescribeLimitsRequest): Kleisli[M, KinesisAsyncClient, DescribeLimitsResponse] = eff(_.describeLimits(a)) // B
    override def describeStream(a: DescribeStreamRequest): Kleisli[M, KinesisAsyncClient, DescribeStreamResponse] = eff(_.describeStream(a)) // B
    override def describeStreamConsumer(a: DescribeStreamConsumerRequest): Kleisli[M, KinesisAsyncClient, DescribeStreamConsumerResponse] = eff(_.describeStreamConsumer(a)) // B
    override def describeStreamSummary(a: DescribeStreamSummaryRequest): Kleisli[M, KinesisAsyncClient, DescribeStreamSummaryResponse] = eff(_.describeStreamSummary(a)) // B
    override def disableEnhancedMonitoring(a: DisableEnhancedMonitoringRequest): Kleisli[M, KinesisAsyncClient, DisableEnhancedMonitoringResponse] = eff(_.disableEnhancedMonitoring(a)) // B
    override def enableEnhancedMonitoring(a: EnableEnhancedMonitoringRequest): Kleisli[M, KinesisAsyncClient, EnableEnhancedMonitoringResponse] = eff(_.enableEnhancedMonitoring(a)) // B
    override def getRecords(a: GetRecordsRequest): Kleisli[M, KinesisAsyncClient, GetRecordsResponse] = eff(_.getRecords(a)) // B
    override def getResourcePolicy(a: GetResourcePolicyRequest): Kleisli[M, KinesisAsyncClient, GetResourcePolicyResponse] = eff(_.getResourcePolicy(a)) // B
    override def getShardIterator(a: GetShardIteratorRequest): Kleisli[M, KinesisAsyncClient, GetShardIteratorResponse] = eff(_.getShardIterator(a)) // B
    override def increaseStreamRetentionPeriod(a: IncreaseStreamRetentionPeriodRequest): Kleisli[M, KinesisAsyncClient, IncreaseStreamRetentionPeriodResponse] = eff(_.increaseStreamRetentionPeriod(a)) // B
    override def listShards(a: ListShardsRequest): Kleisli[M, KinesisAsyncClient, ListShardsResponse] = eff(_.listShards(a)) // B
    override def listStreamConsumers(a: ListStreamConsumersRequest): Kleisli[M, KinesisAsyncClient, ListStreamConsumersResponse] = eff(_.listStreamConsumers(a)) // B
    override def listStreamConsumersPaginator(a: ListStreamConsumersRequest): Kleisli[M, KinesisAsyncClient, ListStreamConsumersPublisher] = primitive(_.listStreamConsumersPaginator(a)) // B
    override def listStreams : Kleisli[M, KinesisAsyncClient, ListStreamsResponse] = eff(_.listStreams) // A
    override def listStreams(a: ListStreamsRequest): Kleisli[M, KinesisAsyncClient, ListStreamsResponse] = eff(_.listStreams(a)) // B
    override def listStreamsPaginator : Kleisli[M, KinesisAsyncClient, ListStreamsPublisher] = primitive(_.listStreamsPaginator) // A
    override def listStreamsPaginator(a: ListStreamsRequest): Kleisli[M, KinesisAsyncClient, ListStreamsPublisher] = primitive(_.listStreamsPaginator(a)) // B
    override def listTagsForResource(a: ListTagsForResourceRequest): Kleisli[M, KinesisAsyncClient, ListTagsForResourceResponse] = eff(_.listTagsForResource(a)) // B
    override def listTagsForStream(a: ListTagsForStreamRequest): Kleisli[M, KinesisAsyncClient, ListTagsForStreamResponse] = eff(_.listTagsForStream(a)) // B
    override def mergeShards(a: MergeShardsRequest): Kleisli[M, KinesisAsyncClient, MergeShardsResponse] = eff(_.mergeShards(a)) // B
    override def putRecord(a: PutRecordRequest): Kleisli[M, KinesisAsyncClient, PutRecordResponse] = eff(_.putRecord(a)) // B
    override def putRecords(a: PutRecordsRequest): Kleisli[M, KinesisAsyncClient, PutRecordsResponse] = eff(_.putRecords(a)) // B
    override def putResourcePolicy(a: PutResourcePolicyRequest): Kleisli[M, KinesisAsyncClient, PutResourcePolicyResponse] = eff(_.putResourcePolicy(a)) // B
    override def registerStreamConsumer(a: RegisterStreamConsumerRequest): Kleisli[M, KinesisAsyncClient, RegisterStreamConsumerResponse] = eff(_.registerStreamConsumer(a)) // B
    override def removeTagsFromStream(a: RemoveTagsFromStreamRequest): Kleisli[M, KinesisAsyncClient, RemoveTagsFromStreamResponse] = eff(_.removeTagsFromStream(a)) // B
    override def serviceName : Kleisli[M, KinesisAsyncClient, String] = primitive(_.serviceName) // A
    override def splitShard(a: SplitShardRequest): Kleisli[M, KinesisAsyncClient, SplitShardResponse] = eff(_.splitShard(a)) // B
    override def startStreamEncryption(a: StartStreamEncryptionRequest): Kleisli[M, KinesisAsyncClient, StartStreamEncryptionResponse] = eff(_.startStreamEncryption(a)) // B
    override def stopStreamEncryption(a: StopStreamEncryptionRequest): Kleisli[M, KinesisAsyncClient, StopStreamEncryptionResponse] = eff(_.stopStreamEncryption(a)) // B
    override def subscribeToShard(a: SubscribeToShardRequest, b: SubscribeToShardResponseHandler): Kleisli[M, KinesisAsyncClient, Void] = eff(_.subscribeToShard(a, b)) // B
    override def tagResource(a: TagResourceRequest): Kleisli[M, KinesisAsyncClient, TagResourceResponse] = eff(_.tagResource(a)) // B
    override def untagResource(a: UntagResourceRequest): Kleisli[M, KinesisAsyncClient, UntagResourceResponse] = eff(_.untagResource(a)) // B
    override def updateShardCount(a: UpdateShardCountRequest): Kleisli[M, KinesisAsyncClient, UpdateShardCountResponse] = eff(_.updateShardCount(a)) // B
    override def updateStreamMode(a: UpdateStreamModeRequest): Kleisli[M, KinesisAsyncClient, UpdateStreamModeResponse] = eff(_.updateStreamMode(a)) // B
    override def waiter : Kleisli[M, KinesisAsyncClient, KinesisAsyncWaiter] = primitive(_.waiter) // A
  
  
    def lens[E](f: E => KinesisAsyncClient): KinesisAsyncClientOp[Kleisli[M, E, *]] =
      new KinesisAsyncClientOp[Kleisli[M, E, *]] {
      override def addTagsToStream(a: AddTagsToStreamRequest) = Kleisli(e => eff1(f(e).addTagsToStream(a)))
      override def close = Kleisli(e => primitive1(f(e).close))
      override def createStream(a: CreateStreamRequest) = Kleisli(e => eff1(f(e).createStream(a)))
      override def decreaseStreamRetentionPeriod(a: DecreaseStreamRetentionPeriodRequest) = Kleisli(e => eff1(f(e).decreaseStreamRetentionPeriod(a)))
      override def deleteResourcePolicy(a: DeleteResourcePolicyRequest) = Kleisli(e => eff1(f(e).deleteResourcePolicy(a)))
      override def deleteStream(a: DeleteStreamRequest) = Kleisli(e => eff1(f(e).deleteStream(a)))
      override def deregisterStreamConsumer(a: DeregisterStreamConsumerRequest) = Kleisli(e => eff1(f(e).deregisterStreamConsumer(a)))
      override def describeLimits = Kleisli(e => eff1(f(e).describeLimits))
      override def describeLimits(a: DescribeLimitsRequest) = Kleisli(e => eff1(f(e).describeLimits(a)))
      override def describeStream(a: DescribeStreamRequest) = Kleisli(e => eff1(f(e).describeStream(a)))
      override def describeStreamConsumer(a: DescribeStreamConsumerRequest) = Kleisli(e => eff1(f(e).describeStreamConsumer(a)))
      override def describeStreamSummary(a: DescribeStreamSummaryRequest) = Kleisli(e => eff1(f(e).describeStreamSummary(a)))
      override def disableEnhancedMonitoring(a: DisableEnhancedMonitoringRequest) = Kleisli(e => eff1(f(e).disableEnhancedMonitoring(a)))
      override def enableEnhancedMonitoring(a: EnableEnhancedMonitoringRequest) = Kleisli(e => eff1(f(e).enableEnhancedMonitoring(a)))
      override def getRecords(a: GetRecordsRequest) = Kleisli(e => eff1(f(e).getRecords(a)))
      override def getResourcePolicy(a: GetResourcePolicyRequest) = Kleisli(e => eff1(f(e).getResourcePolicy(a)))
      override def getShardIterator(a: GetShardIteratorRequest) = Kleisli(e => eff1(f(e).getShardIterator(a)))
      override def increaseStreamRetentionPeriod(a: IncreaseStreamRetentionPeriodRequest) = Kleisli(e => eff1(f(e).increaseStreamRetentionPeriod(a)))
      override def listShards(a: ListShardsRequest) = Kleisli(e => eff1(f(e).listShards(a)))
      override def listStreamConsumers(a: ListStreamConsumersRequest) = Kleisli(e => eff1(f(e).listStreamConsumers(a)))
      override def listStreamConsumersPaginator(a: ListStreamConsumersRequest) = Kleisli(e => primitive1(f(e).listStreamConsumersPaginator(a)))
      override def listStreams = Kleisli(e => eff1(f(e).listStreams))
      override def listStreams(a: ListStreamsRequest) = Kleisli(e => eff1(f(e).listStreams(a)))
      override def listStreamsPaginator = Kleisli(e => primitive1(f(e).listStreamsPaginator))
      override def listStreamsPaginator(a: ListStreamsRequest) = Kleisli(e => primitive1(f(e).listStreamsPaginator(a)))
      override def listTagsForResource(a: ListTagsForResourceRequest) = Kleisli(e => eff1(f(e).listTagsForResource(a)))
      override def listTagsForStream(a: ListTagsForStreamRequest) = Kleisli(e => eff1(f(e).listTagsForStream(a)))
      override def mergeShards(a: MergeShardsRequest) = Kleisli(e => eff1(f(e).mergeShards(a)))
      override def putRecord(a: PutRecordRequest) = Kleisli(e => eff1(f(e).putRecord(a)))
      override def putRecords(a: PutRecordsRequest) = Kleisli(e => eff1(f(e).putRecords(a)))
      override def putResourcePolicy(a: PutResourcePolicyRequest) = Kleisli(e => eff1(f(e).putResourcePolicy(a)))
      override def registerStreamConsumer(a: RegisterStreamConsumerRequest) = Kleisli(e => eff1(f(e).registerStreamConsumer(a)))
      override def removeTagsFromStream(a: RemoveTagsFromStreamRequest) = Kleisli(e => eff1(f(e).removeTagsFromStream(a)))
      override def serviceName = Kleisli(e => primitive1(f(e).serviceName))
      override def splitShard(a: SplitShardRequest) = Kleisli(e => eff1(f(e).splitShard(a)))
      override def startStreamEncryption(a: StartStreamEncryptionRequest) = Kleisli(e => eff1(f(e).startStreamEncryption(a)))
      override def stopStreamEncryption(a: StopStreamEncryptionRequest) = Kleisli(e => eff1(f(e).stopStreamEncryption(a)))
      override def subscribeToShard(a: SubscribeToShardRequest, b: SubscribeToShardResponseHandler) = Kleisli(e => eff1(f(e).subscribeToShard(a, b)))
      override def tagResource(a: TagResourceRequest) = Kleisli(e => eff1(f(e).tagResource(a)))
      override def untagResource(a: UntagResourceRequest) = Kleisli(e => eff1(f(e).untagResource(a)))
      override def updateShardCount(a: UpdateShardCountRequest) = Kleisli(e => eff1(f(e).updateShardCount(a)))
      override def updateStreamMode(a: UpdateStreamModeRequest) = Kleisli(e => eff1(f(e).updateStreamMode(a)))
      override def waiter = Kleisli(e => primitive1(f(e).waiter))
  
      }
    }

  // end interpreters

  def KinesisAsyncClientResource(builder : KinesisAsyncClientBuilder) : Resource[M, KinesisAsyncClient] = Resource.fromAutoCloseable(asyncM.delay(builder.build()))
  def KinesisAsyncClientOpResource(builder: KinesisAsyncClientBuilder) = KinesisAsyncClientResource(builder).map(create)

  def create(client : KinesisAsyncClient) : KinesisAsyncClientOp[M] = new KinesisAsyncClientOp[M] {

    // domain-specific operations are implemented in terms of `primitive`
    override def addTagsToStream(a: AddTagsToStreamRequest) = eff1(client.addTagsToStream(a))
    override def close = primitive1(client.close)
    override def createStream(a: CreateStreamRequest) = eff1(client.createStream(a))
    override def decreaseStreamRetentionPeriod(a: DecreaseStreamRetentionPeriodRequest) = eff1(client.decreaseStreamRetentionPeriod(a))
    override def deleteResourcePolicy(a: DeleteResourcePolicyRequest) = eff1(client.deleteResourcePolicy(a))
    override def deleteStream(a: DeleteStreamRequest) = eff1(client.deleteStream(a))
    override def deregisterStreamConsumer(a: DeregisterStreamConsumerRequest) = eff1(client.deregisterStreamConsumer(a))
    override def describeLimits = eff1(client.describeLimits)
    override def describeLimits(a: DescribeLimitsRequest) = eff1(client.describeLimits(a))
    override def describeStream(a: DescribeStreamRequest) = eff1(client.describeStream(a))
    override def describeStreamConsumer(a: DescribeStreamConsumerRequest) = eff1(client.describeStreamConsumer(a))
    override def describeStreamSummary(a: DescribeStreamSummaryRequest) = eff1(client.describeStreamSummary(a))
    override def disableEnhancedMonitoring(a: DisableEnhancedMonitoringRequest) = eff1(client.disableEnhancedMonitoring(a))
    override def enableEnhancedMonitoring(a: EnableEnhancedMonitoringRequest) = eff1(client.enableEnhancedMonitoring(a))
    override def getRecords(a: GetRecordsRequest) = eff1(client.getRecords(a))
    override def getResourcePolicy(a: GetResourcePolicyRequest) = eff1(client.getResourcePolicy(a))
    override def getShardIterator(a: GetShardIteratorRequest) = eff1(client.getShardIterator(a))
    override def increaseStreamRetentionPeriod(a: IncreaseStreamRetentionPeriodRequest) = eff1(client.increaseStreamRetentionPeriod(a))
    override def listShards(a: ListShardsRequest) = eff1(client.listShards(a))
    override def listStreamConsumers(a: ListStreamConsumersRequest) = eff1(client.listStreamConsumers(a))
    override def listStreamConsumersPaginator(a: ListStreamConsumersRequest) = primitive1(client.listStreamConsumersPaginator(a))
    override def listStreams = eff1(client.listStreams)
    override def listStreams(a: ListStreamsRequest) = eff1(client.listStreams(a))
    override def listStreamsPaginator = primitive1(client.listStreamsPaginator)
    override def listStreamsPaginator(a: ListStreamsRequest) = primitive1(client.listStreamsPaginator(a))
    override def listTagsForResource(a: ListTagsForResourceRequest) = eff1(client.listTagsForResource(a))
    override def listTagsForStream(a: ListTagsForStreamRequest) = eff1(client.listTagsForStream(a))
    override def mergeShards(a: MergeShardsRequest) = eff1(client.mergeShards(a))
    override def putRecord(a: PutRecordRequest) = eff1(client.putRecord(a))
    override def putRecords(a: PutRecordsRequest) = eff1(client.putRecords(a))
    override def putResourcePolicy(a: PutResourcePolicyRequest) = eff1(client.putResourcePolicy(a))
    override def registerStreamConsumer(a: RegisterStreamConsumerRequest) = eff1(client.registerStreamConsumer(a))
    override def removeTagsFromStream(a: RemoveTagsFromStreamRequest) = eff1(client.removeTagsFromStream(a))
    override def serviceName = primitive1(client.serviceName)
    override def splitShard(a: SplitShardRequest) = eff1(client.splitShard(a))
    override def startStreamEncryption(a: StartStreamEncryptionRequest) = eff1(client.startStreamEncryption(a))
    override def stopStreamEncryption(a: StopStreamEncryptionRequest) = eff1(client.stopStreamEncryption(a))
    override def subscribeToShard(a: SubscribeToShardRequest, b: SubscribeToShardResponseHandler) = eff1(client.subscribeToShard(a, b))
    override def tagResource(a: TagResourceRequest) = eff1(client.tagResource(a))
    override def untagResource(a: UntagResourceRequest) = eff1(client.untagResource(a))
    override def updateShardCount(a: UpdateShardCountRequest) = eff1(client.updateShardCount(a))
    override def updateStreamMode(a: UpdateStreamModeRequest) = eff1(client.updateStreamMode(a))
    override def waiter = primitive1(client.waiter)


  }


}

