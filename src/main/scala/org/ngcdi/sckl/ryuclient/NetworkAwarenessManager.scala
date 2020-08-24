package org.ngcdi.sckl.ryuclient

import akka.actor.ActorSystem
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import org.ngcdi.sckl.ryuclient.NetworkAwarenessService
import org.ngcdi.sckl.behaviour.SetupNetworkAwarenessManager

case class ControllerNotFoundException(id: Int)
    extends Exception(s"Could not find controller $id", None.orNull)

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

  var controllers: Array[NetworkAwarenessController] = _

  var initialized = false

  def init(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Unit] = {
    val clients = baseUrls.map { baseUrl =>
      new NetworkAwarenessClient(baseUrl)
    }
    val linkFutures = clients.map { client => client.getLinks }
    val links = Future.sequence(linkFutures)
    val ret = links.map { links =>
      controllers = links
        .zip(clients)
        .zipWithIndex
        .map {
          case ((singleControllerLinks, client), controllerId) =>
            new NetworkAwarenessController(
              controllerId,
              client,
              NetworkAwarenessTopology
                .fromLinks(singleControllerLinks, controllerId)
            )
        }
        .toArray
    }
    ret.map { x => 
      initialized = true
      x
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
}
