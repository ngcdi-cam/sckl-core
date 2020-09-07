package org.ngcdi.sckl.behaviour.awareness

import org.ngcdi.sckl.behaviour.NetworkAwarenessManagerReceiverBehaviour
import org.ngcdi.sckl.ClusteringConfig
import scala.concurrent.Future
import org.ngcdi.sckl.ryuclient.NetworkAwarenessSwitch
import org.ngcdi.sckl.behaviour.neighbouring.NameResolutionUtils

trait NetworkAwarenessSwitchProvider
    extends NetworkAwarenessManagerReceiverBehaviour {

  val awarenessSwitchId = NameResolutionUtils.nodeNameToDpid(ClusteringConfig.nodeName)
  val awarenessSwitch: Future[NetworkAwarenessSwitch] =
    awarenessManager.map( { manager =>
      val switch = manager.getSwitchById(awarenessSwitchId).get
      log.info("My switch id is " + switch.dpid)
      switch
    })
}
