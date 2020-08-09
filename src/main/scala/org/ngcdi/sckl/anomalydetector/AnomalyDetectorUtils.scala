package org.ngcdi.sckl.anomalydetector

object AnomalyDetectorUtils {
  def beginDetectionSinceTick(startTick: Int): (Int => Boolean) = { tick => tick >= startTick }
}
