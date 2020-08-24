package org.ngcdi.sckl

import org.ngcdi.sckl.msgs.Measurement
import org.ngcdi.sckl.anomalydetector.AnomalyDetectionResult

object AnomalyMessages {
  // @name: NewProcessedMeasurements
  // @description: NewProcessedMeasurements should be sent by ServiceView when a set of processed measurements are available
  // @sender: ServiceView
  // @receiver: AnomalyDetector

  case class NewProcessedMeasurements(tick: Int, sample: Seq[Measurement])

  // @name: AnomalyDetected
  // @description: AnomalyDetected messages should be sent by subclasses of AnomalyDetector whenever a anomaly detected
  // @sender: subclass of this AnomalyDetector trait
  // @receiver: interested Actuators which react to the anomaly

  case class AnomalyDetected(anomaly: AnomalyDetectionResult)
}

object AwarenessMessages {

  // @name: DoGetAwarenessStats
  // @description: DoGetAwarenessStats messages are sent by NetworkAwarenessStatsStreamerBehaviour periodically to get the latest stats from the Network Awareness app
  // @sender: NetworkAwarenessStatsStreamerBehaviour periodically
  // @receiver: NetworkAwarenessStatsStreamerBehaviour

  object DoGetAwarenessStats
}