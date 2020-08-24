package org.ngcdi.sckl.behaviour

import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.ryuclient.NetworkAwarenessManager

case class SetupNetworkAwarenessManager(manager: NetworkAwarenessManager)

trait NetworkAwarenessManagerReceiverBehaviour extends ScklActor {
  implicit var awarenessManager: NetworkAwarenessManager = _

  def networkAwarenessManagerReceiverBehaviour: Receive = {
    case SetupNetworkAwarenessManager(manager) =>
      this.awarenessManager = manager
      log.info("Awareness Manager is set up")
  }
}
