package com.twitter.diffy.workflow

import javax.inject.Inject

import com.twitter.diffy.analysis.{JoinedEndpoint, DifferencesFilterFactory, JoinedDifferences}
import com.twitter.diffy.proxy.Settings
import com.twitter.diffy.util.{SimpleMessage, EmailSender}
import com.twitter.finatra.http.internal.marshalling.mustache.MustacheService
import com.twitter.logging.Logger
import com.twitter.util.{Duration, Future}

case class Endpoint(endpointName: String, count: Long, fields: Iterable[Field])
case class Field(fieldName: String, relativeDifference: String, absoluteDifference: String)

case class ReportData(
    delay: Duration,
    rootUrl: String,
    serviceName: String,
    criticalDiffs: Int,
    criticalEndpoints: Seq[Endpoint],
    passingEndpoints: Seq[Endpoint])

object ReportGenerator {
  val NormalizedUrlRegex = "(http://)?([^/]*)(/?)".r
  def normalizeUrl(url: String): String = url match {
    case NormalizedUrlRegex(_, rawUrl, _) => s"http://$rawUrl/"
  }
}

class ReportGenerator @Inject()(
    joinedDifferences: JoinedDifferences,
    settings: Settings,
    mustacheService: MustacheService)
{
  import ReportGenerator._

  private[this] val log: Logger = Logger(classOf[ReportGenerator])
  private[this] val emailSender = new EmailSender(log)

  private[this] def sendReport(report: ReportData) =
    emailSender(buildMessage(report))

  def buildSubject(serviceName: String, criticalDiffs: Int) =
    if (criticalDiffs == 0) {
      s"[diffy] ${serviceName} - All endpoints passed"
    } else {
      s"[diffy] ${serviceName} ${criticalDiffs} critical"
    }

  def buildMessage(report: ReportData) : SimpleMessage = {
    SimpleMessage(
      from = "Diffy <diffy@no-reply.com>",
      to = settings.teamEmail,
      bcc = settings.teamEmail,
      subject = buildSubject(report.serviceName, report.criticalDiffs),
      body = new String(mustacheService.createChannelBuffer("cron_report.mustache", report).toByteBuffer.array)
    )
  }

  private[this] val filter =
    DifferencesFilterFactory(
      settings.relativeThreshold,
      settings.absoluteThreshold
    )

  def extractReport(endpoints: Map[String, JoinedEndpoint]): ReportData = {
    val endpointGrouping =
      endpoints.map { case ((endpoint, joinedEndpoint)) =>
        Endpoint(
          endpoint,
          joinedEndpoint.total,
          joinedEndpoint.fields.collect {
            case ((path, field)) if filter(field) =>
              Field(path, "%1.2f".format(field.relativeDifference), "%1.2f".format(field.absoluteDifference))
          }
        )
      }.toSeq.groupBy(_.fields.size > 0)

    val criticalEndpoints = endpointGrouping.getOrElse(true, Seq.empty).sortBy(_.fields.size).reverse
    val passingEndpoints = endpointGrouping.getOrElse(false, Seq.empty).sortBy(_.endpointName)

    val fieldCount = criticalEndpoints.map(_.fields.size).sum

    ReportData(
      settings.emailDelay,
      normalizeUrl(settings.rootUrl),
      settings.serviceName,
      fieldCount,
      criticalEndpoints,
      passingEndpoints
    )
  }

  def conditionallySendReport(reportData: ReportData) =
    if (reportData.criticalDiffs == 0 && settings.skipEmailsWhenNoErrors) {
      Future.Unit
    } else {
      sendReport(reportData)
    }

  def sendEmail = {
    joinedDifferences.endpoints map { extractReport _ andThen conditionallySendReport }
  }

  def extractReportFromDifferences = {
    joinedDifferences.endpoints map extractReport
  }
}
