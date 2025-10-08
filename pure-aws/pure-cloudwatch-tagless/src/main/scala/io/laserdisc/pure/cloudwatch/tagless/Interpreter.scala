package io.laserdisc.pure.cloudwatch.tagless

// Library imports
import cats.data.Kleisli
import cats.effect.{Async, Resource}

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

  lazy val CloudWatchAsyncClientInterpreter: CloudWatchAsyncClientInterpreter = new CloudWatchAsyncClientInterpreter {}
  // Some methods are common to all interpreters and can be overridden to change behavior globally.

  private def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(j => asyncM.blocking(f(j)))
  private def primitive1[J, A](f: => A): M[A]              = asyncM.blocking(f)

  private def eff[J, A](fut: J => CompletableFuture[A]): Kleisli[M, J, A] = Kleisli { j =>
    asyncM.fromCompletableFuture(asyncM.delay(fut(j)))
  }
  private def eff1[J, A](fut: => CompletableFuture[A]): M[A] =
    asyncM.fromCompletableFuture(asyncM.delay(fut))

  // Interpreters // scalafmt: off
  trait CloudWatchAsyncClientInterpreter extends CloudWatchAsyncClientOp[Kleisli[M, CloudWatchAsyncClient, *]] {

    // domain-specific operations are implemented in terms of `primitive`
    override def close: Kleisli[M, CloudWatchAsyncClient, Unit]                                                                                              = primitive(_.close)
    override def deleteAlarms(a: DeleteAlarmsRequest): Kleisli[M, CloudWatchAsyncClient, DeleteAlarmsResponse]                                               = eff(_.deleteAlarms(a))
    override def deleteAnomalyDetector(a: DeleteAnomalyDetectorRequest): Kleisli[M, CloudWatchAsyncClient, DeleteAnomalyDetectorResponse]                    = eff(_.deleteAnomalyDetector(a))
    override def deleteDashboards(a: DeleteDashboardsRequest): Kleisli[M, CloudWatchAsyncClient, DeleteDashboardsResponse]                                   = eff(_.deleteDashboards(a))
    override def deleteInsightRules(a: DeleteInsightRulesRequest): Kleisli[M, CloudWatchAsyncClient, DeleteInsightRulesResponse]                             = eff(_.deleteInsightRules(a))
    override def deleteMetricStream(a: DeleteMetricStreamRequest): Kleisli[M, CloudWatchAsyncClient, DeleteMetricStreamResponse]                             = eff(_.deleteMetricStream(a))
    override def describeAlarmContributors(a: DescribeAlarmContributorsRequest): Kleisli[M, CloudWatchAsyncClient, DescribeAlarmContributorsResponse]        = eff(_.describeAlarmContributors(a))
    override def describeAlarmHistory: Kleisli[M, CloudWatchAsyncClient, DescribeAlarmHistoryResponse]                                                       = eff(_.describeAlarmHistory)
    override def describeAlarmHistory(a: DescribeAlarmHistoryRequest): Kleisli[M, CloudWatchAsyncClient, DescribeAlarmHistoryResponse]                       = eff(_.describeAlarmHistory(a))
    override def describeAlarmHistoryPaginator: Kleisli[M, CloudWatchAsyncClient, DescribeAlarmHistoryPublisher]                                             = primitive(_.describeAlarmHistoryPaginator)
    override def describeAlarmHistoryPaginator(a: DescribeAlarmHistoryRequest): Kleisli[M, CloudWatchAsyncClient, DescribeAlarmHistoryPublisher]             = primitive(_.describeAlarmHistoryPaginator(a))
    override def describeAlarms: Kleisli[M, CloudWatchAsyncClient, DescribeAlarmsResponse]                                                                   = eff(_.describeAlarms)
    override def describeAlarms(a: DescribeAlarmsRequest): Kleisli[M, CloudWatchAsyncClient, DescribeAlarmsResponse]                                         = eff(_.describeAlarms(a))
    override def describeAlarmsForMetric(a: DescribeAlarmsForMetricRequest): Kleisli[M, CloudWatchAsyncClient, DescribeAlarmsForMetricResponse]              = eff(_.describeAlarmsForMetric(a))
    override def describeAlarmsPaginator: Kleisli[M, CloudWatchAsyncClient, DescribeAlarmsPublisher]                                                         = primitive(_.describeAlarmsPaginator)
    override def describeAlarmsPaginator(a: DescribeAlarmsRequest): Kleisli[M, CloudWatchAsyncClient, DescribeAlarmsPublisher]                               = primitive(_.describeAlarmsPaginator(a))
    override def describeAnomalyDetectors(a: DescribeAnomalyDetectorsRequest): Kleisli[M, CloudWatchAsyncClient, DescribeAnomalyDetectorsResponse]           = eff(_.describeAnomalyDetectors(a))
    override def describeAnomalyDetectorsPaginator(a: DescribeAnomalyDetectorsRequest): Kleisli[M, CloudWatchAsyncClient, DescribeAnomalyDetectorsPublisher] = primitive(_.describeAnomalyDetectorsPaginator(a))
    override def describeInsightRules(a: DescribeInsightRulesRequest): Kleisli[M, CloudWatchAsyncClient, DescribeInsightRulesResponse]                       = eff(_.describeInsightRules(a))
    override def describeInsightRulesPaginator(a: DescribeInsightRulesRequest): Kleisli[M, CloudWatchAsyncClient, DescribeInsightRulesPublisher]             = primitive(_.describeInsightRulesPaginator(a))
    override def disableAlarmActions(a: DisableAlarmActionsRequest): Kleisli[M, CloudWatchAsyncClient, DisableAlarmActionsResponse]                          = eff(_.disableAlarmActions(a))
    override def disableInsightRules(a: DisableInsightRulesRequest): Kleisli[M, CloudWatchAsyncClient, DisableInsightRulesResponse]                          = eff(_.disableInsightRules(a))
    override def enableAlarmActions(a: EnableAlarmActionsRequest): Kleisli[M, CloudWatchAsyncClient, EnableAlarmActionsResponse]                             = eff(_.enableAlarmActions(a))
    override def enableInsightRules(a: EnableInsightRulesRequest): Kleisli[M, CloudWatchAsyncClient, EnableInsightRulesResponse]                             = eff(_.enableInsightRules(a))
    override def getDashboard(a: GetDashboardRequest): Kleisli[M, CloudWatchAsyncClient, GetDashboardResponse]                                               = eff(_.getDashboard(a))
    override def getInsightRuleReport(a: GetInsightRuleReportRequest): Kleisli[M, CloudWatchAsyncClient, GetInsightRuleReportResponse]                       = eff(_.getInsightRuleReport(a))
    override def getMetricData(a: GetMetricDataRequest): Kleisli[M, CloudWatchAsyncClient, GetMetricDataResponse]                                            = eff(_.getMetricData(a))
    override def getMetricDataPaginator(a: GetMetricDataRequest): Kleisli[M, CloudWatchAsyncClient, GetMetricDataPublisher]                                  = primitive(_.getMetricDataPaginator(a))
    override def getMetricStatistics(a: GetMetricStatisticsRequest): Kleisli[M, CloudWatchAsyncClient, GetMetricStatisticsResponse]                          = eff(_.getMetricStatistics(a))
    override def getMetricStream(a: GetMetricStreamRequest): Kleisli[M, CloudWatchAsyncClient, GetMetricStreamResponse]                                      = eff(_.getMetricStream(a))
    override def getMetricWidgetImage(a: GetMetricWidgetImageRequest): Kleisli[M, CloudWatchAsyncClient, GetMetricWidgetImageResponse]                       = eff(_.getMetricWidgetImage(a))
    override def listDashboards: Kleisli[M, CloudWatchAsyncClient, ListDashboardsResponse]                                                                   = eff(_.listDashboards)
    override def listDashboards(a: ListDashboardsRequest): Kleisli[M, CloudWatchAsyncClient, ListDashboardsResponse]                                         = eff(_.listDashboards(a))
    override def listDashboardsPaginator: Kleisli[M, CloudWatchAsyncClient, ListDashboardsPublisher]                                                         = primitive(_.listDashboardsPaginator)
    override def listDashboardsPaginator(a: ListDashboardsRequest): Kleisli[M, CloudWatchAsyncClient, ListDashboardsPublisher]                               = primitive(_.listDashboardsPaginator(a))
    override def listManagedInsightRules(a: ListManagedInsightRulesRequest): Kleisli[M, CloudWatchAsyncClient, ListManagedInsightRulesResponse]              = eff(_.listManagedInsightRules(a))
    override def listManagedInsightRulesPaginator(a: ListManagedInsightRulesRequest): Kleisli[M, CloudWatchAsyncClient, ListManagedInsightRulesPublisher]    = primitive(_.listManagedInsightRulesPaginator(a))
    override def listMetricStreams(a: ListMetricStreamsRequest): Kleisli[M, CloudWatchAsyncClient, ListMetricStreamsResponse]                                = eff(_.listMetricStreams(a))
    override def listMetricStreamsPaginator(a: ListMetricStreamsRequest): Kleisli[M, CloudWatchAsyncClient, ListMetricStreamsPublisher]                      = primitive(_.listMetricStreamsPaginator(a))
    override def listMetrics: Kleisli[M, CloudWatchAsyncClient, ListMetricsResponse]                                                                         = eff(_.listMetrics)
    override def listMetrics(a: ListMetricsRequest): Kleisli[M, CloudWatchAsyncClient, ListMetricsResponse]                                                  = eff(_.listMetrics(a))
    override def listMetricsPaginator: Kleisli[M, CloudWatchAsyncClient, ListMetricsPublisher]                                                               = primitive(_.listMetricsPaginator)
    override def listMetricsPaginator(a: ListMetricsRequest): Kleisli[M, CloudWatchAsyncClient, ListMetricsPublisher]                                        = primitive(_.listMetricsPaginator(a))
    override def listTagsForResource(a: ListTagsForResourceRequest): Kleisli[M, CloudWatchAsyncClient, ListTagsForResourceResponse]                          = eff(_.listTagsForResource(a))
    override def putAnomalyDetector(a: PutAnomalyDetectorRequest): Kleisli[M, CloudWatchAsyncClient, PutAnomalyDetectorResponse]                             = eff(_.putAnomalyDetector(a))
    override def putCompositeAlarm(a: PutCompositeAlarmRequest): Kleisli[M, CloudWatchAsyncClient, PutCompositeAlarmResponse]                                = eff(_.putCompositeAlarm(a))
    override def putDashboard(a: PutDashboardRequest): Kleisli[M, CloudWatchAsyncClient, PutDashboardResponse]                                               = eff(_.putDashboard(a))
    override def putInsightRule(a: PutInsightRuleRequest): Kleisli[M, CloudWatchAsyncClient, PutInsightRuleResponse]                                         = eff(_.putInsightRule(a))
    override def putManagedInsightRules(a: PutManagedInsightRulesRequest): Kleisli[M, CloudWatchAsyncClient, PutManagedInsightRulesResponse]                 = eff(_.putManagedInsightRules(a))
    override def putMetricAlarm(a: PutMetricAlarmRequest): Kleisli[M, CloudWatchAsyncClient, PutMetricAlarmResponse]                                         = eff(_.putMetricAlarm(a))
    override def putMetricData(a: PutMetricDataRequest): Kleisli[M, CloudWatchAsyncClient, PutMetricDataResponse]                                            = eff(_.putMetricData(a))
    override def putMetricStream(a: PutMetricStreamRequest): Kleisli[M, CloudWatchAsyncClient, PutMetricStreamResponse]                                      = eff(_.putMetricStream(a))
    override def serviceName: Kleisli[M, CloudWatchAsyncClient, String]                                                                                      = primitive(_.serviceName)
    override def setAlarmState(a: SetAlarmStateRequest): Kleisli[M, CloudWatchAsyncClient, SetAlarmStateResponse]                                            = eff(_.setAlarmState(a))
    override def startMetricStreams(a: StartMetricStreamsRequest): Kleisli[M, CloudWatchAsyncClient, StartMetricStreamsResponse]                             = eff(_.startMetricStreams(a))
    override def stopMetricStreams(a: StopMetricStreamsRequest): Kleisli[M, CloudWatchAsyncClient, StopMetricStreamsResponse]                                = eff(_.stopMetricStreams(a))
    override def tagResource(a: TagResourceRequest): Kleisli[M, CloudWatchAsyncClient, TagResourceResponse]                                                  = eff(_.tagResource(a))
    override def untagResource(a: UntagResourceRequest): Kleisli[M, CloudWatchAsyncClient, UntagResourceResponse]                                            = eff(_.untagResource(a))
    override def waiter: Kleisli[M, CloudWatchAsyncClient, CloudWatchAsyncWaiter]                                                                            = primitive(_.waiter)

    def lens[E](f: E => CloudWatchAsyncClient): CloudWatchAsyncClientOp[Kleisli[M, E, *]] =
      new CloudWatchAsyncClientOp[Kleisli[M, E, *]] {
        override def close: Kleisli[M, E, Unit]                                                                                              = Kleisli(e => primitive1(f(e).close))
        override def deleteAlarms(a: DeleteAlarmsRequest): Kleisli[M, E, DeleteAlarmsResponse]                                               = Kleisli(e => eff1(f(e).deleteAlarms(a)))
        override def deleteAnomalyDetector(a: DeleteAnomalyDetectorRequest): Kleisli[M, E, DeleteAnomalyDetectorResponse]                    = Kleisli(e => eff1(f(e).deleteAnomalyDetector(a)))
        override def deleteDashboards(a: DeleteDashboardsRequest): Kleisli[M, E, DeleteDashboardsResponse]                                   = Kleisli(e => eff1(f(e).deleteDashboards(a)))
        override def deleteInsightRules(a: DeleteInsightRulesRequest): Kleisli[M, E, DeleteInsightRulesResponse]                             = Kleisli(e => eff1(f(e).deleteInsightRules(a)))
        override def deleteMetricStream(a: DeleteMetricStreamRequest): Kleisli[M, E, DeleteMetricStreamResponse]                             = Kleisli(e => eff1(f(e).deleteMetricStream(a)))
        override def describeAlarmContributors(a: DescribeAlarmContributorsRequest): Kleisli[M, E, DescribeAlarmContributorsResponse]        = Kleisli(e => eff1(f(e).describeAlarmContributors(a)))
        override def describeAlarmHistory: Kleisli[M, E, DescribeAlarmHistoryResponse]                                                       = Kleisli(e => eff1(f(e).describeAlarmHistory))
        override def describeAlarmHistory(a: DescribeAlarmHistoryRequest): Kleisli[M, E, DescribeAlarmHistoryResponse]                       = Kleisli(e => eff1(f(e).describeAlarmHistory(a)))
        override def describeAlarmHistoryPaginator: Kleisli[M, E, DescribeAlarmHistoryPublisher]                                             = Kleisli(e => primitive1(f(e).describeAlarmHistoryPaginator))
        override def describeAlarmHistoryPaginator(a: DescribeAlarmHistoryRequest): Kleisli[M, E, DescribeAlarmHistoryPublisher]             = Kleisli(e => primitive1(f(e).describeAlarmHistoryPaginator(a)))
        override def describeAlarms: Kleisli[M, E, DescribeAlarmsResponse]                                                                   = Kleisli(e => eff1(f(e).describeAlarms))
        override def describeAlarms(a: DescribeAlarmsRequest): Kleisli[M, E, DescribeAlarmsResponse]                                         = Kleisli(e => eff1(f(e).describeAlarms(a)))
        override def describeAlarmsForMetric(a: DescribeAlarmsForMetricRequest): Kleisli[M, E, DescribeAlarmsForMetricResponse]              = Kleisli(e => eff1(f(e).describeAlarmsForMetric(a)))
        override def describeAlarmsPaginator: Kleisli[M, E, DescribeAlarmsPublisher]                                                         = Kleisli(e => primitive1(f(e).describeAlarmsPaginator))
        override def describeAlarmsPaginator(a: DescribeAlarmsRequest): Kleisli[M, E, DescribeAlarmsPublisher]                               = Kleisli(e => primitive1(f(e).describeAlarmsPaginator(a)))
        override def describeAnomalyDetectors(a: DescribeAnomalyDetectorsRequest): Kleisli[M, E, DescribeAnomalyDetectorsResponse]           = Kleisli(e => eff1(f(e).describeAnomalyDetectors(a)))
        override def describeAnomalyDetectorsPaginator(a: DescribeAnomalyDetectorsRequest): Kleisli[M, E, DescribeAnomalyDetectorsPublisher] = Kleisli(e => primitive1(f(e).describeAnomalyDetectorsPaginator(a)))
        override def describeInsightRules(a: DescribeInsightRulesRequest): Kleisli[M, E, DescribeInsightRulesResponse]                       = Kleisli(e => eff1(f(e).describeInsightRules(a)))
        override def describeInsightRulesPaginator(a: DescribeInsightRulesRequest): Kleisli[M, E, DescribeInsightRulesPublisher]             = Kleisli(e => primitive1(f(e).describeInsightRulesPaginator(a)))
        override def disableAlarmActions(a: DisableAlarmActionsRequest): Kleisli[M, E, DisableAlarmActionsResponse]                          = Kleisli(e => eff1(f(e).disableAlarmActions(a)))
        override def disableInsightRules(a: DisableInsightRulesRequest): Kleisli[M, E, DisableInsightRulesResponse]                          = Kleisli(e => eff1(f(e).disableInsightRules(a)))
        override def enableAlarmActions(a: EnableAlarmActionsRequest): Kleisli[M, E, EnableAlarmActionsResponse]                             = Kleisli(e => eff1(f(e).enableAlarmActions(a)))
        override def enableInsightRules(a: EnableInsightRulesRequest): Kleisli[M, E, EnableInsightRulesResponse]                             = Kleisli(e => eff1(f(e).enableInsightRules(a)))
        override def getDashboard(a: GetDashboardRequest): Kleisli[M, E, GetDashboardResponse]                                               = Kleisli(e => eff1(f(e).getDashboard(a)))
        override def getInsightRuleReport(a: GetInsightRuleReportRequest): Kleisli[M, E, GetInsightRuleReportResponse]                       = Kleisli(e => eff1(f(e).getInsightRuleReport(a)))
        override def getMetricData(a: GetMetricDataRequest): Kleisli[M, E, GetMetricDataResponse]                                            = Kleisli(e => eff1(f(e).getMetricData(a)))
        override def getMetricDataPaginator(a: GetMetricDataRequest): Kleisli[M, E, GetMetricDataPublisher]                                  = Kleisli(e => primitive1(f(e).getMetricDataPaginator(a)))
        override def getMetricStatistics(a: GetMetricStatisticsRequest): Kleisli[M, E, GetMetricStatisticsResponse]                          = Kleisli(e => eff1(f(e).getMetricStatistics(a)))
        override def getMetricStream(a: GetMetricStreamRequest): Kleisli[M, E, GetMetricStreamResponse]                                      = Kleisli(e => eff1(f(e).getMetricStream(a)))
        override def getMetricWidgetImage(a: GetMetricWidgetImageRequest): Kleisli[M, E, GetMetricWidgetImageResponse]                       = Kleisli(e => eff1(f(e).getMetricWidgetImage(a)))
        override def listDashboards: Kleisli[M, E, ListDashboardsResponse]                                                                   = Kleisli(e => eff1(f(e).listDashboards))
        override def listDashboards(a: ListDashboardsRequest): Kleisli[M, E, ListDashboardsResponse]                                         = Kleisli(e => eff1(f(e).listDashboards(a)))
        override def listDashboardsPaginator: Kleisli[M, E, ListDashboardsPublisher]                                                         = Kleisli(e => primitive1(f(e).listDashboardsPaginator))
        override def listDashboardsPaginator(a: ListDashboardsRequest): Kleisli[M, E, ListDashboardsPublisher]                               = Kleisli(e => primitive1(f(e).listDashboardsPaginator(a)))
        override def listManagedInsightRules(a: ListManagedInsightRulesRequest): Kleisli[M, E, ListManagedInsightRulesResponse]              = Kleisli(e => eff1(f(e).listManagedInsightRules(a)))
        override def listManagedInsightRulesPaginator(a: ListManagedInsightRulesRequest): Kleisli[M, E, ListManagedInsightRulesPublisher]    = Kleisli(e => primitive1(f(e).listManagedInsightRulesPaginator(a)))
        override def listMetricStreams(a: ListMetricStreamsRequest): Kleisli[M, E, ListMetricStreamsResponse]                                = Kleisli(e => eff1(f(e).listMetricStreams(a)))
        override def listMetricStreamsPaginator(a: ListMetricStreamsRequest): Kleisli[M, E, ListMetricStreamsPublisher]                      = Kleisli(e => primitive1(f(e).listMetricStreamsPaginator(a)))
        override def listMetrics: Kleisli[M, E, ListMetricsResponse]                                                                         = Kleisli(e => eff1(f(e).listMetrics))
        override def listMetrics(a: ListMetricsRequest): Kleisli[M, E, ListMetricsResponse]                                                  = Kleisli(e => eff1(f(e).listMetrics(a)))
        override def listMetricsPaginator: Kleisli[M, E, ListMetricsPublisher]                                                               = Kleisli(e => primitive1(f(e).listMetricsPaginator))
        override def listMetricsPaginator(a: ListMetricsRequest): Kleisli[M, E, ListMetricsPublisher]                                        = Kleisli(e => primitive1(f(e).listMetricsPaginator(a)))
        override def listTagsForResource(a: ListTagsForResourceRequest): Kleisli[M, E, ListTagsForResourceResponse]                          = Kleisli(e => eff1(f(e).listTagsForResource(a)))
        override def putAnomalyDetector(a: PutAnomalyDetectorRequest): Kleisli[M, E, PutAnomalyDetectorResponse]                             = Kleisli(e => eff1(f(e).putAnomalyDetector(a)))
        override def putCompositeAlarm(a: PutCompositeAlarmRequest): Kleisli[M, E, PutCompositeAlarmResponse]                                = Kleisli(e => eff1(f(e).putCompositeAlarm(a)))
        override def putDashboard(a: PutDashboardRequest): Kleisli[M, E, PutDashboardResponse]                                               = Kleisli(e => eff1(f(e).putDashboard(a)))
        override def putInsightRule(a: PutInsightRuleRequest): Kleisli[M, E, PutInsightRuleResponse]                                         = Kleisli(e => eff1(f(e).putInsightRule(a)))
        override def putManagedInsightRules(a: PutManagedInsightRulesRequest): Kleisli[M, E, PutManagedInsightRulesResponse]                 = Kleisli(e => eff1(f(e).putManagedInsightRules(a)))
        override def putMetricAlarm(a: PutMetricAlarmRequest): Kleisli[M, E, PutMetricAlarmResponse]                                         = Kleisli(e => eff1(f(e).putMetricAlarm(a)))
        override def putMetricData(a: PutMetricDataRequest): Kleisli[M, E, PutMetricDataResponse]                                            = Kleisli(e => eff1(f(e).putMetricData(a)))
        override def putMetricStream(a: PutMetricStreamRequest): Kleisli[M, E, PutMetricStreamResponse]                                      = Kleisli(e => eff1(f(e).putMetricStream(a)))
        override def serviceName: Kleisli[M, E, String]                                                                                      = Kleisli(e => primitive1(f(e).serviceName))
        override def setAlarmState(a: SetAlarmStateRequest): Kleisli[M, E, SetAlarmStateResponse]                                            = Kleisli(e => eff1(f(e).setAlarmState(a)))
        override def startMetricStreams(a: StartMetricStreamsRequest): Kleisli[M, E, StartMetricStreamsResponse]                             = Kleisli(e => eff1(f(e).startMetricStreams(a)))
        override def stopMetricStreams(a: StopMetricStreamsRequest): Kleisli[M, E, StopMetricStreamsResponse]                                = Kleisli(e => eff1(f(e).stopMetricStreams(a)))
        override def tagResource(a: TagResourceRequest): Kleisli[M, E, TagResourceResponse]                                                  = Kleisli(e => eff1(f(e).tagResource(a)))
        override def untagResource(a: UntagResourceRequest): Kleisli[M, E, UntagResourceResponse]                                            = Kleisli(e => eff1(f(e).untagResource(a)))
        override def waiter: Kleisli[M, E, CloudWatchAsyncWaiter]                                                                            = Kleisli(e => primitive1(f(e).waiter))
      }
  }
  // end interpreters

  def CloudWatchAsyncClientResource(builder: CloudWatchAsyncClientBuilder): Resource[M, CloudWatchAsyncClient]        = Resource.fromAutoCloseable(asyncM.delay(builder.build()))
  def CloudWatchAsyncClientOpResource(builder: CloudWatchAsyncClientBuilder): Resource[M, CloudWatchAsyncClientOp[M]] = CloudWatchAsyncClientResource(builder).map(create)

  def create(client: CloudWatchAsyncClient): CloudWatchAsyncClientOp[M] = new CloudWatchAsyncClientOp[M] {

    // domain-specific operations are implemented in terms of `primitive`
    override def close: M[Unit]                                                                                              = primitive1(client.close)
    override def deleteAlarms(a: DeleteAlarmsRequest): M[DeleteAlarmsResponse]                                               = eff1(client.deleteAlarms(a))
    override def deleteAnomalyDetector(a: DeleteAnomalyDetectorRequest): M[DeleteAnomalyDetectorResponse]                    = eff1(client.deleteAnomalyDetector(a))
    override def deleteDashboards(a: DeleteDashboardsRequest): M[DeleteDashboardsResponse]                                   = eff1(client.deleteDashboards(a))
    override def deleteInsightRules(a: DeleteInsightRulesRequest): M[DeleteInsightRulesResponse]                             = eff1(client.deleteInsightRules(a))
    override def deleteMetricStream(a: DeleteMetricStreamRequest): M[DeleteMetricStreamResponse]                             = eff1(client.deleteMetricStream(a))
    override def describeAlarmContributors(a: DescribeAlarmContributorsRequest): M[DescribeAlarmContributorsResponse]        = eff1(client.describeAlarmContributors(a))
    override def describeAlarmHistory: M[DescribeAlarmHistoryResponse]                                                       = eff1(client.describeAlarmHistory)
    override def describeAlarmHistory(a: DescribeAlarmHistoryRequest): M[DescribeAlarmHistoryResponse]                       = eff1(client.describeAlarmHistory(a))
    override def describeAlarmHistoryPaginator: M[DescribeAlarmHistoryPublisher]                                             = primitive1(client.describeAlarmHistoryPaginator)
    override def describeAlarmHistoryPaginator(a: DescribeAlarmHistoryRequest): M[DescribeAlarmHistoryPublisher]             = primitive1(client.describeAlarmHistoryPaginator(a))
    override def describeAlarms: M[DescribeAlarmsResponse]                                                                   = eff1(client.describeAlarms)
    override def describeAlarms(a: DescribeAlarmsRequest): M[DescribeAlarmsResponse]                                         = eff1(client.describeAlarms(a))
    override def describeAlarmsForMetric(a: DescribeAlarmsForMetricRequest): M[DescribeAlarmsForMetricResponse]              = eff1(client.describeAlarmsForMetric(a))
    override def describeAlarmsPaginator: M[DescribeAlarmsPublisher]                                                         = primitive1(client.describeAlarmsPaginator)
    override def describeAlarmsPaginator(a: DescribeAlarmsRequest): M[DescribeAlarmsPublisher]                               = primitive1(client.describeAlarmsPaginator(a))
    override def describeAnomalyDetectors(a: DescribeAnomalyDetectorsRequest): M[DescribeAnomalyDetectorsResponse]           = eff1(client.describeAnomalyDetectors(a))
    override def describeAnomalyDetectorsPaginator(a: DescribeAnomalyDetectorsRequest): M[DescribeAnomalyDetectorsPublisher] = primitive1(client.describeAnomalyDetectorsPaginator(a))
    override def describeInsightRules(a: DescribeInsightRulesRequest): M[DescribeInsightRulesResponse]                       = eff1(client.describeInsightRules(a))
    override def describeInsightRulesPaginator(a: DescribeInsightRulesRequest): M[DescribeInsightRulesPublisher]             = primitive1(client.describeInsightRulesPaginator(a))
    override def disableAlarmActions(a: DisableAlarmActionsRequest): M[DisableAlarmActionsResponse]                          = eff1(client.disableAlarmActions(a))
    override def disableInsightRules(a: DisableInsightRulesRequest): M[DisableInsightRulesResponse]                          = eff1(client.disableInsightRules(a))
    override def enableAlarmActions(a: EnableAlarmActionsRequest): M[EnableAlarmActionsResponse]                             = eff1(client.enableAlarmActions(a))
    override def enableInsightRules(a: EnableInsightRulesRequest): M[EnableInsightRulesResponse]                             = eff1(client.enableInsightRules(a))
    override def getDashboard(a: GetDashboardRequest): M[GetDashboardResponse]                                               = eff1(client.getDashboard(a))
    override def getInsightRuleReport(a: GetInsightRuleReportRequest): M[GetInsightRuleReportResponse]                       = eff1(client.getInsightRuleReport(a))
    override def getMetricData(a: GetMetricDataRequest): M[GetMetricDataResponse]                                            = eff1(client.getMetricData(a))
    override def getMetricDataPaginator(a: GetMetricDataRequest): M[GetMetricDataPublisher]                                  = primitive1(client.getMetricDataPaginator(a))
    override def getMetricStatistics(a: GetMetricStatisticsRequest): M[GetMetricStatisticsResponse]                          = eff1(client.getMetricStatistics(a))
    override def getMetricStream(a: GetMetricStreamRequest): M[GetMetricStreamResponse]                                      = eff1(client.getMetricStream(a))
    override def getMetricWidgetImage(a: GetMetricWidgetImageRequest): M[GetMetricWidgetImageResponse]                       = eff1(client.getMetricWidgetImage(a))
    override def listDashboards: M[ListDashboardsResponse]                                                                   = eff1(client.listDashboards)
    override def listDashboards(a: ListDashboardsRequest): M[ListDashboardsResponse]                                         = eff1(client.listDashboards(a))
    override def listDashboardsPaginator: M[ListDashboardsPublisher]                                                         = primitive1(client.listDashboardsPaginator)
    override def listDashboardsPaginator(a: ListDashboardsRequest): M[ListDashboardsPublisher]                               = primitive1(client.listDashboardsPaginator(a))
    override def listManagedInsightRules(a: ListManagedInsightRulesRequest): M[ListManagedInsightRulesResponse]              = eff1(client.listManagedInsightRules(a))
    override def listManagedInsightRulesPaginator(a: ListManagedInsightRulesRequest): M[ListManagedInsightRulesPublisher]    = primitive1(client.listManagedInsightRulesPaginator(a))
    override def listMetricStreams(a: ListMetricStreamsRequest): M[ListMetricStreamsResponse]                                = eff1(client.listMetricStreams(a))
    override def listMetricStreamsPaginator(a: ListMetricStreamsRequest): M[ListMetricStreamsPublisher]                      = primitive1(client.listMetricStreamsPaginator(a))
    override def listMetrics: M[ListMetricsResponse]                                                                         = eff1(client.listMetrics)
    override def listMetrics(a: ListMetricsRequest): M[ListMetricsResponse]                                                  = eff1(client.listMetrics(a))
    override def listMetricsPaginator: M[ListMetricsPublisher]                                                               = primitive1(client.listMetricsPaginator)
    override def listMetricsPaginator(a: ListMetricsRequest): M[ListMetricsPublisher]                                        = primitive1(client.listMetricsPaginator(a))
    override def listTagsForResource(a: ListTagsForResourceRequest): M[ListTagsForResourceResponse]                          = eff1(client.listTagsForResource(a))
    override def putAnomalyDetector(a: PutAnomalyDetectorRequest): M[PutAnomalyDetectorResponse]                             = eff1(client.putAnomalyDetector(a))
    override def putCompositeAlarm(a: PutCompositeAlarmRequest): M[PutCompositeAlarmResponse]                                = eff1(client.putCompositeAlarm(a))
    override def putDashboard(a: PutDashboardRequest): M[PutDashboardResponse]                                               = eff1(client.putDashboard(a))
    override def putInsightRule(a: PutInsightRuleRequest): M[PutInsightRuleResponse]                                         = eff1(client.putInsightRule(a))
    override def putManagedInsightRules(a: PutManagedInsightRulesRequest): M[PutManagedInsightRulesResponse]                 = eff1(client.putManagedInsightRules(a))
    override def putMetricAlarm(a: PutMetricAlarmRequest): M[PutMetricAlarmResponse]                                         = eff1(client.putMetricAlarm(a))
    override def putMetricData(a: PutMetricDataRequest): M[PutMetricDataResponse]                                            = eff1(client.putMetricData(a))
    override def putMetricStream(a: PutMetricStreamRequest): M[PutMetricStreamResponse]                                      = eff1(client.putMetricStream(a))
    override def serviceName: M[String]                                                                                      = primitive1(client.serviceName)
    override def setAlarmState(a: SetAlarmStateRequest): M[SetAlarmStateResponse]                                            = eff1(client.setAlarmState(a))
    override def startMetricStreams(a: StartMetricStreamsRequest): M[StartMetricStreamsResponse]                             = eff1(client.startMetricStreams(a))
    override def stopMetricStreams(a: StopMetricStreamsRequest): M[StopMetricStreamsResponse]                                = eff1(client.stopMetricStreams(a))
    override def tagResource(a: TagResourceRequest): M[TagResourceResponse]                                                  = eff1(client.tagResource(a))
    override def untagResource(a: UntagResourceRequest): M[UntagResourceResponse]                                            = eff1(client.untagResource(a))
    override def waiter: M[CloudWatchAsyncWaiter]                                                                            = primitive1(client.waiter)

  }

}
