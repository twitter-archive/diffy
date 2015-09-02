package com.twitter.diffy.analysis

import javax.inject.Inject

import com.twitter.util.Future
import scala.math.abs

object DifferencesFilterFactory {
  def apply(relative: Double, absolute: Double): JoinedField => Boolean = {
    (field: JoinedField) =>
      field.raw.differences > field.noise.differences &&
        field.relativeDifference > relative &&
        field.absoluteDifference > absolute
  }
}

case class JoinedDifferences @Inject() (raw: RawDifferenceCounter, noise: NoiseDifferenceCounter) {
  lazy val endpoints: Future[Map[String, JoinedEndpoint]] = {
    raw.counter.endpoints map { _.keys } flatMap { eps =>
      Future.collect(
        eps map { ep =>
          endpoint(ep) map { ep -> _ }
        } toSeq
      ) map { _.toMap }
    }
  }

  def endpoint(endpoint: String): Future[JoinedEndpoint] = {
    Future.join(
      raw.counter.endpoint(endpoint),
      raw.counter.fields(endpoint),
      noise.counter.fields(endpoint)
    ) map { case (endpoint, rawFields, noiseFields) =>
      JoinedEndpoint(endpoint, rawFields, noiseFields)
    }
  }
}

case class JoinedEndpoint(
  endpoint: EndpointMetadata,
  original: Map[String, FieldMetadata],
  noise: Map[String, FieldMetadata])
{
  def differences = endpoint.differences
  def total = endpoint.total
  lazy val fields: Map[String, JoinedField] = original map { case (path, field) =>
    path -> JoinedField(endpoint, field, noise.getOrElse(path, FieldMetadata.Empty))
  } toMap
}

case class JoinedField(endpoint: EndpointMetadata, raw: FieldMetadata, noise: FieldMetadata) {
  // the percent difference out of the total # of requests
  def absoluteDifference = abs(raw.differences - noise.differences) / endpoint.total.toDouble * 100
  // the square error between this field's differences and the noisey counterpart's differences
  def relativeDifference = abs(raw.differences - noise.differences) / (raw.differences + noise.differences).toDouble * 100
}
