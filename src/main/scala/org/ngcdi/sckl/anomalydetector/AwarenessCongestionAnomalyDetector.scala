package org.ngcdi.sckl.anomalydetector

import org.ngcdi.sckl.msgs.Measurement
import org.ngcdi.sckl.Constants._

case class AwarenessCongestionAnomalyDetectionResult(
    val threshold: Double, // threshold of proportion of the total bandwidth
    val congestedSwitches: Map[String, Double] // link -> bandwidth usage proportion
) extends AnomalyDetectionResult

class AwarenessCongestionAnomalyDetector(
    onFailure: AwarenessCongestionAnomalyDetectionResult => Unit = { x =>
      Unit
    },
    filter: Int => Boolean = { x => true }
) extends AbstractAnomalyDetector[Double, AwarenessCongestionAnomalyDetectionResult](
      onFailure,
      filter
    ) {
  override val name: String = "AwarenessCongestionDetector"
  override val kind: AnomalyKind = SingleAnomaly

  override def check(
      tick: Int,
      sample: Seq[Measurement],
      options: Double // threshold
  ): Option[AwarenessCongestionAnomalyDetectionResult] = {
    val threshold = options

    val aggregatedSample = sample.groupBy(_.resourceId).map {
      case (link, measurements) => 
        Tuple2(link, measurements.groupBy(_.metricName).map {
          case (metricName, measurements) =>
            Tuple2(metricName, measurements.map(_.value).sum / measurements.size)
        }.toMap)
    }.toMap

    println("aggregatedSample: " + aggregatedSample)

    val congestedSwitches = aggregatedSample.filter {
      case (link, stats) =>
        stats.contains(awarenessThroughput) && stats.contains(awarenessBandwidth)
    }
    .map {
      case (link, stats) =>
        val usage = stats.get(awarenessThroughput).get / stats.get(awarenessBandwidth).get
        println("link: " + link + ", usage: " + usage)
        Tuple2(link, usage)
    }.filter(_._2 >= threshold).toMap

    if (congestedSwitches.isEmpty) {
      None
    } else {
      Some(AwarenessCongestionAnomalyDetectionResult(threshold, congestedSwitches))
    }
  }
}
