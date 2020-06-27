package com.twitter.diffy.compare

import com.twitter.diffy.lifter.JsonLifter
import java.nio.ByteBuffer
import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.mock.MockitoSugar

@RunWith(classOf[JUnitRunner])
class DifferenceSpec
  extends FunSpec
  with MustMatchers
  with MockitoSugar
{
  case class Name(first: String, last: String)
  case class User(id: Long, name: Name)
  case class UserResult(found: Map[Long, User], notFound: Seq[Long])

  describe("Difference") {
    it("should detect when there is no difference between two objects") {
      val testCases =
        Seq(
          (true, true),
          (1, 1),
          ("hello", "hello"),
          (Seq(1,2,3), Seq(1,2,3)),
          (Array(1,2,3), Array(1,2,3)),
          (Set(1,2,3), Set(1,2,3)),
          (Map(1->"1", 2-> "2"), Map(1->"1", 2->"2")),
          (Name("puneet", "khanduri"), Name("puneet", "khanduri"))
        )
      testCases foreach { case (left, right) =>
        Difference(left, right) must be(NoDifference(Difference.lift(left)))
        Difference(JsonLifter(left), JsonLifter(right)) must be(NoDifference(Difference.lift(JsonLifter(left))))
      }
    }

    it("should detect when two primitives are not equal") {
      val testCases =
        Seq(
          (true, false),
          (1, 2),
          ("hello", "world"),
          (1.8, 3.9)
        )
      testCases foreach { case (left, right) =>
        Difference(left, right) must be(PrimitiveDifference(left,right))
      }
    }

    it("should throw TypeDifference when 2 primitives are of different types") {
      val testCases = Seq(
        (true, 1),
        (1, "hello"),
        ("hello", 1.2),
        (2, 3L),
        (3.2, 2),
        ("hello", false)
      )
      testCases foreach { case (left, right) =>
        Difference(left, right) must be(TypeDifference(left, right))
      }
    }

    it("should detect a difference between JsonNull and other objects") {
      val testCases = Seq(true, 1, "hello", Seq(1, 2, 3))

      testCases foreach { testCase =>
        Difference(JsonLifter.JsonNull, testCase) must
          be(TypeDifference(JsonLifter.JsonNull, testCase))
      }
    }

    it("should detect no difference when two JsonNull objects are compared") {
      Difference(JsonLifter.JsonNull, JsonLifter.JsonNull) must be(NoDifference(JsonLifter.JsonNull))
    }

    it("should return an OrderingDifference when two Seqs have the same elements but different order") {
      Difference(Seq("a", "b", "c"), Seq("c", "b", "a")) must be(
        OrderingDifference(leftPattern = Seq(0, 1, 2), rightPattern = Seq(2, 1, 0))
      )
    }

    it("should return a SeqSizeDifference when two Seqs are of different size") {
      Difference(Seq("a", "a", "b", "b"), Seq("a", "b", "b", "c", "c")) must be(
        SeqSizeDifference(
          leftNotRight = Seq("a"),
          rightNotLeft = Seq("c", "c")
        )
      )
    }

    it("should return a IndexedDifference when two Seqs are of the same size but have different elements") {
      Difference(Seq("a", "b", "c"), Seq("a", "b", "d")) must be(
        IndexedDifference(
          indexedDiffs =
            Seq(
              NoDifference("a"),
              NoDifference("b"),
              PrimitiveDifference("c", "d")
            )
        )
      )
    }

    it("should detect when two Sets are not equal") {
      Difference(Set(1, 2, 3), Set(2, 3, 4)) must be(
        SetDifference(leftNotRight = Set(1), rightNotLeft = Set(4))
      )
    }

    it("should detect when two Maps are not equal") {
      Difference(Map(1 -> 2, 2 -> 3, 3 -> 4), Map(1 -> 2, 2 -> 4, 4 -> 5)) must be(
        MapDifference(
          keys = SetDifference(leftNotRight = Set(3), rightNotLeft = Set(4)),
          values = Map(1 -> NoDifference(2), 2 -> PrimitiveDifference(3, 4))
        )
      )
    }

    it("should detect when two case class instances are not equal") {
      Difference(Name("puneet", "khanduri"), Name("prashant", "khanduri")) must be(
        ObjectDifference(MapDifference(
          keys = NoDifference(Set("first", "last")),
          values =
            Map(
              "first" -> PrimitiveDifference("puneet", "prashant"),
              "last" -> NoDifference("khanduri")
            )
        ))
      )
    }

    it("should detect difference between binaries embedded within thrift structs") {
      val left = ByteBuffer.wrap("leftBuffer".getBytes)
      val right = ByteBuffer.wrap("rightBuffer".getBytes)
      Difference(left,right) must be (
        PrimitiveDifference("leftBuffer", "rightBuffer")
      )
    }
  }
}
