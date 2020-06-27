package com.twitter.diffy

import com.twitter.diffy.analysis._
import com.twitter.diffy.thriftscala._
import com.twitter.diffy.lifter.JsonLifter
import scala.language.postfixOps

object Renderer {
  def differences(diffs: Map[String, String]) =
    diffs map { case (k, v) => k -> JsonLifter.decode(v) }

  def differenceResults(drs: Iterable[DifferenceResult], includeRequestResponses: Boolean = false) =
    drs map { differenceResult(_, includeRequestResponses) }

  def differenceResult(dr: DifferenceResult, includeRequestResponses: Boolean = false) =
    Map(
      "id" -> dr.id.toString,
      "trace_id" -> dr.traceId,
      "timestamp_msec" -> dr.timestampMsec,
      "endpoint" -> dr.endpoint,
      "differences" -> differences(dr.differences.toMap)
    ) ++ {
      if (includeRequestResponses) {
        Map(
          "request" -> JsonLifter.decode(dr.request),
          "left" -> JsonLifter.decode(dr.responses.primary),
          "right" -> JsonLifter.decode(dr.responses.candidate)
        )
      } else {
        Map.empty[String, Any]
      }
    }

  def endpoints(endpoints: Map[String, EndpointMetadata]) =
    endpoints map { case (ep, meta) =>
      ep -> endpoint(meta)
    }

  def endpoint(endpoint: EndpointMetadata) = Map(
    "total" -> endpoint.total,
    "differences" -> endpoint.differences
  )

  def field(field: FieldMetadata, includeWeight: Boolean) =
    Map("differences" -> field.differences) ++ {
      if (includeWeight) {
        Map("weight" -> field.weight)
      } else {
        Map.empty[String, Any]
      }
    }

  def field(field: JoinedField, includeWeight: Boolean) =
    Map(
      "differences" -> field.raw.differences,
      "noise" -> field.noise.differences,
      "relative_difference" -> field.relativeDifference,
      "absolute_difference" -> field.absoluteDifference
    ) ++ {
      if (includeWeight) Map("weight" -> field.raw.weight) else Map.empty
    }

  def fields(
    fields: Map[String, JoinedField],
    includeWeight: Boolean = false
  ) =
    fields map { case (path, meta) =>
      path -> field(meta, includeWeight)
    } toMap

  def error(message: String) =
    Map("error" -> message)

  def success(message: String) =
    Map("success" -> message)
}