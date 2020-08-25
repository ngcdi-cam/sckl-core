package org.ngcdi.sckl.behaviour

import org.ngcdi.sckl.AnomalyMessages.NewProcessedMeasurements

trait CombinedDetectorAndActuatorBehaviour
    extends CongestionAnomalyDetectorBehaviour
    with CongestionAnomalyActuatorBehaviour
    with OverheatingAnomalyDetectorBehaviour
    with OverheatingAnomalyActuatorBehaviour {
  

  def combinedDetectorAndActuatorPrestart() = {
    overheatingAnomalyDetectorPrestart()
    congestionAnomalyDetectorPrestart()
  }
  
  override def anomalyActuatorBehaviour: Receive = {
    super[OverheatingAnomalyActuatorBehaviour].anomalyActuatorBehaviour
    .orElse(
      super[CongestionAnomalyActuatorBehaviour].anomalyActuatorBehaviour
    )
  }

  override def anomalyDetectorBehaviour: Receive = {
    case NewProcessedMeasurements(tick, sample) => 
      congestionDetector.run(tick, sample, congestionThreshold)
      overheatingDetector.run(tick, sample, overheatingThreshold)
  }

}
