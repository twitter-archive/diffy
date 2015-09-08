package com.twitter.diffy.analysis

import javax.inject.Inject

import com.twitter.diffy.compare.{Difference, PrimitiveDifference}
import com.twitter.diffy.lifter.{JsonLifter, Message}
import com.twitter.diffy.thriftscala._
import com.twitter.finagle.tracing.Trace
import com.twitter.logging._
import com.twitter.util.{Future, Time}
import com.twitter.util.StorageUnitConversions._
import scala.util.Random

object DifferenceAnalyzer {
  val UndefinedEndpoint = Some("Undefined_endpoint")
  val log = Logger(classOf[DifferenceAnalyzer])
  log.setUseParentHandlers(false)
  log.addHandler(
    FileHandler(
      filename = "differences.log",
      rollPolicy = Policy.MaxSize(128.megabytes),
      rotateCount = 2
    )()
  )

  def normalizeEndpointName(name: String) = name.replace("/", "-")
}

case class Field(endpoint: String, prefix: String)

class DifferenceAnalyzer @Inject()(
    rawCounter: RawDifferenceCounter,
    noiseCounter: NoiseDifferenceCounter,
    store: InMemoryDifferenceCollector)
{
  import DifferenceAnalyzer._

  def apply(
    request: Message,
    candidate: Message,
    primary: Message,
    secondary: Message
  ): Unit = {
    getEndpointName(request.endpoint, candidate.endpoint,
        primary.endpoint, secondary.endpoint) foreach { endpointName =>
      // If there is no traceId then generate our own
      val id = Trace.idOption map { _.traceId.toLong } getOrElse(Random.nextLong)

      val rawDiff = Difference(primary, candidate).flattened
      val noiseDiff = Difference(primary, secondary).flattened

      rawCounter.counter.count(endpointName, rawDiff)
      noiseCounter.counter.count(endpointName, noiseDiff)

      if (rawDiff.size > 0) {
        val diffResult = DifferenceResult(
          id,
          Trace.idOption map { _.traceId.toLong },
          endpointName,
          Time.now.inMillis,
          differencesToJson(rawDiff),
          JsonLifter.encode(request.result),
          Responses(
            candidate = JsonLifter.encode(candidate.result),
            primary = JsonLifter.encode(primary.result),
            secondary = JsonLifter.encode(secondary.result)
          )
        )

        log.info(s"diff[$id]=$diffResult")
        store.create(diffResult)
      } else {
        log.debug(s"diff[$id]=NoDifference")
      }
    }
  }

  def clear(): Future[Unit] = Future {
    rawCounter.counter.clear()
    noiseCounter.counter.clear()
    store.clear()
  }

  def differencesToJson(diffs: Map[String, Difference]): Map[String, String] =
    diffs map {
      case (field, diff @ PrimitiveDifference(_: Long, _)) =>
        field ->
          JsonLifter.encode(
            diff.toMap map {
              case (k, v) => k -> v.toString
            }
          )

      case (field, diff) => field -> JsonLifter.encode(diff.toMap)
    }

  private[this] def getEndpointName(
      requestEndpoint: Option[String],
      candidateEndpoint: Option[String],
      primaryEndpoint: Option[String],
      secondaryEndpoint: Option[String]): Option[String] = {
    val rawEndpointName = (requestEndpoint, candidateEndpoint, primaryEndpoint, secondaryEndpoint) match {
      case (Some(_), _, _, _) => requestEndpoint
      // undefined endpoint when action header is missing from all three instances
      case (_, None, None, None) => UndefinedEndpoint
      // the assumption is that primary and secondary should call the same endpoint,
      // otherwise it's noise and we should discard the request
      case (_, None, _, _) if primaryEndpoint == secondaryEndpoint => primaryEndpoint
      case (_, None, _, _) => None
      case (_, Some(candidate), _, _) => candidateEndpoint
    }

    rawEndpointName map { normalizeEndpointName(_) }
  }
}
