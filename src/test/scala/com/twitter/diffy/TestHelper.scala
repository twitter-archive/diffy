package com.twitter.diffy

import java.net.InetSocketAddress

import com.twitter.diffy.proxy._
import com.twitter.util.TimeConversions._
import org.scalatest.mock.MockitoSugar
import com.twitter.diffy.analysis._
import com.twitter.diffy.compare.Difference

object TestHelper extends MockitoSugar {
  lazy val testSettings = Settings(
    datacenter = "test",
    servicePort = new InetSocketAddress(9999),
    candidate = mock[Target],
    primary = mock[Target],
    secondary = mock[Target],
    protocol = "test",
    clientId = "test",
    pathToThriftJar = "test",
    serviceClass = "test",
    serviceName = "test",
    apiRoot = "test",
    enableThriftMux = false,
    relativeThreshold = 0.0,
    absoluteThreshold = 0.0,
    teamEmail = "test",
    emailDelay = 0.seconds,
    rootUrl = "test",
    allowHttpSideEffects = true,
    excludeHttpHeadersComparison = true,
    skipEmailsWhenNoErrors = false,
    httpsPort = "443"
  )

  def makeEmptyJoinedDifferences = {
    val rawCounter = RawDifferenceCounter(new InMemoryDifferenceCounter())
    val noiseCounter = NoiseDifferenceCounter(new InMemoryDifferenceCounter())
    JoinedDifferences(rawCounter, noiseCounter)
  }

  def makePopulatedJoinedDifferences(endpoint : String, diffs : Map[String, Difference]) = {
    val rawCounter = RawDifferenceCounter(new InMemoryDifferenceCounter())
    val noiseCounter = NoiseDifferenceCounter(new InMemoryDifferenceCounter())
    val data = new InMemoryEndpointMetadata()
    data.add(diffs)
    rawCounter.counter.asInstanceOf[InMemoryDifferenceCounter].endpointsMap += (endpoint -> data)

    JoinedDifferences(rawCounter, noiseCounter)
  }
}