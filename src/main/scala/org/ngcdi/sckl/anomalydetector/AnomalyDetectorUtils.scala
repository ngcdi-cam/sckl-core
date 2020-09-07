package org.ngcdi.sckl.anomalydetector

object AnomalyDetectorUtils {
  def beginDetectionSinceTick(startTick: Int): (Int => Boolean) = { tick => tick >= startTick }
  val noFilter: (Int => Boolean) = { _ => true }
  val noHandleFailure: (Any => Unit ) = { _ => Unit }
}
