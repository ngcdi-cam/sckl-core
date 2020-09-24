package org.ngcdi.sckl.behaviour.anomaly

import org.ngcdi.sckl.AnomalyMessages.NewProcessedMeasurements

trait FlowCombinedDetectorAndActuatorBehaviour
    extends AbstractCombinedDetectorAndActuatorBehaviour
    with CongestionAnomalyDetectorBehaviour
    with FlowCongestionAnomalyActuatorBehaviour
    with OverheatingAnomalyDetectorBehaviour
    with OverheatingAnomalyActuatorBehaviour {
  

  final override def combinedDetectorAndActuatorPrestart() = {
    overheatingAnomalyDetectorPrestart()
    congestionAnomalyDetectorPrestart()
  }
  
  final override def anomalyActuatorBehaviour: Receive = {
    super[OverheatingAnomalyActuatorBehaviour].anomalyActuatorBehaviour
    .orElse(
      super[FlowCongestionAnomalyActuatorBehaviour].anomalyActuatorBehaviour
    )
  }

  final override def anomalyDetectorBehaviour: Receive = {
    case NewProcessedMeasurements(tick, sample) => 
      congestionDetector.run(tick, sample, congestionThreshold)
      overheatingDetector.run(tick, sample, overheatingThreshold)
  }

}
