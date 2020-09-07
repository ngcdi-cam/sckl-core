package org.ngcdi.sckl.ryuclient

import akka.actor.ActorSystem
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import org.ngcdi.sckl.ryuclient.NetworkAwarenessService
import org.ngcdi.sckl.behaviour.SetupNetworkAwarenessManager

import scala.collection.mutable
import akka.actor.ActorRef
import org.ngcdi.sckl.behaviour.neighbouring.NameResolutionUtils
import akka.actor.ActorContext
import akka.util.Timeout

case class ControllerNotFoundException(id: Int)
    extends Exception(s"Could not find controller $id", None.orNull)

case class SwitchNotFoundException(id: Int)
    extends Exception(s"Could not find switch $id", None.orNull)

// object NetworkAwarenessManager {
//   def fromSetupInfo(
//       baseUrls: Seq[String],
//       topo: NetworkAwarenessTopology
//   ): NetworkAwarenessManager = {
//     val manager = new NetworkAwarenessManager(baseUrls)
//     manager.
//   }

//   def toSetupInfo(
//     manager: NetworkAwarenessManager
//   ): SetupNetworkAwarenessManager = {
//     SetupNetworkAwarenessManager()
//   }
// }

class NetworkAwarenessManager(baseUrls: Seq[String]) extends Serializable {

  var controllers = Seq.empty[NetworkAwarenessController]

  var initialized = false

  @transient lazy val switchActorRefCache =
    mutable.Map.empty[NetworkAwarenessSwitch, Future[ActorRef]]

  def init(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Unit] = {
    Future.sequence(baseUrls.zipWithIndex.map { 
      case (baseUrl, controllerId) => 
        val client = new NetworkAwarenessClient(baseUrl)
        
        for {
          links <- client.getLinks
          accessTableRaw <- client.getAccessTable
          ret <- Future {
            val topology = NetworkAwarenessTopology.fromLinks(links, controllerId)
            val accessTable = NetworkAwarenessAccessTable(accessTableRaw, topology)
            NetworkAwarenessController(controllerId, client, topology, accessTable)
          }
        } yield ret
    }).map { x => 
      controllers = x
      initialized = true
      Unit
    }
  }

  def resolveControllerOfSwitch(
      switch: NetworkAwarenessSwitch
  ): Option[NetworkAwarenessController] = {
    controllers.lift(switch.controllerId)
  }

  def setSwitchWeight(switch: NetworkAwarenessSwitch, weight: Double)(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Boolean] = {
    resolveControllerOfSwitch(switch)
      .map(_.client.setSwitchWeights(Map(switch.dpid -> weight)))
      .getOrElse(
        Future.failed(
          new Exception(
            "Could not resolve the controller of the specified switch."
          )
        )
      )
  }

  def getSwitchById(
      dpid: Int,
      controllerId: Int = 0
  ): Option[NetworkAwarenessSwitch] = {
    controllers.lift(controllerId).flatMap { controller =>
      controller.topology.switches.get(dpid)
    }
  }

  def getStats(
      controllerId: Int = 0
  )(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[NetworkAwarenessStatEntry]] = {
    controllers
      .lift(controllerId)
      .map(_.client.getStats)
      .getOrElse(Future.failed(ControllerNotFoundException(controllerId)))
  }

  def getSwitchStats(
      dpid: Int = 0,
      controllerId: Int = 0,
      filter: Boolean = true
  )(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[NetworkAwarenessStatEntry]] = {
    controllers
      .lift(controllerId)
      .map(_.client.getSwitchStats(dpid, filter))
      .getOrElse(Future.failed(ControllerNotFoundException(controllerId)))
  }

  def getSwitchFlows(
      dpid: Int = 0,
      controllerId: Int = 0,
      filter: Boolean = true
  )(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[NetworkAwarenessFlowEntry]] = {
    controllers
      .lift(controllerId)
      .map { x => x.client.getSwitchFlows(dpid, filter) }
      .getOrElse(Future.failed(ControllerNotFoundException(controllerId)))
  }

  def getSwitchStats(
      switch: NetworkAwarenessSwitch
  )(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[NetworkAwarenessStatEntry]] = {
    getSwitchStats(switch.dpid, switch.controllerId, true)
  }

  def getSwitchFlows(
      switch: NetworkAwarenessSwitch
  )(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[NetworkAwarenessFlowEntry]] = {
    getSwitchFlows(switch.dpid, switch.controllerId, true)
  }

  def installServices(
      services: Seq[NetworkAwarenessService],
      controllerId: Int = 0
  )(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Boolean] = {
    controllers
      .lift(controllerId)
      .map(_.client.setServices(services))
      .getOrElse(Future.failed(ControllerNotFoundException(controllerId)))
  }

  def getActorOfSwitch(
      switch: NetworkAwarenessSwitch
  )(implicit actorContext: ActorContext, timeout: Timeout): Future[ActorRef] = {
    switchActorRefCache.getOrElseUpdate(
      switch, {
        NameResolutionUtils.resolveNodeName(
          NameResolutionUtils.dpidToNodeHostName(switch.dpid)
        )
      }
    )
  }
}
