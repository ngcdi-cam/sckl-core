package org.ngcdi.sckl.behaviour.awareness

import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.awareness.AwarenessManager
import scala.concurrent.{ Future, Promise }

case class SetupAwarenessManager(manager: AwarenessManager)

trait AwarenessManagerReceiverBehaviour extends ScklActor {
  private val awarenessManagerPromise = Promise[AwarenessManager]()
  implicit val awarenessManager: Future[AwarenessManager] = awarenessManagerPromise.future

  def awarenessManagerReceiverBehaviour: Receive = {
    case SetupAwarenessManager(manager) =>
      log.info("Awareness Manager is set up")
      awarenessManagerPromise.success(manager)
  }
}
