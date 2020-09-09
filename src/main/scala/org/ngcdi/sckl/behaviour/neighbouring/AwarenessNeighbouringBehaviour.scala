package org.ngcdi.sckl.behaviour.neighbouring

import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.behaviour.NetworkAwarenessManagerReceiverBehaviour
import org.ngcdi.sckl.behaviour.awareness.NetworkAwarenessSwitchProvider
import scala.util.Failure
import scala.util.Success
import org.ngcdi.sckl.msgs.SenseFlow
import org.ngcdi.sckl.Config

trait AwarenessNeighbouringBehaviour 
  extends ScklActor
  with TargetPathsProvider
  with NetworkAwarenessManagerReceiverBehaviour
  with NetworkAwarenessSwitchProvider {

  override def neighbouringBehaviourPrestart(): Unit = {
    awarenessSwitch.onComplete { 
      case Success(switch) => 
        val nodeNames = switch.getPeerSwitches.map { switch => NameResolutionUtils.dpidToNodeHostName(switch.dpid) }.toSeq
        log.info("Target paths: " + nodeNames)
        resolveNodeNamesWithRetry(nodeNames).foreach { Unit =>
          self ! SenseFlow(Config.keyHosts)
        }
      case Failure(exception) => 
        log.error("Failed to get awareness switch: " + exception)
    }
  }
}