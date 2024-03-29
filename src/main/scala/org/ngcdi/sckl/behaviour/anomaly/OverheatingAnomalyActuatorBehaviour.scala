package org.ngcdi.sckl.behaviour.anomaly

import org.ngcdi.sckl.actuator.OverheatingActuator
import org.ngcdi.sckl.Constants
import org.ngcdi.sckl.AnomalyMessages._
import org.ngcdi.sckl.anomalydetector.OverheatingAnomalyDetectionResult
import org.ngcdi.sckl.behaviour.awareness.AwarenessManagerReceiverBehaviour
import scala.util.Success
import scala.util.Failure

trait OverheatingAnomalyActuatorBehaviour
    extends AnomalyActuatorBehaviour
    with AwarenessManagerReceiverBehaviour {

  private val overheatingActuator = new OverheatingActuator(
    Constants.overheatingActuatorTemperatureWeightMap
  )

  override def anomalyActuatorBehaviour: Receive = {
    case AnomalyDetected(anomaly: OverheatingAnomalyDetectionResult) =>
      overheatingActuator.triggerAction(anomaly).onComplete {
        case Success(value) =>
          log.debug(
            "Successfully reduced switch weights as a result of overheating"
          )
        case Failure(exception) =>
          log.error(
            "Failed to reduce switch weights as a result of overheating: " + exception
          )
      }
  }
}
