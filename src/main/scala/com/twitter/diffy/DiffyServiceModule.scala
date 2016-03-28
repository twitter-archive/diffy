package com.twitter.diffy

import com.google.inject.Provides
import com.twitter.diffy.analysis.{InMemoryDifferenceCollector, NoiseDifferenceCounter, RawDifferenceCounter, InMemoryDifferenceCounter}
import com.twitter.diffy.proxy.{Target, Settings}
import com.twitter.inject.TwitterModule
import com.twitter.util.TimeConversions._
import java.net.InetSocketAddress
import javax.inject.Singleton
import com.twitter.util.Duration

object DiffyServiceModule extends TwitterModule {
  val datacenter =
    flag("dc", "localhost", "the datacenter where this Diffy instance is deployed")

  val servicePort =
    flag("proxy.port", new InetSocketAddress(9992), "The port where the proxy service should listen")

  val candidatePath =
    flag[String]("candidate", "candidate serverset where code that needs testing is deployed")

  val primaryPath =
    flag[String]("master.primary", "primary master serverset where known good code is deployed")

  val secondaryPath =
    flag[String]("master.secondary", "secondary master serverset where known good code is deployed")

  val protocol =
    flag[String]("service.protocol", "Service protocol: thrift, http or https")

  val clientId =
    flag[String]("proxy.clientId", "diffy.proxy", "The clientId to be used by the proxy service to talk to candidate, primary, and master")

  val pathToThriftJar =
    flag[String]("thrift.jar", "path/to/thrift.jar", "The path to a fat Thrift jar")

  val serviceClass =
    flag[String]("thrift.serviceClass", "UserService", "The service name within the thrift jar e.g. UserService")

  val serviceName =
    flag[String]("serviceName", "Gizmoduck", "The service title e.g. Gizmoduck")

  val apiRoot =
    flag[String]("apiRoot", "", "The API root the front end should ping, defaults to the current host")

  val enableThriftMux =
    flag[Boolean]("enableThriftMux", true, "use thrift mux server and clients")

  val relativeThreshold =
    flag[Double]("threshold.relative", 20.0, "minimum (inclusive) relative threshold that a field must have to be returned")

  val absoluteThreshold =
    flag[Double]("threshold.absolute", 0.03, "minimum (inclusive) absolute threshold that a field must have to be returned")

  val teamEmail =
    flag[String]("notifications.targetEmail", "diffy-team@twitter.com", "team email to which cron report should be sent")

  val emailDelay =
    flag[Duration]("notifications.delay", 4.hours, "duration to wait before sending report out. e.g. 30.minutes or 4.hours")

  val rootUrl =
    flag[String]("rootUrl", "", "Root url to access this service, e.g. diffy-staging-gizmoduck.service.smf1.twitter.com")

  val allowHttpSideEffects =
    flag[Boolean]("allowHttpSideEffects", false, "Ignore POST, PUT, and DELETE requests if set to false")

  val excludeHttpHeadersComparison =
    flag[Boolean]("excludeHttpHeadersComparison", false, "Exclude comparison on HTTP headers if set to false")

  val skipEmailsWhenNoErrors =
    flag[Boolean]("skipEmailsWhenNoErrors", false, "Do not send emails if there are no critical errors")

  var httpsPort =
    flag[String]("httpsPort", "443", "Port to be used when using HTTPS as a protocol")

  @Provides
  @Singleton
  def settings =
    Settings(
      datacenter(),
      servicePort(),
      Target(candidatePath()),
      Target(primaryPath()),
      Target(secondaryPath()),
      protocol(),
      clientId(),
      pathToThriftJar(),
      serviceClass(),
      serviceName(),
      apiRoot(),
      enableThriftMux(),
      relativeThreshold(),
      absoluteThreshold(),
      teamEmail(),
      emailDelay(),
      rootUrl(),
      allowHttpSideEffects(),
      excludeHttpHeadersComparison(),
      skipEmailsWhenNoErrors(),
      httpsPort()
    )

  @Provides
  @Singleton
  def providesRawCounter = RawDifferenceCounter(new InMemoryDifferenceCounter)

  @Provides
  @Singleton
  def providesNoiseCounter = NoiseDifferenceCounter(new InMemoryDifferenceCounter)

  @Provides
  @Singleton
  def providesCollector = new InMemoryDifferenceCollector
}
