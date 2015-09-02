package com.twitter.diffy.lifter

import com.twitter.diffy.ParentSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JsonLifterSpec extends ParentSpec {
  describe("JsonLifter"){
    it("should correctly lift maps when keys are invalid identifier prefixes") {
      JsonLifter.lift(JsonLifter.decode("""{"1":1}""")) mustBe a [Map[_, _]]
    }

    it("should correctly lift objects when keys are valid identifier prefixes") {
      JsonLifter.lift(JsonLifter.decode("""{"a":1}""")) mustBe a [FieldMap[_]]
    }
  }
}
