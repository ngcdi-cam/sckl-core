package org.ngcdi.sckl.anomalydetector

import org.ngcdi.sckl.msgs.Measurement
import scala.util.Try

case class CongestionAnomalyDetectionResult(
    val maxBandwidth: Double,
    val threshold: Double,
    val congestedSwitches: Set[String]
) extends AnomalyDetectionResult

class CongestionAnomalyDetector(
    onFailure: CongestionAnomalyDetectionResult => Unit = { x =>
      Unit
    },
    filter: Int => Boolean = { x => true }
) extends AbstractAnomalyDetector[Double, CongestionAnomalyDetectionResult](
      onFailure,
      filter
    ) {

  override val name = "PortStatsCongestionDetector"
  override val kind = SingleAnomaly

  override def check(
      tick: Int,
      sample: Seq[Measurement],
      options: Double // threshold
  ): Option[CongestionAnomalyDetectionResult] = {
    val bandwidthMetricId = "1" // FIXME: do not hardcode metric id here
    val threshold = options
    val congestedSwitches = sample
      .filter((m: Measurement) =>
        m.metricId == bandwidthMetricId && m.value > threshold
      )
      .groupBy(_.neId)
      .keySet

    val maxBandwidth = Try(
      sample
        .filter((m: Measurement) => m.metricId == bandwidthMetricId)
        .map(_.value)
        .max
    ).toOption.getOrElse(0.0)

    if (congestedSwitches.isEmpty) {
      None
    } else {
      Some(
        CongestionAnomalyDetectionResult(
          maxBandwidth,
          threshold,
          congestedSwitches
        )
      )
    }
  }
}
