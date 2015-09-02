package com.twitter.diffy.lifter

import com.twitter.diffy.ParentSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class StringLifterSpec extends ParentSpec {
  describe("String") {
    val htmlString = "<html><head>as</head><body><p>it's an html!</p></body></html>"
    val jsonString = """{"a": "it's a json!" }"""
    val regularString = "hello world!"

    it("should be true") {
      StringLifter.htmlRegexPattern.findFirstIn(htmlString).isDefined must be (true)
    }

    it("must return a FieldMap when lifted (html)") {
      StringLifter.lift(htmlString) mustBe a [FieldMap[_]]
    }

    it("must return a FieldMap when lifted (json)") {
      StringLifter.lift(jsonString) mustBe a [FieldMap[_]]
    }

    it("must return the original string when lifted") {
      StringLifter.lift(regularString) must be ("hello world!")
    }
  }
}