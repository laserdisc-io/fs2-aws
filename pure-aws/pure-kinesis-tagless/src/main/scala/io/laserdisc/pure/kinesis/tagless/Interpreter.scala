package io.laserdisc.pure.kinesis.tagless

// Library imports
import cats.data.Kleisli
import cats.effect.{Async, Resource}

import software.amazon.awssdk.services.kinesis.*
import software.amazon.awssdk.services.kinesis.model.*

// Types referenced
import software.amazon.awssdk.services.kinesis.paginators.ListStreamConsumersPublisher
import software.amazon.awssdk.services.kinesis.paginators.ListStreamsPublisher
import software.amazon.awssdk.services.kinesis.waiters.KinesisAsyncWaiter

object Interpreter {

  def apply[M[_]](implicit
      am: Async[M]
  ): Interpreter[M] =
    new Interpreter[M] {
      val asyncM = am
    }

}

// Family of interpreters into Kleisli arrows for some monad M.
trait Interpreter[M[_]] { outer =>

  import java.util.concurrent.CompletableFuture

  implicit val asyncM: Async[M]

  lazy val KinesisAsyncClientInterpreter: KinesisAsyncClientInterpreter = new KinesisAsyncClientInterpreter {}
  // Some methods are common to all interpreters and can be overridden to change behavior globally.

  private def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(j => asyncM.blocking(f(j)))
  private def primitive1[J, A](f: => A): M[A]              = asyncM.blocking(f)

  private def eff[J, A](fut: J => CompletableFuture[A]): Kleisli[M, J, A] = Kleisli { j =>
    asyncM.fromCompletableFuture(asyncM.delay(fut(j)))
  }
  private def eff1[J, A](fut: => CompletableFuture[A]): M[A] =
    asyncM.fromCompletableFuture(asyncM.delay(fut))

  // Interpreters // scalafmt: off
  trait KinesisAsyncClientInterpreter extends KinesisAsyncClientOp[Kleisli[M, KinesisAsyncClient, *]] {

