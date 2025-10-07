package io.laserdisc.pure.cloudwatch.tagless

// Library imports
import cats.data.Kleisli
import cats.effect.{ Async,  Resource }

import software.amazon.awssdk.services.cloudwatch.*
import software.amazon.awssdk.services.cloudwatch.model.*

// Types referenced
import software.amazon.awssdk.services.cloudwatch.paginators.DescribeAlarmHistoryPublisher
import software.amazon.awssdk.services.cloudwatch.paginators.DescribeAlarmsPublisher
import software.amazon.awssdk.services.cloudwatch.paginators.DescribeAnomalyDetectorsPublisher
import software.amazon.awssdk.services.cloudwatch.paginators.DescribeInsightRulesPublisher
import software.amazon.awssdk.services.cloudwatch.paginators.GetMetricDataPublisher
import software.amazon.awssdk.services.cloudwatch.paginators.ListDashboardsPublisher
import software.amazon.awssdk.services.cloudwatch.paginators.ListManagedInsightRulesPublisher
import software.amazon.awssdk.services.cloudwatch.paginators.ListMetricStreamsPublisher
import software.amazon.awssdk.services.cloudwatch.paginators.ListMetricsPublisher
import software.amazon.awssdk.services.cloudwatch.waiters.CloudWatchAsyncWaiter


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

  lazy val CloudWatchAsyncClientInterpreter: CloudWatchAsyncClientInterpreter = new CloudWatchAsyncClientInterpreter { }
  // Some methods are common to all interpreters and can be overridden to change behavior globally.

  def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(j => asyncM.blocking(f(j)))
  def primitive1[J, A](f: => A): M[A]              = asyncM.blocking(f)

  def eff[J, A](fut: J => CompletableFuture[A]): Kleisli[M, J, A] = Kleisli { j =>
    asyncM.fromCompletableFuture(asyncM.delay(fut(j)))
  }
  def eff1[J, A](fut: => CompletableFuture[A]): M[A] =
    asyncM.fromCompletableFuture(asyncM.delay(fut))

  // Interpreters
  trait CloudWatchAsyncClientInterpreter extends CloudWatchAsyncClientOp[Kleisli[M, CloudWatchAsyncClient, *]] {
  
    // domain-specific operations are implemented in terms of `primitive`
    override def close : Kleisli[M, CloudWatchAsyncClient, Unit] = primitive(_.close) // A
    override def deleteAlarms(a: DeleteAlarmsRequest): Kleisli[M, CloudWatchAsyncClient, DeleteAlarmsResponse] = eff(_.deleteAlarms(a)) // B
    override def deleteAnomalyDetector(a: DeleteAnomalyDetectorRequest): Kleisli[M, CloudWatchAsyncClient, DeleteAnomalyDetectorResponse] = eff(_.deleteAnomalyDetector(a)) // B
    override def deleteDashboards(a: DeleteDashboardsRequest): Kleisli[M, CloudWatchAsyncClient, DeleteDashboardsResponse] = eff(_.deleteDashboards(a)) // B
    override def deleteInsightRules(a: DeleteInsightRulesRequest): Kleisli[M, CloudWatchAsyncClient, DeleteInsightRulesResponse] = eff(_.deleteInsightRules(a)) // B
    override def deleteMetricStream(a: DeleteMetricStreamRequest): Kleisli[M, CloudWatchAsyncClient, DeleteMetricStreamResponse] = eff(_.deleteMetricStream(a)) // B
    override def describeAlarmContributors(a: DescribeAlarmContributorsRequest): Kleisli[M, CloudWatchAsyncClient, DescribeAlarmContributorsResponse] = eff(_.describeAlarmContributors(a)) // B
    override def describeAlarmHistory : Kleisli[M, CloudWatchAsyncClient, DescribeAlarmHistoryResponse] = eff(_.describeAlarmHistory) // A
    override def describeAlarmHistory(a: DescribeAlarmHistoryRequest): Kleisli[M, CloudWatchAsyncClient, DescribeAlarmHistoryResponse] = eff(_.describeAlarmHistory(a)) // B
    override def describeAlarmHistoryPaginator : Kleisli[M, CloudWatchAsyncClient, DescribeAlarmHistoryPublisher] = primitive(_.describeAlarmHistoryPaginator) // A
    override def describeAlarmHistoryPaginator(a: DescribeAlarmHistoryRequest): Kleisli[M, CloudWatchAsyncClient, DescribeAlarmHistoryPublisher] = primitive(_.describeAlarmHistoryPaginator(a)) // B
    override def describeAlarms : Kleisli[M, CloudWatchAsyncClient, DescribeAlarmsResponse] = eff(_.describeAlarms) // A
    override def describeAlarms(a: DescribeAlarmsRequest): Kleisli[M, CloudWatchAsyncClient, DescribeAlarmsResponse] = eff(_.describeAlarms(a)) // B
    override def describeAlarmsForMetric(a: DescribeAlarmsForMetricRequest): Kleisli[M, CloudWatchAsyncClient, DescribeAlarmsForMetricResponse] = eff(_.describeAlarmsForMetric(a)) // B
    override def describeAlarmsPaginator : Kleisli[M, CloudWatchAsyncClient, DescribeAlarmsPublisher] = primitive(_.describeAlarmsPaginator) // A
    override def describeAlarmsPaginator(a: DescribeAlarmsRequest): Kleisli[M, CloudWatchAsyncClient, DescribeAlarmsPublisher] = primitive(_.describeAlarmsPaginator(a)) // B
    override def describeAnomalyDetectors(a: DescribeAnomalyDetectorsRequest): Kleisli[M, CloudWatchAsyncClient, DescribeAnomalyDetectorsResponse] = eff(_.describeAnomalyDetectors(a)) // B
    override def describeAnomalyDetectorsPaginator(a: DescribeAnomalyDetectorsRequest): Kleisli[M, CloudWatchAsyncClient, DescribeAnomalyDetectorsPublisher] = primitive(_.describeAnomalyDetectorsPaginator(a)) // B
    override def describeInsightRules(a: DescribeInsightRulesRequest): Kleisli[M, CloudWatchAsyncClient, DescribeInsightRulesResponse] = eff(_.describeInsightRules(a)) // B
    override def describeInsightRulesPaginator(a: DescribeInsightRulesRequest): Kleisli[M, CloudWatchAsyncClient, DescribeInsightRulesPublisher] = primitive(_.describeInsightRulesPaginator(a)) // B
    override def disableAlarmActions(a: DisableAlarmActionsRequest): Kleisli[M, CloudWatchAsyncClient, DisableAlarmActionsResponse] = eff(_.disableAlarmActions(a)) // B
    override def disableInsightRules(a: DisableInsightRulesRequest): Kleisli[M, CloudWatchAsyncClient, DisableInsightRulesResponse] = eff(_.disableInsightRules(a)) // B
    override def enableAlarmActions(a: EnableAlarmActionsRequest): Kleisli[M, CloudWatchAsyncClient, EnableAlarmActionsResponse] = eff(_.enableAlarmActions(a)) // B
    override def enableInsightRules(a: EnableInsightRulesRequest): Kleisli[M, CloudWatchAsyncClient, EnableInsightRulesResponse] = eff(_.enableInsightRules(a)) // B
    override def getDashboard(a: GetDashboardRequest): Kleisli[M, CloudWatchAsyncClient, GetDashboardResponse] = eff(_.getDashboard(a)) // B
    override def getInsightRuleReport(a: GetInsightRuleReportRequest): Kleisli[M, CloudWatchAsyncClient, GetInsightRuleReportResponse] = eff(_.getInsightRuleReport(a)) // B
    override def getMetricData(a: GetMetricDataRequest): Kleisli[M, CloudWatchAsyncClient, GetMetricDataResponse] = eff(_.getMetricData(a)) // B
    override def getMetricDataPaginator(a: GetMetricDataRequest): Kleisli[M, CloudWatchAsyncClient, GetMetricDataPublisher] = primitive(_.getMetricDataPaginator(a)) // B
    override def getMetricStatistics(a: GetMetricStatisticsRequest): Kleisli[M, CloudWatchAsyncClient, GetMetricStatisticsResponse] = eff(_.getMetricStatistics(a)) // B
    override def getMetricStream(a: GetMetricStreamRequest): Kleisli[M, CloudWatchAsyncClient, GetMetricStreamResponse] = eff(_.getMetricStream(a)) // B
    override def getMetricWidgetImage(a: GetMetricWidgetImageRequest): Kleisli[M, CloudWatchAsyncClient, GetMetricWidgetImageResponse] = eff(_.getMetricWidgetImage(a)) // B
    override def listDashboards : Kleisli[M, CloudWatchAsyncClient, ListDashboardsResponse] = eff(_.listDashboards) // A
    override def listDashboards(a: ListDashboardsRequest): Kleisli[M, CloudWatchAsyncClient, ListDashboardsResponse] = eff(_.listDashboards(a)) // B
    override def listDashboardsPaginator : Kleisli[M, CloudWatchAsyncClient, ListDashboardsPublisher] = primitive(_.listDashboardsPaginator) // A
    override def listDashboardsPaginator(a: ListDashboardsRequest): Kleisli[M, CloudWatchAsyncClient, ListDashboardsPublisher] = primitive(_.listDashboardsPaginator(a)) // B
    override def listManagedInsightRules(a: ListManagedInsightRulesRequest): Kleisli[M, CloudWatchAsyncClient, ListManagedInsightRulesResponse] = eff(_.listManagedInsightRules(a)) // B
    override def listManagedInsightRulesPaginator(a: ListManagedInsightRulesRequest): Kleisli[M, CloudWatchAsyncClient, ListManagedInsightRulesPublisher] = primitive(_.listManagedInsightRulesPaginator(a)) // B
    override def listMetricStreams(a: ListMetricStreamsRequest): Kleisli[M, CloudWatchAsyncClient, ListMetricStreamsResponse] = eff(_.listMetricStreams(a)) // B
    override def listMetricStreamsPaginator(a: ListMetricStreamsRequest): Kleisli[M, CloudWatchAsyncClient, ListMetricStreamsPublisher] = primitive(_.listMetricStreamsPaginator(a)) // B
    override def listMetrics : Kleisli[M, CloudWatchAsyncClient, ListMetricsResponse] = eff(_.listMetrics) // A
    override def listMetrics(a: ListMetricsRequest): Kleisli[M, CloudWatchAsyncClient, ListMetricsResponse] = eff(_.listMetrics(a)) // B
    override def listMetricsPaginator : Kleisli[M, CloudWatchAsyncClient, ListMetricsPublisher] = primitive(_.listMetricsPaginator) // A
    override def listMetricsPaginator(a: ListMetricsRequest): Kleisli[M, CloudWatchAsyncClient, ListMetricsPublisher] = primitive(_.listMetricsPaginator(a)) // B
    override def listTagsForResource(a: ListTagsForResourceRequest): Kleisli[M, CloudWatchAsyncClient, ListTagsForResourceResponse] = eff(_.listTagsForResource(a)) // B
    override def putAnomalyDetector(a: PutAnomalyDetectorRequest): Kleisli[M, CloudWatchAsyncClient, PutAnomalyDetectorResponse] = eff(_.putAnomalyDetector(a)) // B
    override def putCompositeAlarm(a: PutCompositeAlarmRequest): Kleisli[M, CloudWatchAsyncClient, PutCompositeAlarmResponse] = eff(_.putCompositeAlarm(a)) // B
    override def putDashboard(a: PutDashboardRequest): Kleisli[M, CloudWatchAsyncClient, PutDashboardResponse] = eff(_.putDashboard(a)) // B
    override def putInsightRule(a: PutInsightRuleRequest): Kleisli[M, CloudWatchAsyncClient, PutInsightRuleResponse] = eff(_.putInsightRule(a)) // B
    override def putManagedInsightRules(a: PutManagedInsightRulesRequest): Kleisli[M, CloudWatchAsyncClient, PutManagedInsightRulesResponse] = eff(_.putManagedInsightRules(a)) // B
    override def putMetricAlarm(a: PutMetricAlarmRequest): Kleisli[M, CloudWatchAsyncClient, PutMetricAlarmResponse] = eff(_.putMetricAlarm(a)) // B
    override def putMetricData(a: PutMetricDataRequest): Kleisli[M, CloudWatchAsyncClient, PutMetricDataResponse] = eff(_.putMetricData(a)) // B
    override def putMetricStream(a: PutMetricStreamRequest): Kleisli[M, CloudWatchAsyncClient, PutMetricStreamResponse] = eff(_.putMetricStream(a)) // B
    override def serviceName : Kleisli[M, CloudWatchAsyncClient, String] = primitive(_.serviceName) // A
    override def setAlarmState(a: SetAlarmStateRequest): Kleisli[M, CloudWatchAsyncClient, SetAlarmStateResponse] = eff(_.setAlarmState(a)) // B
    override def startMetricStreams(a: StartMetricStreamsRequest): Kleisli[M, CloudWatchAsyncClient, StartMetricStreamsResponse] = eff(_.startMetricStreams(a)) // B
    override def stopMetricStreams(a: StopMetricStreamsRequest): Kleisli[M, CloudWatchAsyncClient, StopMetricStreamsResponse] = eff(_.stopMetricStreams(a)) // B
    override def tagResource(a: TagResourceRequest): Kleisli[M, CloudWatchAsyncClient, TagResourceResponse] = eff(_.tagResource(a)) // B
    override def untagResource(a: UntagResourceRequest): Kleisli[M, CloudWatchAsyncClient, UntagResourceResponse] = eff(_.untagResource(a)) // B
    override def waiter : Kleisli[M, CloudWatchAsyncClient, CloudWatchAsyncWaiter] = primitive(_.waiter) // A
  
  
    def lens[E](f: E => CloudWatchAsyncClient): CloudWatchAsyncClientOp[Kleisli[M, E, *]] =
      new CloudWatchAsyncClientOp[Kleisli[M, E, *]] {
      override def close = Kleisli(e => primitive1(f(e).close))
      override def deleteAlarms(a: DeleteAlarmsRequest) = Kleisli(e => eff1(f(e).deleteAlarms(a)))
      override def deleteAnomalyDetector(a: DeleteAnomalyDetectorRequest) = Kleisli(e => eff1(f(e).deleteAnomalyDetector(a)))
      override def deleteDashboards(a: DeleteDashboardsRequest) = Kleisli(e => eff1(f(e).deleteDashboards(a)))
      override def deleteInsightRules(a: DeleteInsightRulesRequest) = Kleisli(e => eff1(f(e).deleteInsightRules(a)))
      override def deleteMetricStream(a: DeleteMetricStreamRequest) = Kleisli(e => eff1(f(e).deleteMetricStream(a)))
      override def describeAlarmContributors(a: DescribeAlarmContributorsRequest) = Kleisli(e => eff1(f(e).describeAlarmContributors(a)))
      override def describeAlarmHistory = Kleisli(e => eff1(f(e).describeAlarmHistory))
      override def describeAlarmHistory(a: DescribeAlarmHistoryRequest) = Kleisli(e => eff1(f(e).describeAlarmHistory(a)))
      override def describeAlarmHistoryPaginator = Kleisli(e => primitive1(f(e).describeAlarmHistoryPaginator))
      override def describeAlarmHistoryPaginator(a: DescribeAlarmHistoryRequest) = Kleisli(e => primitive1(f(e).describeAlarmHistoryPaginator(a)))
      override def describeAlarms = Kleisli(e => eff1(f(e).describeAlarms))
      override def describeAlarms(a: DescribeAlarmsRequest) = Kleisli(e => eff1(f(e).describeAlarms(a)))
      override def describeAlarmsForMetric(a: DescribeAlarmsForMetricRequest) = Kleisli(e => eff1(f(e).describeAlarmsForMetric(a)))
      override def describeAlarmsPaginator = Kleisli(e => primitive1(f(e).describeAlarmsPaginator))
      override def describeAlarmsPaginator(a: DescribeAlarmsRequest) = Kleisli(e => primitive1(f(e).describeAlarmsPaginator(a)))
      override def describeAnomalyDetectors(a: DescribeAnomalyDetectorsRequest) = Kleisli(e => eff1(f(e).describeAnomalyDetectors(a)))
      override def describeAnomalyDetectorsPaginator(a: DescribeAnomalyDetectorsRequest) = Kleisli(e => primitive1(f(e).describeAnomalyDetectorsPaginator(a)))
      override def describeInsightRules(a: DescribeInsightRulesRequest) = Kleisli(e => eff1(f(e).describeInsightRules(a)))
      override def describeInsightRulesPaginator(a: DescribeInsightRulesRequest) = Kleisli(e => primitive1(f(e).describeInsightRulesPaginator(a)))
      override def disableAlarmActions(a: DisableAlarmActionsRequest) = Kleisli(e => eff1(f(e).disableAlarmActions(a)))
      override def disableInsightRules(a: DisableInsightRulesRequest) = Kleisli(e => eff1(f(e).disableInsightRules(a)))
      override def enableAlarmActions(a: EnableAlarmActionsRequest) = Kleisli(e => eff1(f(e).enableAlarmActions(a)))
      override def enableInsightRules(a: EnableInsightRulesRequest) = Kleisli(e => eff1(f(e).enableInsightRules(a)))
      override def getDashboard(a: GetDashboardRequest) = Kleisli(e => eff1(f(e).getDashboard(a)))
      override def getInsightRuleReport(a: GetInsightRuleReportRequest) = Kleisli(e => eff1(f(e).getInsightRuleReport(a)))
      override def getMetricData(a: GetMetricDataRequest) = Kleisli(e => eff1(f(e).getMetricData(a)))
      override def getMetricDataPaginator(a: GetMetricDataRequest) = Kleisli(e => primitive1(f(e).getMetricDataPaginator(a)))
      override def getMetricStatistics(a: GetMetricStatisticsRequest) = Kleisli(e => eff1(f(e).getMetricStatistics(a)))
      override def getMetricStream(a: GetMetricStreamRequest) = Kleisli(e => eff1(f(e).getMetricStream(a)))
      override def getMetricWidgetImage(a: GetMetricWidgetImageRequest) = Kleisli(e => eff1(f(e).getMetricWidgetImage(a)))
      override def listDashboards = Kleisli(e => eff1(f(e).listDashboards))
      override def listDashboards(a: ListDashboardsRequest) = Kleisli(e => eff1(f(e).listDashboards(a)))
      override def listDashboardsPaginator = Kleisli(e => primitive1(f(e).listDashboardsPaginator))
      override def listDashboardsPaginator(a: ListDashboardsRequest) = Kleisli(e => primitive1(f(e).listDashboardsPaginator(a)))
      override def listManagedInsightRules(a: ListManagedInsightRulesRequest) = Kleisli(e => eff1(f(e).listManagedInsightRules(a)))
      override def listManagedInsightRulesPaginator(a: ListManagedInsightRulesRequest) = Kleisli(e => primitive1(f(e).listManagedInsightRulesPaginator(a)))
      override def listMetricStreams(a: ListMetricStreamsRequest) = Kleisli(e => eff1(f(e).listMetricStreams(a)))
      override def listMetricStreamsPaginator(a: ListMetricStreamsRequest) = Kleisli(e => primitive1(f(e).listMetricStreamsPaginator(a)))
      override def listMetrics = Kleisli(e => eff1(f(e).listMetrics))
      override def listMetrics(a: ListMetricsRequest) = Kleisli(e => eff1(f(e).listMetrics(a)))
      override def listMetricsPaginator = Kleisli(e => primitive1(f(e).listMetricsPaginator))
      override def listMetricsPaginator(a: ListMetricsRequest) = Kleisli(e => primitive1(f(e).listMetricsPaginator(a)))
      override def listTagsForResource(a: ListTagsForResourceRequest) = Kleisli(e => eff1(f(e).listTagsForResource(a)))
      override def putAnomalyDetector(a: PutAnomalyDetectorRequest) = Kleisli(e => eff1(f(e).putAnomalyDetector(a)))
      override def putCompositeAlarm(a: PutCompositeAlarmRequest) = Kleisli(e => eff1(f(e).putCompositeAlarm(a)))
      override def putDashboard(a: PutDashboardRequest) = Kleisli(e => eff1(f(e).putDashboard(a)))
      override def putInsightRule(a: PutInsightRuleRequest) = Kleisli(e => eff1(f(e).putInsightRule(a)))
      override def putManagedInsightRules(a: PutManagedInsightRulesRequest) = Kleisli(e => eff1(f(e).putManagedInsightRules(a)))
      override def putMetricAlarm(a: PutMetricAlarmRequest) = Kleisli(e => eff1(f(e).putMetricAlarm(a)))
      override def putMetricData(a: PutMetricDataRequest) = Kleisli(e => eff1(f(e).putMetricData(a)))
      override def putMetricStream(a: PutMetricStreamRequest) = Kleisli(e => eff1(f(e).putMetricStream(a)))
      override def serviceName = Kleisli(e => primitive1(f(e).serviceName))
      override def setAlarmState(a: SetAlarmStateRequest) = Kleisli(e => eff1(f(e).setAlarmState(a)))
      override def startMetricStreams(a: StartMetricStreamsRequest) = Kleisli(e => eff1(f(e).startMetricStreams(a)))
      override def stopMetricStreams(a: StopMetricStreamsRequest) = Kleisli(e => eff1(f(e).stopMetricStreams(a)))
      override def tagResource(a: TagResourceRequest) = Kleisli(e => eff1(f(e).tagResource(a)))
      override def untagResource(a: UntagResourceRequest) = Kleisli(e => eff1(f(e).untagResource(a)))
      override def waiter = Kleisli(e => primitive1(f(e).waiter))
  
      }
    }

  // end interpreters

  def CloudWatchAsyncClientResource(builder : CloudWatchAsyncClientBuilder) : Resource[M, CloudWatchAsyncClient] = Resource.fromAutoCloseable(asyncM.delay(builder.build()))
  def CloudWatchAsyncClientOpResource(builder: CloudWatchAsyncClientBuilder) = CloudWatchAsyncClientResource(builder).map(create)

  def create(client : CloudWatchAsyncClient) : CloudWatchAsyncClientOp[M] = new CloudWatchAsyncClientOp[M] {

    // domain-specific operations are implemented in terms of `primitive`
    override def close = primitive1(client.close)
    override def deleteAlarms(a: DeleteAlarmsRequest) = eff1(client.deleteAlarms(a))
    override def deleteAnomalyDetector(a: DeleteAnomalyDetectorRequest) = eff1(client.deleteAnomalyDetector(a))
    override def deleteDashboards(a: DeleteDashboardsRequest) = eff1(client.deleteDashboards(a))
    override def deleteInsightRules(a: DeleteInsightRulesRequest) = eff1(client.deleteInsightRules(a))
    override def deleteMetricStream(a: DeleteMetricStreamRequest) = eff1(client.deleteMetricStream(a))
    override def describeAlarmContributors(a: DescribeAlarmContributorsRequest) = eff1(client.describeAlarmContributors(a))
    override def describeAlarmHistory = eff1(client.describeAlarmHistory)
    override def describeAlarmHistory(a: DescribeAlarmHistoryRequest) = eff1(client.describeAlarmHistory(a))
    override def describeAlarmHistoryPaginator = primitive1(client.describeAlarmHistoryPaginator)
    override def describeAlarmHistoryPaginator(a: DescribeAlarmHistoryRequest) = primitive1(client.describeAlarmHistoryPaginator(a))
    override def describeAlarms = eff1(client.describeAlarms)
    override def describeAlarms(a: DescribeAlarmsRequest) = eff1(client.describeAlarms(a))
    override def describeAlarmsForMetric(a: DescribeAlarmsForMetricRequest) = eff1(client.describeAlarmsForMetric(a))
    override def describeAlarmsPaginator = primitive1(client.describeAlarmsPaginator)
    override def describeAlarmsPaginator(a: DescribeAlarmsRequest) = primitive1(client.describeAlarmsPaginator(a))
    override def describeAnomalyDetectors(a: DescribeAnomalyDetectorsRequest) = eff1(client.describeAnomalyDetectors(a))
    override def describeAnomalyDetectorsPaginator(a: DescribeAnomalyDetectorsRequest) = primitive1(client.describeAnomalyDetectorsPaginator(a))
    override def describeInsightRules(a: DescribeInsightRulesRequest) = eff1(client.describeInsightRules(a))
    override def describeInsightRulesPaginator(a: DescribeInsightRulesRequest) = primitive1(client.describeInsightRulesPaginator(a))
    override def disableAlarmActions(a: DisableAlarmActionsRequest) = eff1(client.disableAlarmActions(a))
    override def disableInsightRules(a: DisableInsightRulesRequest) = eff1(client.disableInsightRules(a))
    override def enableAlarmActions(a: EnableAlarmActionsRequest) = eff1(client.enableAlarmActions(a))
    override def enableInsightRules(a: EnableInsightRulesRequest) = eff1(client.enableInsightRules(a))
    override def getDashboard(a: GetDashboardRequest) = eff1(client.getDashboard(a))
    override def getInsightRuleReport(a: GetInsightRuleReportRequest) = eff1(client.getInsightRuleReport(a))
    override def getMetricData(a: GetMetricDataRequest) = eff1(client.getMetricData(a))
    override def getMetricDataPaginator(a: GetMetricDataRequest) = primitive1(client.getMetricDataPaginator(a))
    override def getMetricStatistics(a: GetMetricStatisticsRequest) = eff1(client.getMetricStatistics(a))
    override def getMetricStream(a: GetMetricStreamRequest) = eff1(client.getMetricStream(a))
    override def getMetricWidgetImage(a: GetMetricWidgetImageRequest) = eff1(client.getMetricWidgetImage(a))
    override def listDashboards = eff1(client.listDashboards)
    override def listDashboards(a: ListDashboardsRequest) = eff1(client.listDashboards(a))
    override def listDashboardsPaginator = primitive1(client.listDashboardsPaginator)
    override def listDashboardsPaginator(a: ListDashboardsRequest) = primitive1(client.listDashboardsPaginator(a))
    override def listManagedInsightRules(a: ListManagedInsightRulesRequest) = eff1(client.listManagedInsightRules(a))
    override def listManagedInsightRulesPaginator(a: ListManagedInsightRulesRequest) = primitive1(client.listManagedInsightRulesPaginator(a))
    override def listMetricStreams(a: ListMetricStreamsRequest) = eff1(client.listMetricStreams(a))
    override def listMetricStreamsPaginator(a: ListMetricStreamsRequest) = primitive1(client.listMetricStreamsPaginator(a))
    override def listMetrics = eff1(client.listMetrics)
    override def listMetrics(a: ListMetricsRequest) = eff1(client.listMetrics(a))
    override def listMetricsPaginator = primitive1(client.listMetricsPaginator)
    override def listMetricsPaginator(a: ListMetricsRequest) = primitive1(client.listMetricsPaginator(a))
    override def listTagsForResource(a: ListTagsForResourceRequest) = eff1(client.listTagsForResource(a))
    override def putAnomalyDetector(a: PutAnomalyDetectorRequest) = eff1(client.putAnomalyDetector(a))
    override def putCompositeAlarm(a: PutCompositeAlarmRequest) = eff1(client.putCompositeAlarm(a))
    override def putDashboard(a: PutDashboardRequest) = eff1(client.putDashboard(a))
    override def putInsightRule(a: PutInsightRuleRequest) = eff1(client.putInsightRule(a))
    override def putManagedInsightRules(a: PutManagedInsightRulesRequest) = eff1(client.putManagedInsightRules(a))
    override def putMetricAlarm(a: PutMetricAlarmRequest) = eff1(client.putMetricAlarm(a))
    override def putMetricData(a: PutMetricDataRequest) = eff1(client.putMetricData(a))
    override def putMetricStream(a: PutMetricStreamRequest) = eff1(client.putMetricStream(a))
    override def serviceName = primitive1(client.serviceName)
    override def setAlarmState(a: SetAlarmStateRequest) = eff1(client.setAlarmState(a))
    override def startMetricStreams(a: StartMetricStreamsRequest) = eff1(client.startMetricStreams(a))
    override def stopMetricStreams(a: StopMetricStreamsRequest) = eff1(client.stopMetricStreams(a))
    override def tagResource(a: TagResourceRequest) = eff1(client.tagResource(a))
    override def untagResource(a: UntagResourceRequest) = eff1(client.untagResource(a))
    override def waiter = primitive1(client.waiter)


  }


}

