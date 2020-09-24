package org.ngcdi.sckl.behaviour.anomaly

import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.AnomalyMessages._

trait AnomalyDetectorBehaviour extends ScklActor {
  
  // anomalyDetectorBehaviour should be overridden by the subclasses
  def anomalyDetectorBehaviour: Receive = {
    case NewProcessedMeasurements(tick, sample) =>
      log.error("NewProcessedMeasurements message not handled by a concrete AnomalyDetector")
  }
}
