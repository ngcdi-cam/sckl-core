package org.ngcdi.sckl.anomalydetector

import org.ngcdi.sckl.msgs.Measurement

abstract class AnomalyDetectionResult()
object Empty extends AnomalyDetectionResult()

abstract class AnomalyKind
object SingleAnomaly extends AnomalyKind
object GroupAnomaly extends AnomalyKind
object UndefinedAnomaly extends AnomalyKind

abstract class AbstractAnomalyDetector[O, R <: AnomalyDetectionResult](
    onFailure: R => Unit = AnomalyDetectorUtils.noHandleFailure,
    filter: Int => Boolean = AnomalyDetectorUtils.noFilter
) {
  val name: String = "UnnamedDetector"
  val kind: AnomalyKind = UndefinedAnomaly

  def check(
      tick: Int,
      sample: Seq[Measurement],
      options: O
  ): Option[R]

  final def run(
      tick: Int,
      sample: Seq[Measurement],
      options: O
  ): Option[R] = {
    if (!filter.apply(tick)) None
    //log.info(s"Running anomaly detector $name...")
    val result = check(tick, sample, options)
    if (result.isDefined) onFailure.apply(result.get)
    result
  }
}
