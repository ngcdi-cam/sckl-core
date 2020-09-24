package org.ngcdi.sckl.behaviour.anomaly

import akka.actor._
import org.ngcdi.sckl.anomalydetector.OverheatingAnomalyDetector
import org.ngcdi.sckl.Constants
import org.ngcdi.sckl.AnomalyMessages._

trait OverheatingAnomalyDetectorBehaviour extends AnomalyDetectorBehaviour {
  var overheatingDetector: OverheatingAnomalyDetector = _

  val overheatingThreshold =
    Constants.overheatingActuatorTemperatureWeightMap.keySet
      .map { case (low, high) => low }
      .filterNot(_ == Double.NegativeInfinity)
      .min
  

  def overheatingAnomalyDetectorPrestart(
      targetActor: ActorRef = self
  ) = {
    overheatingDetector = new OverheatingAnomalyDetector({ anomaly =>
      log.warning("!!! Overheating Anomaly Detected: " + anomaly)
      
      // notify the anomaly to the relevant actuators
      targetActor ! AnomalyDetected(anomaly)
    })
  }

  override def anomalyDetectorBehaviour: Receive = {
    case NewProcessedMeasurements(tick, sample) =>
      overheatingDetector.run(tick, sample, overheatingThreshold)
  }
}
