package org.ngcdi.sckl.behaviour.anomaly

import org.ngcdi.sckl.ScklActor

trait AbstractCombinedDetectorAndActuatorBehaviour extends ScklActor {
  def combinedDetectorAndActuatorPrestart() = {}
  def anomalyActuatorBehaviour(): Receive = PartialFunction.empty
  def anomalyDetectorBehaviour: Receive = PartialFunction.empty
}