    // domain-specific operations are implemented in terms of `primitive`
    override def addTagsToStream(a: AddTagsToStreamRequest): Kleisli[M, KinesisAsyncClient, AddTagsToStreamResponse]                                           = eff(_.addTagsToStream(a))
    override def close: Kleisli[M, KinesisAsyncClient, Unit]                                                                                                   = primitive(_.close)
    override def createStream(a: CreateStreamRequest): Kleisli[M, KinesisAsyncClient, CreateStreamResponse]                                                    = eff(_.createStream(a))
    override def decreaseStreamRetentionPeriod(a: DecreaseStreamRetentionPeriodRequest): Kleisli[M, KinesisAsyncClient, DecreaseStreamRetentionPeriodResponse] = eff(_.decreaseStreamRetentionPeriod(a))
    override def deleteResourcePolicy(a: DeleteResourcePolicyRequest): Kleisli[M, KinesisAsyncClient, DeleteResourcePolicyResponse]                            = eff(_.deleteResourcePolicy(a))
    override def deleteStream(a: DeleteStreamRequest): Kleisli[M, KinesisAsyncClient, DeleteStreamResponse]                                                    = eff(_.deleteStream(a))
    override def deregisterStreamConsumer(a: DeregisterStreamConsumerRequest): Kleisli[M, KinesisAsyncClient, DeregisterStreamConsumerResponse]                = eff(_.deregisterStreamConsumer(a))
    override def describeLimits: Kleisli[M, KinesisAsyncClient, DescribeLimitsResponse]                                                                        = eff(_.describeLimits)
    override def describeLimits(a: DescribeLimitsRequest): Kleisli[M, KinesisAsyncClient, DescribeLimitsResponse]                                              = eff(_.describeLimits(a))
    override def describeStream(a: DescribeStreamRequest): Kleisli[M, KinesisAsyncClient, DescribeStreamResponse]                                              = eff(_.describeStream(a))
    override def describeStreamConsumer(a: DescribeStreamConsumerRequest): Kleisli[M, KinesisAsyncClient, DescribeStreamConsumerResponse]                      = eff(_.describeStreamConsumer(a))
    override def describeStreamSummary(a: DescribeStreamSummaryRequest): Kleisli[M, KinesisAsyncClient, DescribeStreamSummaryResponse]                         = eff(_.describeStreamSummary(a))
    override def disableEnhancedMonitoring(a: DisableEnhancedMonitoringRequest): Kleisli[M, KinesisAsyncClient, DisableEnhancedMonitoringResponse]             = eff(_.disableEnhancedMonitoring(a))
    override def enableEnhancedMonitoring(a: EnableEnhancedMonitoringRequest): Kleisli[M, KinesisAsyncClient, EnableEnhancedMonitoringResponse]                = eff(_.enableEnhancedMonitoring(a))
    override def getRecords(a: GetRecordsRequest): Kleisli[M, KinesisAsyncClient, GetRecordsResponse]                                                          = eff(_.getRecords(a))
    override def getResourcePolicy(a: GetResourcePolicyRequest): Kleisli[M, KinesisAsyncClient, GetResourcePolicyResponse]                                     = eff(_.getResourcePolicy(a))
    override def getShardIterator(a: GetShardIteratorRequest): Kleisli[M, KinesisAsyncClient, GetShardIteratorResponse]                                        = eff(_.getShardIterator(a))
    override def increaseStreamRetentionPeriod(a: IncreaseStreamRetentionPeriodRequest): Kleisli[M, KinesisAsyncClient, IncreaseStreamRetentionPeriodResponse] = eff(_.increaseStreamRetentionPeriod(a))
    override def listShards(a: ListShardsRequest): Kleisli[M, KinesisAsyncClient, ListShardsResponse]                                                          = eff(_.listShards(a))
    override def listStreamConsumers(a: ListStreamConsumersRequest): Kleisli[M, KinesisAsyncClient, ListStreamConsumersResponse]                               = eff(_.listStreamConsumers(a))
    override def listStreamConsumersPaginator(a: ListStreamConsumersRequest): Kleisli[M, KinesisAsyncClient, ListStreamConsumersPublisher]                     = primitive(_.listStreamConsumersPaginator(a))
    override def listStreams: Kleisli[M, KinesisAsyncClient, ListStreamsResponse]                                                                              = eff(_.listStreams)
    override def listStreams(a: ListStreamsRequest): Kleisli[M, KinesisAsyncClient, ListStreamsResponse]                                                       = eff(_.listStreams(a))
    override def listStreamsPaginator: Kleisli[M, KinesisAsyncClient, ListStreamsPublisher]                                                                    = primitive(_.listStreamsPaginator)
    override def listStreamsPaginator(a: ListStreamsRequest): Kleisli[M, KinesisAsyncClient, ListStreamsPublisher]                                             = primitive(_.listStreamsPaginator(a))
    override def listTagsForResource(a: ListTagsForResourceRequest): Kleisli[M, KinesisAsyncClient, ListTagsForResourceResponse]                               = eff(_.listTagsForResource(a))
    override def listTagsForStream(a: ListTagsForStreamRequest): Kleisli[M, KinesisAsyncClient, ListTagsForStreamResponse]                                     = eff(_.listTagsForStream(a))
    override def mergeShards(a: MergeShardsRequest): Kleisli[M, KinesisAsyncClient, MergeShardsResponse]                                                       = eff(_.mergeShards(a))
    override def putRecord(a: PutRecordRequest): Kleisli[M, KinesisAsyncClient, PutRecordResponse]                                                             = eff(_.putRecord(a))
    override def putRecords(a: PutRecordsRequest): Kleisli[M, KinesisAsyncClient, PutRecordsResponse]                                                          = eff(_.putRecords(a))
    override def putResourcePolicy(a: PutResourcePolicyRequest): Kleisli[M, KinesisAsyncClient, PutResourcePolicyResponse]                                     = eff(_.putResourcePolicy(a))
    override def registerStreamConsumer(a: RegisterStreamConsumerRequest): Kleisli[M, KinesisAsyncClient, RegisterStreamConsumerResponse]                      = eff(_.registerStreamConsumer(a))
    override def removeTagsFromStream(a: RemoveTagsFromStreamRequest): Kleisli[M, KinesisAsyncClient, RemoveTagsFromStreamResponse]                            = eff(_.removeTagsFromStream(a))
    override def serviceName: Kleisli[M, KinesisAsyncClient, String]                                                                                           = primitive(_.serviceName)
    override def splitShard(a: SplitShardRequest): Kleisli[M, KinesisAsyncClient, SplitShardResponse]                                                          = eff(_.splitShard(a))
    override def startStreamEncryption(a: StartStreamEncryptionRequest): Kleisli[M, KinesisAsyncClient, StartStreamEncryptionResponse]                         = eff(_.startStreamEncryption(a))
    override def stopStreamEncryption(a: StopStreamEncryptionRequest): Kleisli[M, KinesisAsyncClient, StopStreamEncryptionResponse]                            = eff(_.stopStreamEncryption(a))
    override def subscribeToShard(a: SubscribeToShardRequest, b: SubscribeToShardResponseHandler): Kleisli[M, KinesisAsyncClient, Void]                        = eff(_.subscribeToShard(a, b))
    override def tagResource(a: TagResourceRequest): Kleisli[M, KinesisAsyncClient, TagResourceResponse]                                                       = eff(_.tagResource(a))
    override def untagResource(a: UntagResourceRequest): Kleisli[M, KinesisAsyncClient, UntagResourceResponse]                                                 = eff(_.untagResource(a))
    override def updateShardCount(a: UpdateShardCountRequest): Kleisli[M, KinesisAsyncClient, UpdateShardCountResponse]                                        = eff(_.updateShardCount(a))
    override def updateStreamMode(a: UpdateStreamModeRequest): Kleisli[M, KinesisAsyncClient, UpdateStreamModeResponse]                                        = eff(_.updateStreamMode(a))
    override def waiter: Kleisli[M, KinesisAsyncClient, KinesisAsyncWaiter]                                                                                    = primitive(_.waiter)

