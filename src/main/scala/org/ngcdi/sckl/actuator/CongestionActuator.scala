package org.ngcdi.sckl.actuator

import org.ngcdi.sckl.anomalydetector._
import org.ngcdi.sckl.awareness.AwarenessManager
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import org.ngcdi.sckl.awareness.AwarenessService
import akka.actor.ActorSystem

class CongestionActuator(servicesToInstall: Seq[AwarenessService])(
    implicit ec: ExecutionContext
) extends Actuator[AwarenessCongestionAnomalyDetectionResult, Future[Boolean]] {

  override def triggerAction(
      anomaly: AwarenessCongestionAnomalyDetectionResult
  )(implicit actorSystem: ActorSystem, awarenessManager: Future[AwarenessManager]): Future[Boolean] = {
    val controllerId = 0 // TODO: get controller ID from anomalyDetectionResult
    for {
      manager <- awarenessManager
      result <- manager.installServices(servicesToInstall, controllerId)
    } yield result
  }
}
