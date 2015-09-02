package com.twitter.diffy.analysis

import com.twitter.diffy.compare.Difference
import com.twitter.util.Future

trait EndpointMetadata {
  // number of differences seen at this endpoint
  def differences: Int
  // total # of requests seen for this endpoint
  def total: Int
}

object FieldMetadata {
  val Empty = new FieldMetadata {
    override val differences = 0
    override val weight = 0
  }
}

trait FieldMetadata {
  // number of difference seen for this field
  def differences: Int
  // weight of this field relative to other fields, this number is calculated by counting the
  // number of fields that saw differences on every request that this field saw a difference in
  def weight: Int
}

trait DifferenceCounter {
  def count(endpoint: String, diffs: Map[String, Difference]): Future[Unit]
  def endpoints: Future[Map[String, EndpointMetadata]]
  def endpoint(endpoint: String) = endpoints flatMap { ep => Future { ep(endpoint) } }
  def fields(endpoint: String): Future[Map[String, FieldMetadata]]
  def clear(): Future[Unit]
}

case class RawDifferenceCounter(counter: DifferenceCounter)
case class NoiseDifferenceCounter(counter: DifferenceCounter)