package org.ngcdi.sckl.actuator

import org.ngcdi.sckl.anomalydetector._
import org.ngcdi.sckl.ryuclient.NetworkAwarenessManager
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem

class OverheatingActuator(temperatureWeightMap: Map[(Double, Double), Double])(implicit
    ec: ExecutionContext,
) extends Actuator[OverheatingAnomalyDetectionResult, Future[Seq[Boolean]]] {

  private def mapTemperatureToWeight(temp: Double): Double = {
    temperatureWeightMap
      .find { case ((low, high), weight) => temp >= low && temp < high }
      .map { case ((low, high), weight) => weight}
      .getOrElse(0.0)
  }

  override def triggerAction(
      anomalyDetectionResult: OverheatingAnomalyDetectionResult
  )(implicit actorSystem: ActorSystem, awarenessManager: NetworkAwarenessManager): Future[Seq[Boolean]] = {

    if (awarenessManager == null) {
      actorSystem.log.warning("Awareness manager not ready, not triggering action")
      Future.failed(new Exception("Awareness manager not ready, not triggering action"))
    }
    else {
      val controllerId = 0 // TODO: get controller ID from anomalyDetectionResult

      val futures = anomalyDetectionResult.overheatedSwitches.map {
        case (dpid, temp) =>
          val switch = awarenessManager.getSwitchById(dpid, controllerId).get
          val weight = mapTemperatureToWeight(temp)
          actorSystem.log.info(s"The weight for temperature $temp is $weight")
          awarenessManager.setSwitchWeight(switch, weight)
      }.toSeq

      Future.sequence(futures)
    }
  }
}
