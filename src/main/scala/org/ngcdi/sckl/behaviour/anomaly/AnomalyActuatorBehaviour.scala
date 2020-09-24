package org.ngcdi.sckl.behaviour.anomaly

import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.AnomalyMessages._

trait AnomalyActuatorBehaviour extends ScklActor {
  
  // anomalyActuatorBehaviour should be overridden by the subclasses
  def anomalyActuatorBehaviour: Receive = {
    case AnomalyDetected(x) =>
      log.error("AnomalyDetected message not handled by a concrete AnomalyActuator")
  }
}