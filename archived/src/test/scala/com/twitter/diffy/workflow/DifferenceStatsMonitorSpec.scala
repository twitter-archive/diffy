package com.twitter.diffy.workflow

import com.twitter.diffy.ParentSpec
import com.twitter.diffy.analysis.{RawDifferenceCounter, EndpointMetadata, DifferenceCounter}
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.util.{Future, MockTimer, Time}
import com.twitter.util.TimeConversions._
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DifferenceStatsMonitorSpec extends ParentSpec {
  describe("DifferenceStatsMonitor"){
    val diffCounter = mock[DifferenceCounter]
    val metadata =
      new EndpointMetadata {
        override val differences = 0
        override val total = 0
      }

    val endpoints = Map("endpointName" -> metadata)
    when(diffCounter.endpoints) thenReturn Future.value(endpoints)

    val stats = new InMemoryStatsReceiver
    val timer = new MockTimer
    val monitor = new DifferenceStatsMonitor(RawDifferenceCounter(diffCounter), stats, timer)

    it("must add gauges after waiting a minute"){
      Time.withCurrentTimeFrozen { tc =>
        monitor.schedule()
        timer.tasks.size must be(1)
        stats.gauges.size must be(0)
        tc.advance(1.minute)
        timer.tick()
        timer.tasks.size must be(1)
        stats.gauges.size must be(2)
        stats.gauges.keySet map { _.takeRight(2) } must be(Set(Seq("endpointName", "total"), Seq("endpointName", "differences")))
      }
    }
  }
}
