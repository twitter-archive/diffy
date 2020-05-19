package com.twitter.diffy.lifter

import scala.collection.MapProxy

case class FieldMap[T](override val self: Map[String, T]) extends MapProxy[String, T] {
  override lazy val toString: String = {
    self.toSeq.sortBy { case (k, v) => k }.toString
  }
}