package org.ngcdi.sckl.behaviour

import org.ngcdi.sckl.actuator.CongestionActuator
import org.ngcdi.sckl.Constants
import org.ngcdi.sckl.AnomalyMessages._
import org.ngcdi.sckl.anomalydetector.AwarenessCongestionAnomalyDetectionResult
import scala.util.Success
import scala.util.Failure

trait CongestionAnomalyActuatorBehaviour
    extends AnomalyActuatorBehaviour
    with NetworkAwarenessManagerReceiverBehaviour {

  private val actuator = new CongestionActuator(
    Constants.servicesToInstall
  )

  override def anomalyActuatorBehaviour: Receive = {
    case AnomalyDetected(anomaly: AwarenessCongestionAnomalyDetectionResult) =>
      actuator.triggerAction(anomaly).onComplete {
        case Success(value) =>
          log.info(
            "Successfully updated services as a result of congestion"
          )
        case Failure(exception) =>
          log.error(
            "Failed to update services as a result of congestion: " + exception
          )
      }
  }
}