    def lens[E](f: E => KinesisAsyncClient): KinesisAsyncClientOp[Kleisli[M, E, *]] =
      new KinesisAsyncClientOp[Kleisli[M, E, *]] {
        override def addTagsToStream(a: AddTagsToStreamRequest): Kleisli[M, E, AddTagsToStreamResponse]                                           = Kleisli(e => eff1(f(e).addTagsToStream(a)))
        override def close: Kleisli[M, E, Unit]                                                                                                   = Kleisli(e => primitive1(f(e).close))
        override def createStream(a: CreateStreamRequest): Kleisli[M, E, CreateStreamResponse]                                                    = Kleisli(e => eff1(f(e).createStream(a)))
        override def decreaseStreamRetentionPeriod(a: DecreaseStreamRetentionPeriodRequest): Kleisli[M, E, DecreaseStreamRetentionPeriodResponse] = Kleisli(e => eff1(f(e).decreaseStreamRetentionPeriod(a)))
        override def deleteResourcePolicy(a: DeleteResourcePolicyRequest): Kleisli[M, E, DeleteResourcePolicyResponse]                            = Kleisli(e => eff1(f(e).deleteResourcePolicy(a)))
        override def deleteStream(a: DeleteStreamRequest): Kleisli[M, E, DeleteStreamResponse]                                                    = Kleisli(e => eff1(f(e).deleteStream(a)))
        override def deregisterStreamConsumer(a: DeregisterStreamConsumerRequest): Kleisli[M, E, DeregisterStreamConsumerResponse]                = Kleisli(e => eff1(f(e).deregisterStreamConsumer(a)))
        override def describeLimits: Kleisli[M, E, DescribeLimitsResponse]                                                                        = Kleisli(e => eff1(f(e).describeLimits))
        override def describeLimits(a: DescribeLimitsRequest): Kleisli[M, E, DescribeLimitsResponse]                                              = Kleisli(e => eff1(f(e).describeLimits(a)))
        override def describeStream(a: DescribeStreamRequest): Kleisli[M, E, DescribeStreamResponse]                                              = Kleisli(e => eff1(f(e).describeStream(a)))
        override def describeStreamConsumer(a: DescribeStreamConsumerRequest): Kleisli[M, E, DescribeStreamConsumerResponse]                      = Kleisli(e => eff1(f(e).describeStreamConsumer(a)))
        override def describeStreamSummary(a: DescribeStreamSummaryRequest): Kleisli[M, E, DescribeStreamSummaryResponse]                         = Kleisli(e => eff1(f(e).describeStreamSummary(a)))
        override def disableEnhancedMonitoring(a: DisableEnhancedMonitoringRequest): Kleisli[M, E, DisableEnhancedMonitoringResponse]             = Kleisli(e => eff1(f(e).disableEnhancedMonitoring(a)))
        override def enableEnhancedMonitoring(a: EnableEnhancedMonitoringRequest): Kleisli[M, E, EnableEnhancedMonitoringResponse]                = Kleisli(e => eff1(f(e).enableEnhancedMonitoring(a)))
        override def getRecords(a: GetRecordsRequest): Kleisli[M, E, GetRecordsResponse]                                                          = Kleisli(e => eff1(f(e).getRecords(a)))
        override def getResourcePolicy(a: GetResourcePolicyRequest): Kleisli[M, E, GetResourcePolicyResponse]                                     = Kleisli(e => eff1(f(e).getResourcePolicy(a)))
        override def getShardIterator(a: GetShardIteratorRequest): Kleisli[M, E, GetShardIteratorResponse]                                        = Kleisli(e => eff1(f(e).getShardIterator(a)))
        override def increaseStreamRetentionPeriod(a: IncreaseStreamRetentionPeriodRequest): Kleisli[M, E, IncreaseStreamRetentionPeriodResponse] = Kleisli(e => eff1(f(e).increaseStreamRetentionPeriod(a)))
        override def listShards(a: ListShardsRequest): Kleisli[M, E, ListShardsResponse]                                                          = Kleisli(e => eff1(f(e).listShards(a)))
        override def listStreamConsumers(a: ListStreamConsumersRequest): Kleisli[M, E, ListStreamConsumersResponse]                               = Kleisli(e => eff1(f(e).listStreamConsumers(a)))
        override def listStreamConsumersPaginator(a: ListStreamConsumersRequest): Kleisli[M, E, ListStreamConsumersPublisher]                     = Kleisli(e => primitive1(f(e).listStreamConsumersPaginator(a)))
        override def listStreams: Kleisli[M, E, ListStreamsResponse]                                                                              = Kleisli(e => eff1(f(e).listStreams))
        override def listStreams(a: ListStreamsRequest): Kleisli[M, E, ListStreamsResponse]                                                       = Kleisli(e => eff1(f(e).listStreams(a)))
        override def listStreamsPaginator: Kleisli[M, E, ListStreamsPublisher]                                                                    = Kleisli(e => primitive1(f(e).listStreamsPaginator))
        override def listStreamsPaginator(a: ListStreamsRequest): Kleisli[M, E, ListStreamsPublisher]                                             = Kleisli(e => primitive1(f(e).listStreamsPaginator(a)))
        override def listTagsForResource(a: ListTagsForResourceRequest): Kleisli[M, E, ListTagsForResourceResponse]                               = Kleisli(e => eff1(f(e).listTagsForResource(a)))
        override def listTagsForStream(a: ListTagsForStreamRequest): Kleisli[M, E, ListTagsForStreamResponse]                                     = Kleisli(e => eff1(f(e).listTagsForStream(a)))
        override def mergeShards(a: MergeShardsRequest): Kleisli[M, E, MergeShardsResponse]                                                       = Kleisli(e => eff1(f(e).mergeShards(a)))
        override def putRecord(a: PutRecordRequest): Kleisli[M, E, PutRecordResponse]                                                             = Kleisli(e => eff1(f(e).putRecord(a)))
        override def putRecords(a: PutRecordsRequest): Kleisli[M, E, PutRecordsResponse]                                                          = Kleisli(e => eff1(f(e).putRecords(a)))
        override def putResourcePolicy(a: PutResourcePolicyRequest): Kleisli[M, E, PutResourcePolicyResponse]                                     = Kleisli(e => eff1(f(e).putResourcePolicy(a)))
        override def registerStreamConsumer(a: RegisterStreamConsumerRequest): Kleisli[M, E, RegisterStreamConsumerResponse]                      = Kleisli(e => eff1(f(e).registerStreamConsumer(a)))
        override def removeTagsFromStream(a: RemoveTagsFromStreamRequest): Kleisli[M, E, RemoveTagsFromStreamResponse]                            = Kleisli(e => eff1(f(e).removeTagsFromStream(a)))
        override def serviceName: Kleisli[M, E, String]                                                                                           = Kleisli(e => primitive1(f(e).serviceName))
        override def splitShard(a: SplitShardRequest): Kleisli[M, E, SplitShardResponse]                                                          = Kleisli(e => eff1(f(e).splitShard(a)))
        override def startStreamEncryption(a: StartStreamEncryptionRequest): Kleisli[M, E, StartStreamEncryptionResponse]                         = Kleisli(e => eff1(f(e).startStreamEncryption(a)))
        override def stopStreamEncryption(a: StopStreamEncryptionRequest): Kleisli[M, E, StopStreamEncryptionResponse]                            = Kleisli(e => eff1(f(e).stopStreamEncryption(a)))
        override def subscribeToShard(a: SubscribeToShardRequest, b: SubscribeToShardResponseHandler): Kleisli[M, E, Void]                        = Kleisli(e => eff1(f(e).subscribeToShard(a, b)))
        override def tagResource(a: TagResourceRequest): Kleisli[M, E, TagResourceResponse]                                                       = Kleisli(e => eff1(f(e).tagResource(a)))
        override def untagResource(a: UntagResourceRequest): Kleisli[M, E, UntagResourceResponse]                                                 = Kleisli(e => eff1(f(e).untagResource(a)))
        override def updateShardCount(a: UpdateShardCountRequest): Kleisli[M, E, UpdateShardCountResponse]                                        = Kleisli(e => eff1(f(e).updateShardCount(a)))
        override def updateStreamMode(a: UpdateStreamModeRequest): Kleisli[M, E, UpdateStreamModeResponse]                                        = Kleisli(e => eff1(f(e).updateStreamMode(a)))
        override def waiter: Kleisli[M, E, KinesisAsyncWaiter]                                                                                    = Kleisli(e => primitive1(f(e).waiter))
      }
  }
  // end interpreters

