package com.twitter.diffy.lifter

import com.twitter.diffy.ParentSpec
import com.twitter.diffy.compare.{Difference, PrimitiveDifference}
import org.jsoup.Jsoup
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HtmlLifterSpec extends ParentSpec {
  describe("HtmlLifter"){
    val simpleActualHtml = """<html><head><title>Sample HTML</title></head><body><div class="header"><h1 class="box">Hello World</h1></div><p>Lorem ipsum dolor sit amet.</p></body></html>"""
    val simpleExpectedHtml = """<html><head><title>Sample HTML</title></head><body><div class="header"><h1 class="round">Hello World</h1></div><p>Lorem ipsum dolor sit amet.</p></body></html>"""

    val simpleActualDoc = Jsoup.parse(simpleActualHtml)
    val simpleExpectedDoc = Jsoup.parse(simpleExpectedHtml)

    it("should return a FieldMap") {
      HtmlLifter.lift(simpleActualDoc) mustBe a [FieldMap[_]]
    }

    it("should return a Primitive Difference") {
      Difference(HtmlLifter.lift(simpleActualDoc), HtmlLifter.lift(simpleExpectedDoc)).flattened must be (FieldMap(Map("body.children.children.attributes.class.PrimitiveDifference" -> PrimitiveDifference("box","round"))))
    }
  }
}