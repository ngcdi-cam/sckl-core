package org.ngcdi.sckl.actuator

import scala.concurrent.Future
import org.ngcdi.sckl.anomalydetector.AnomalyDetectionResult
import akka.actor.ActorSystem
import org.ngcdi.sckl.awareness.AwarenessManager

abstract class Actuator[T <: AnomalyDetectionResult, R] {
  def triggerAction(anomalyDetectionResult: T)(implicit actorSystem: ActorSystem, awarenessManager: Future[AwarenessManager]): R
}
