package com.twitter.diffy.proxy

import java.net.InetSocketAddress

import com.twitter.app.Flaggable
import com.twitter.util.{Duration, Try}

case class Settings(
  datacenter: String,
  servicePort:InetSocketAddress,
  candidate: Target,
  primary: Target,
  secondary: Target,
  protocol: String,
  clientId: String,
  pathToThriftJar: String,
  serviceClass: String,
  serviceName: String,
  apiRoot: String,
  enableThriftMux: Boolean,
  relativeThreshold: Double,
  absoluteThreshold: Double,
  teamEmail: String,
  emailDelay: Duration,
  rootUrl: String,
  allowHttpSideEffects: Boolean,
  responseMode: ResponseMode,
  excludeHttpHeadersComparison: Boolean,
  skipEmailsWhenNoErrors: Boolean,
  httpsPort: String,
  hostname: String = Try(java.net.InetAddress.getLocalHost.toString).getOrElse("unknown"),
  user: String = Try(sys.env("USER")).getOrElse("unknown"))

case class Target(path: String)

sealed trait ResponseMode { def name: String }
object ResponseMode {
  case object EmptyResponse extends ResponseMode { val name = "empty" }
  case object FromPrimary   extends ResponseMode { val name = "primary" }
  case object FromSecondary extends ResponseMode { val name = "secondary" }
  case object FromCandidate extends ResponseMode { val name = "candidate" }

  implicit val flaggable: Flaggable[ResponseMode] = new Flaggable[ResponseMode] {
    override def parse(s: String): ResponseMode = s match {
      case EmptyResponse.name => EmptyResponse
      case FromPrimary.name   => FromPrimary
      case FromSecondary.name => FromSecondary
      case FromCandidate.name => FromCandidate
    }
    override def show(m: ResponseMode) = m.name
  }
}