package org.ngcdi.sckl.behaviour.neighbouring

import akka.actor.ActorContext
import org.ngcdi.sckl.ClusteringConfig
import org.ngcdi.sckl.Constants
import akka.util.Timeout
import scala.concurrent.Future
import akka.actor.ActorRef
import scala.collection.mutable
import akka.pattern.retry
import scala.concurrent.duration.FiniteDuration

object NameResolutionUtils {

  private val actorRefCache = mutable.Map.empty[String, Future[ActorRef]]

  def resolveNodeName(
      nodeName: String,
      attempts: Int = Constants.nameResolutionAttempts,
      delay: FiniteDuration = Constants.nameResolutionDelay
  )(implicit context: ActorContext, timeout: Timeout): Future[ActorRef] = {
    implicit val sched = context.system.scheduler
    implicit val ec = context.system.dispatcher

    val name = if (nodeName.startsWith("seed")) Constants.serviceManagerName else Constants.digitalAssetName
    
    retry(
      () =>
        actorRefCache.getOrElseUpdate(
          nodeName, {
            val address =
              s"akka://${ClusteringConfig.clusterName}@$nodeName:${ClusteringConfig.nodePort}/user/${name}"

            context.system.actorSelection(address).resolveOne()
          }
        ),
      attempts,
      delay
    )
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
