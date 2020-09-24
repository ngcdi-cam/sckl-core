package org.ngcdi.sckl.behaviour.awareness

import org.ngcdi.sckl.behaviour.awareness.AwarenessManagerReceiverBehaviour
import org.ngcdi.sckl.ClusteringConfig
import scala.concurrent.Future
import org.ngcdi.sckl.awareness.AwarenessSwitch
import org.ngcdi.sckl.behaviour.neighbouring.NameResolutionUtils

trait AwarenessSwitchProvider
    extends AwarenessManagerReceiverBehaviour {

  val awarenessSwitchId = NameResolutionUtils.nodeNameToDpid(ClusteringConfig.nodeName)
  val awarenessSwitch: Future[AwarenessSwitch] =
    awarenessManager.map( { manager =>
      val switch = manager.getSwitchByIdAnyController(awarenessSwitchId).get
      log.info(s"Managed switch: $switch")
      switch
    })
}
