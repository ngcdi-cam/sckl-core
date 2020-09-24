package org.ngcdi.sckl.behaviour.neighbouring

import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.behaviour.awareness.AwarenessManagerReceiverBehaviour
import org.ngcdi.sckl.behaviour.awareness.AwarenessSwitchProvider
import scala.util.Failure
import scala.util.Success
import org.ngcdi.sckl.msgs.SenseFlow
import org.ngcdi.sckl.Config

trait AwarenessNeighbouringBehaviour 
  extends ScklActor
  with TargetPathsProvider
  with AwarenessManagerReceiverBehaviour
  with AwarenessSwitchProvider {

  override def neighbouringBehaviourPrestart(): Unit = {
    awarenessSwitch.onComplete { 
      case Success(switch) => 
        val nodeNames = switch.getNeighbourSwitches.map { switch => NameResolutionUtils.dpidToNodeHostName(switch.dpid) }.toSeq
        log.info("Target paths: " + nodeNames)
        resolveNodeNames(nodeNames).foreach { Unit =>
          self ! SenseFlow(Config.keyHosts)
        }
      case Failure(exception) => 
        log.error("Failed to get awareness switch: " + exception)
    }
  }
}