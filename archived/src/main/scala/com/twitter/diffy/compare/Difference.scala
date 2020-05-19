package com.twitter.diffy.compare

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.diffy.lifter.{StringLifter, FieldMap, JsonLifter}
import com.twitter.util.Memoize
import java.nio.ByteBuffer
import scala.language.postfixOps

trait Difference {
  def flattened: Map[String, Difference]
  def toMap: Map[String, Any] = Map("type" -> getClass.getSimpleName)
}

trait TerminalDifference extends Difference {
  val flattened: Map[String, Difference] = Map(this.getClass.getSimpleName -> this)
}

case class NoDifference[A](value: A) extends TerminalDifference {
  override val flattened = Map.empty[String, Difference]
  override def toMap = Map.empty
}

case class TypeDifference[A, B](
    left: A,
    right: B)
  extends TerminalDifference
{
  private[this] def toMessage(obj: Any) = obj.getClass.getSimpleName + ": " + obj.toString
  override def toMap = super.toMap ++
    Seq("left" -> toMessage(left), "right" -> toMessage(right))
}

case class PrimitiveDifference[A](left: A, right: A) extends TerminalDifference {
  override def toMap = super.toMap ++ Seq("left" -> left, "right" -> right)
}

case object MissingField extends TerminalDifference {
  override def toMap = super.toMap ++ Seq("left" -> "present", "right" -> "nil")
}

case object ExtraField extends TerminalDifference {
  override def toMap = super.toMap ++ Seq("left" -> "nil", "right" -> "present")
}

trait SeqDifference extends Difference

case class OrderingDifference(
    leftPattern: Seq[Int],
    rightPattern: Seq[Int])
  extends TerminalDifference
  with SeqDifference
{
  override def toMap = super.toMap ++
    Seq("left" -> leftPattern, "right" -> rightPattern)
}

case class SeqSizeDifference[A](
    leftNotRight: Seq[A],
    rightNotLeft: Seq[A])
  extends TerminalDifference
  with SeqDifference
{
  override def toMap = super.toMap ++
    Seq("left" -> "missing elements: %s".format(leftNotRight),
      "right" -> "unexpected elements: %s".format(rightNotLeft))
}

case class IndexedDifference(indexedDiffs: Seq[Difference]) extends SeqDifference {
  override val flattened = indexedDiffs flatMap { _.flattened } toMap
  override def toMap = super.toMap +
    ("children" -> (indexedDiffs.zipWithIndex map { case (diff, index) => index -> diff.toMap }))
}

case class SetDifference[A](
    leftNotRight: Set[A],
    rightNotLeft: Set[A])
  extends TerminalDifference
{
  override def toMap = super.toMap ++
    Seq("left" -> leftNotRight, "right" -> rightNotLeft)
}

case class MapDifference[A](
    keys: TerminalDifference,
    values: Map[A, Difference])
  extends TerminalDifference
{
  val keysDifference =
    keys.flattened map { case (key, value) =>
      "keys.%s".format(key) -> value
    }

  val valuesDifference =
    values.values flatMap {
      _.flattened map { case (key, value) =>
        "values.%s".format(key) -> value
      }
    } toMap

  override val flattened = keysDifference ++ valuesDifference

  override def toMap = super.toMap ++
    Seq("keys" -> keys.toMap, "values" -> (values map { case (key, diff) => key -> diff.toMap }))
}

case class ObjectDifference(mapDiff: MapDifference[String]) extends Difference {
  override val flattened = {
    val existingKeys: Map[String, Difference] =
      (for {
        (key, diff) <- mapDiff.values
        (path, terminalDiff) <- diff.flattened
      } yield {
        "%s.%s".format(key, path) -> terminalDiff
      }) toMap

    val missingFields: Map[String, Difference] =
      mapDiff.keys match {
        case NoDifference(_) => Map.empty[String, Difference]
        case SetDifference(leftNotRight, rightNotLeft) =>
          (leftNotRight.toSeq map { _ + ".MissingField" -> MissingField } toMap) ++
          (rightNotLeft.toSeq map { _ + ".ExtraField" -> ExtraField } toMap)
      }

    existingKeys ++ missingFields
  }

  override def toMap = super.toMap +
    ("children" -> (mapDiff.values map { case (key, diff) => key -> diff.toMap }))
}

object Difference {
  def apply[A](left: Any, right: Any): Difference =
    (lift(left), lift(right)) match {
      case (l, r) if l == r => NoDifference(l)
      case (l, r) if isPrimitive(l) && l.getClass == r.getClass => PrimitiveDifference(l, r)
      case (ls: Seq[_], rs: Seq[_]) => diffSeq(ls, rs)
      case (ls: Set[A], rs: Set[A]) => diffSet(ls, rs)
      case (lm: FieldMap[Any], rm: FieldMap[Any]) => diffObjectMap(lm, rm)
      case (lm: Map[A, Any], rm: Map[A, Any]) => diffMap(lm, rm)
      case (l, r) if l.getClass != r.getClass => TypeDifference(l, r)
      case (l, r) => diffObject(l, r)
    }

  def diffSet[A](left: Set[A], right: Set[A]): TerminalDifference =
    if(left == right) NoDifference(left) else SetDifference(left -- right, right -- left)

  def diffSeq[A](left: Seq[A], right: Seq[A]): SeqDifference = {
    val leftNotRight = left diff right
    val rightNotLeft = right diff left

    if (leftNotRight ++ rightNotLeft == Nil) {
      def seqPattern(s: Seq[A]) = s map { left.indexOf(_) }
      OrderingDifference(seqPattern(left), seqPattern(right))
    } else if (left.length == right.length) {
      IndexedDifference((left zip right) map { case (le, re) => apply(le, re) })
    } else {
      SeqSizeDifference(leftNotRight, rightNotLeft)
    }
  }

  def diffMap[A](lm: Map[A, Any], rm: Map[A, Any]): MapDifference[A] =
    MapDifference (
      diffSet(lm.keySet, rm.keySet),
      (lm.keySet intersect  rm.keySet) map { key =>
          key -> apply(lm(key), rm(key))
      } toMap
    )

  def diffObjectMap(lm: FieldMap[Any], rm: FieldMap[Any]): ObjectDifference =
    ObjectDifference(diffMap(lm.toMap, rm.toMap))

  def diffObject[A](left: A, right: A): ObjectDifference =
    ObjectDifference(diffMap(mkMap(left), mkMap(right)))

  val isPrimitive: Any => Boolean = {
    case _: Unit => true
    case _: Boolean => true
    case _: Byte => true
    case _: Char => true
    case _: Short => true
    case _: Int => true
    case _: Long => true
    case _: Float => true
    case _: Double => true
    case _: String => true
    case _ => false
  }

  def mkMap(obj: Any): Map[String, Any] = mapMaker(obj.getClass)(obj)

  val mapMaker: Class[_] => (Any => Map[String, Any]) = Memoize { c =>
    val fields = c.getDeclaredFields filterNot { _.getName.contains('$') }
    fields foreach { _.setAccessible(true) }
    { obj: Any =>
        fields map { field =>
          field.getName -> field.get(obj)
        } toMap
    }
  }

  def lift(a: Any): Any = a match {
    case array: Array[_] => array.toSeq
    case byteBuffer: ByteBuffer => new String(byteBuffer.array)
    case jsonNode: JsonNode => JsonLifter.lift(jsonNode)
    case string: String => StringLifter.lift(string)
    case null => JsonLifter.JsonNull
    case _ => a
  }
}