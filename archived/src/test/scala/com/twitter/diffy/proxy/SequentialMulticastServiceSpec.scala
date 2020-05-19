package com.twitter.diffy.proxy

import com.twitter.diffy.ParentSpec
import com.twitter.finagle.Service
import com.twitter.util._
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SequentialMulticastServiceSpec extends ParentSpec {

  describe("SequentialMulticastService"){
    val first, second = mock[Service[String, String]]
    val multicastHandler = new SequentialMulticastService(Seq(first, second))

    it("must not access second until first is done"){
      val firstResponse, secondResponse = new Promise[String]
      when(first("anyString")) thenReturn firstResponse
      when(second("anyString")) thenReturn secondResponse
      val result = multicastHandler("anyString")
      verify(first)("anyString")
      verifyZeroInteractions(second)
      firstResponse.setValue("first")
      verify(second)("anyString")
      secondResponse.setValue("second")
      Await.result(result) must be(Seq(Try("first"), Try("second")))
    }

    it("should call all services") {
      val request = "anyString"
      val services = Seq.fill(100)(mock[Service[String, Int]])
      val responses = Seq.fill(100)(new Promise[Int])
      val svcResp = services zip responses
      svcResp foreach { case (service, response) =>
        when(service(request)) thenReturn response
      }
      val sequentialMulticast = new SequentialMulticastService(services)
      val result = sequentialMulticast("anyString")
      def verifySequentialInteraction(s: Seq[((Service[String,Int], Promise[Int]), Int)]): Unit = s match {
        case Nil =>
        case Seq(((svc, resp), index), tail@_*)  => {
          verify(svc)(request)
          tail foreach { case ((subsequent, _), _) =>
            verifyZeroInteractions(subsequent)
          }
          resp.setValue(index)
          verifySequentialInteraction(tail)
        }
      }
      verifySequentialInteraction(svcResp.zipWithIndex)
      Await.result(result) must be((0 until 100).toSeq map {i => Try(i)})
    }
  }
}
