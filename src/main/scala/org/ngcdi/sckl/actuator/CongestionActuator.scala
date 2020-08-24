package org.ngcdi.sckl.actuator

import org.ngcdi.sckl.anomalydetector._
import org.ngcdi.sckl.ryuclient.NetworkAwarenessManager
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import org.ngcdi.sckl.ryuclient.NetworkAwarenessService
import akka.actor.ActorSystem

class CongestionActuator(servicesToInstall: Seq[NetworkAwarenessService])(
    implicit ec: ExecutionContext
) extends Actuator[AwarenessCongestionAnomalyDetectionResult, Future[Boolean]] {

  override def triggerAction(
      anomaly: AwarenessCongestionAnomalyDetectionResult
  )(implicit actorSystem: ActorSystem, awarenessManager: NetworkAwarenessManager): Future[Boolean] = {
    val controllerId = 0 // TODO: get controller ID from anomalyDetectionResult
    if (awarenessManager == null) {
      actorSystem.log.warning("Awareness manager not ready, not triggering action")
      Future.failed(new Exception("Awareness manager not ready, not triggering action"))
    }
    else {
      awarenessManager.installServices(servicesToInstall, controllerId)
    }
  }
}
