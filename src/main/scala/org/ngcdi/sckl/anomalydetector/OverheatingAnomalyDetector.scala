package org.ngcdi.sckl.anomalydetector

import org.ngcdi.sckl.msgs.Measurement
import org.ngcdi.sckl.Constants.Temperature

case class OverheatingAnomalyDetectionResult(
    threshold: Double,
    overheatedSwitches: Map[Int, Double] // dpid -> temperature
) extends AnomalyDetectionResult

class OverheatingAnomalyDetector(
    onFailure: OverheatingAnomalyDetectionResult => Unit =
      AnomalyDetectorUtils.noHandleFailure,
    
    filter: Int => Boolean = AnomalyDetectorUtils.noFilter
) extends AbstractAnomalyDetector[Double, OverheatingAnomalyDetectionResult](
      onFailure,
      filter
    ) {

  override val name = "OverheatingDetector"
  override val kind = SingleAnomaly

  override def check(
      tick: Int,
      sample: Seq[Measurement],
      options: Double // threshold
  ): Option[OverheatingAnomalyDetectionResult] = {
    val temperatureMetricName = Temperature
    val threshold = options
    val overheatedSwitches = sample
      .filter((m: Measurement) =>
        m.metricName == temperatureMetricName && m.value > threshold
      )
      .map { x => Tuple2(x.neId.toInt, x.value) }
      .toMap

    if (overheatedSwitches.size == 0) {
      None
    } else {
      Some(
        OverheatingAnomalyDetectionResult(
          threshold,
          overheatedSwitches
        )
      )
    }
  }
}
