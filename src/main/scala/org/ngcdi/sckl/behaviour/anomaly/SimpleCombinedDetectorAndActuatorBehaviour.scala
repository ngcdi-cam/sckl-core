package org.ngcdi.sckl.behaviour.anomaly

import org.ngcdi.sckl.AnomalyMessages.NewProcessedMeasurements

trait SimpleCombinedDetectorAndActuatorBehaviour
    extends AbstractCombinedDetectorAndActuatorBehaviour
    with CongestionAnomalyDetectorBehaviour
    with CongestionAnomalyActuatorBehaviour
    with OverheatingAnomalyDetectorBehaviour
    with OverheatingAnomalyActuatorBehaviour {
  

  final override def combinedDetectorAndActuatorPrestart() = {
    overheatingAnomalyDetectorPrestart()
    congestionAnomalyDetectorPrestart()
  }
  
  final override def anomalyActuatorBehaviour: Receive = {
    super[OverheatingAnomalyActuatorBehaviour].anomalyActuatorBehaviour
    .orElse(
      super[CongestionAnomalyActuatorBehaviour].anomalyActuatorBehaviour
    )
  }

  final override def anomalyDetectorBehaviour: Receive = {
    case NewProcessedMeasurements(tick, sample) => 
      congestionDetector.run(tick, sample, congestionThreshold)
      overheatingDetector.run(tick, sample, overheatingThreshold)
  }

}
