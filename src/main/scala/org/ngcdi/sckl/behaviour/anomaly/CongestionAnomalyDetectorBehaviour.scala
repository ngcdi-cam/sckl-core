package org.ngcdi.sckl.behaviour.anomaly

import akka.actor._
import org.ngcdi.sckl.AnomalyMessages._
import org.ngcdi.sckl.anomalydetector.AwarenessCongestionAnomalyDetector
import org.ngcdi.sckl.Constants.congestionDetectorThreshold

trait CongestionAnomalyDetectorBehaviour extends AnomalyDetectorBehaviour {
  var congestionDetector: AwarenessCongestionAnomalyDetector = _
  val congestionThreshold = congestionDetectorThreshold

  // def getThreshold: Double = congestionThreshold
  // def getDetector: AwarenessCongestionAnomalyDetector = congestionDetector

  def congestionAnomalyDetectorPrestart(targetActor: ActorRef = self) = {
    congestionDetector = new AwarenessCongestionAnomalyDetector( { anomaly =>
      log.warning("!!! Congestion Anomaly Detected: " + anomaly)
      targetActor ! AnomalyDetected(anomaly)
    })
  }

  override def anomalyDetectorBehaviour: Receive = {
    case NewProcessedMeasurements(tick, sample) => 
      congestionDetector.run(tick, sample, congestionThreshold)
  }
}