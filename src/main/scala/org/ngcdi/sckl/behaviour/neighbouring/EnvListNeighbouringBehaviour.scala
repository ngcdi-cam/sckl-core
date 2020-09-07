package org.ngcdi.sckl.behaviour.neighbouring

import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.Config

import org.ngcdi.sckl.msgs.SenseFlow

trait EnvListNeighbouringBehaviour extends TargetPathsProvider {
  this: ScklActor =>

  override def neighbouringBehaviourPrestart(): Unit = {
    resolveNodeNamesWithRetry(Config.ngbNames).foreach { Unit =>
      self ! SenseFlow(Config.keyHosts)
    }
  }
}
