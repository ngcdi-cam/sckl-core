package org.ngcdi.sckl.behaviour

import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.ryuclient.NetworkAwarenessManager
import scala.concurrent.{ Future, Promise }

case class SetupNetworkAwarenessManager(manager: NetworkAwarenessManager)

trait NetworkAwarenessManagerReceiverBehaviour extends ScklActor {
  private val awarenessManagerPromise = Promise[NetworkAwarenessManager]()
  implicit val awarenessManager: Future[NetworkAwarenessManager] = awarenessManagerPromise.future

  def networkAwarenessManagerReceiverBehaviour: Receive = {
    case SetupNetworkAwarenessManager(manager) =>
      log.info("Awareness Manager is set up")
      awarenessManagerPromise.success(manager)
  }
}
