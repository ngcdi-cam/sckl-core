package org.ngcdi.sckl.behaviour.neighbouring

import akka.actor.ActorContext
import org.ngcdi.sckl.ClusteringConfig
import org.ngcdi.sckl.Constants
import akka.util.Timeout
import scala.concurrent.Future
import akka.actor.ActorRef

object NameResolutionUtils {
  def resolveNodeName(nodeName: String)(implicit context: ActorContext, timeout: Timeout): Future[ActorRef] = {
    val address =
        s"akka://${ClusteringConfig.clusterName}@$nodeName:${ClusteringConfig.nodePort}/user/${Constants.digitalAssetName}"
        
    context.system.actorSelection(address).resolveOne()
  }

  def nodeNameToDpid(nodeName: String): Int = {
    nodeName.toInt
  }

  def dpidToNodeName(dpid: Int): String = {
    dpid.toString()
  }

  def dpidToNodeHostName(dpid: Int): String = {
    s"c$dpid"
  }
}