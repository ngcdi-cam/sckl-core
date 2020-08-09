package org.ngcdi.sckl.anomalydetector

import org.ngcdi.sckl.msgs.Measurement

case class AnomalyDetectionResult(
    detected: Boolean,
    anomalies: Seq[Measurement]
)
object Empty extends AnomalyDetectionResult(false, Seq.empty)

abstract class AnomalyKind
object SingleAnomaly extends AnomalyKind
object GroupAnomaly extends AnomalyKind
object UndefinedAnomaly extends AnomalyKind

abstract class AbstractAnomalyDetector[O, R](onFailure: R => Unit = { (x: R) => Unit }, filter: Int => Boolean = { x => true }) {

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
    result match {
      case Some(value) => 
        //log.info("Anomaly detected")
        onFailure.apply(value)
      case _           => Unit
    }
    result
  }
}
