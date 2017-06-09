package com.twitter.diffy.proxy

import java.net.InetSocketAddress

import com.twitter.util.Duration

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
  excludeHttpHeadersComparison: Boolean,
  skipEmailsWhenNoErrors: Boolean,
  httpsPort: String)

case class Target(path: String)