  def KinesisAsyncClientResource(builder: KinesisAsyncClientBuilder): Resource[M, KinesisAsyncClient]        = Resource.fromAutoCloseable(asyncM.delay(builder.build()))
  def KinesisAsyncClientOpResource(builder: KinesisAsyncClientBuilder): Resource[M, KinesisAsyncClientOp[M]] = KinesisAsyncClientResource(builder).map(create)

  def create(client: KinesisAsyncClient): KinesisAsyncClientOp[M] = new KinesisAsyncClientOp[M] {

    // domain-specific operations are implemented in terms of `primitive`
    override def addTagsToStream(a: AddTagsToStreamRequest): M[AddTagsToStreamResponse]                                           = eff1(client.addTagsToStream(a))
    override def close: M[Unit]                                                                                                   = primitive1(client.close)
    override def createStream(a: CreateStreamRequest): M[CreateStreamResponse]                                                    = eff1(client.createStream(a))
    override def decreaseStreamRetentionPeriod(a: DecreaseStreamRetentionPeriodRequest): M[DecreaseStreamRetentionPeriodResponse] = eff1(client.decreaseStreamRetentionPeriod(a))
    override def deleteResourcePolicy(a: DeleteResourcePolicyRequest): M[DeleteResourcePolicyResponse]                            = eff1(client.deleteResourcePolicy(a))
    override def deleteStream(a: DeleteStreamRequest): M[DeleteStreamResponse]                                                    = eff1(client.deleteStream(a))
    override def deregisterStreamConsumer(a: DeregisterStreamConsumerRequest): M[DeregisterStreamConsumerResponse]                = eff1(client.deregisterStreamConsumer(a))
    override def describeLimits: M[DescribeLimitsResponse]                                                                        = eff1(client.describeLimits)
    override def describeLimits(a: DescribeLimitsRequest): M[DescribeLimitsResponse]                                              = eff1(client.describeLimits(a))
    override def describeStream(a: DescribeStreamRequest): M[DescribeStreamResponse]                                              = eff1(client.describeStream(a))
    override def describeStreamConsumer(a: DescribeStreamConsumerRequest): M[DescribeStreamConsumerResponse]                      = eff1(client.describeStreamConsumer(a))
    override def describeStreamSummary(a: DescribeStreamSummaryRequest): M[DescribeStreamSummaryResponse]                         = eff1(client.describeStreamSummary(a))
    override def disableEnhancedMonitoring(a: DisableEnhancedMonitoringRequest): M[DisableEnhancedMonitoringResponse]             = eff1(client.disableEnhancedMonitoring(a))
    override def enableEnhancedMonitoring(a: EnableEnhancedMonitoringRequest): M[EnableEnhancedMonitoringResponse]                = eff1(client.enableEnhancedMonitoring(a))
    override def getRecords(a: GetRecordsRequest): M[GetRecordsResponse]                                                          = eff1(client.getRecords(a))
    override def getResourcePolicy(a: GetResourcePolicyRequest): M[GetResourcePolicyResponse]                                     = eff1(client.getResourcePolicy(a))
    override def getShardIterator(a: GetShardIteratorRequest): M[GetShardIteratorResponse]                                        = eff1(client.getShardIterator(a))
    override def increaseStreamRetentionPeriod(a: IncreaseStreamRetentionPeriodRequest): M[IncreaseStreamRetentionPeriodResponse] = eff1(client.increaseStreamRetentionPeriod(a))
    override def listShards(a: ListShardsRequest): M[ListShardsResponse]                                                          = eff1(client.listShards(a))
    override def listStreamConsumers(a: ListStreamConsumersRequest): M[ListStreamConsumersResponse]                               = eff1(client.listStreamConsumers(a))
    override def listStreamConsumersPaginator(a: ListStreamConsumersRequest): M[ListStreamConsumersPublisher]                     = primitive1(client.listStreamConsumersPaginator(a))
    override def listStreams: M[ListStreamsResponse]                                                                              = eff1(client.listStreams)
    override def listStreams(a: ListStreamsRequest): M[ListStreamsResponse]                                                       = eff1(client.listStreams(a))
    override def listStreamsPaginator: M[ListStreamsPublisher]                                                                    = primitive1(client.listStreamsPaginator)
    override def listStreamsPaginator(a: ListStreamsRequest): M[ListStreamsPublisher]                                             = primitive1(client.listStreamsPaginator(a))
    override def listTagsForResource(a: ListTagsForResourceRequest): M[ListTagsForResourceResponse]                               = eff1(client.listTagsForResource(a))
    override def listTagsForStream(a: ListTagsForStreamRequest): M[ListTagsForStreamResponse]                                     = eff1(client.listTagsForStream(a))
    override def mergeShards(a: MergeShardsRequest): M[MergeShardsResponse]                                                       = eff1(client.mergeShards(a))
    override def putRecord(a: PutRecordRequest): M[PutRecordResponse]                                                             = eff1(client.putRecord(a))
    override def putRecords(a: PutRecordsRequest): M[PutRecordsResponse]                                                          = eff1(client.putRecords(a))
    override def putResourcePolicy(a: PutResourcePolicyRequest): M[PutResourcePolicyResponse]                                     = eff1(client.putResourcePolicy(a))
    override def registerStreamConsumer(a: RegisterStreamConsumerRequest): M[RegisterStreamConsumerResponse]                      = eff1(client.registerStreamConsumer(a))
    override def removeTagsFromStream(a: RemoveTagsFromStreamRequest): M[RemoveTagsFromStreamResponse]                            = eff1(client.removeTagsFromStream(a))
    override def serviceName: M[String]                                                                                           = primitive1(client.serviceName)
    override def splitShard(a: SplitShardRequest): M[SplitShardResponse]                                                          = eff1(client.splitShard(a))
    override def startStreamEncryption(a: StartStreamEncryptionRequest): M[StartStreamEncryptionResponse]                         = eff1(client.startStreamEncryption(a))
    override def stopStreamEncryption(a: StopStreamEncryptionRequest): M[StopStreamEncryptionResponse]                            = eff1(client.stopStreamEncryption(a))
    override def subscribeToShard(a: SubscribeToShardRequest, b: SubscribeToShardResponseHandler): M[Void]                        = eff1(client.subscribeToShard(a, b))
    override def tagResource(a: TagResourceRequest): M[TagResourceResponse]                                                       = eff1(client.tagResource(a))
    override def untagResource(a: UntagResourceRequest): M[UntagResourceResponse]                                                 = eff1(client.untagResource(a))
    override def updateShardCount(a: UpdateShardCountRequest): M[UpdateShardCountResponse]                                        = eff1(client.updateShardCount(a))
    override def updateStreamMode(a: UpdateStreamModeRequest): M[UpdateStreamModeResponse]                                        = eff1(client.updateStreamMode(a))
    override def waiter: M[KinesisAsyncWaiter]                                                                                    = primitive1(client.waiter)

  }

}
