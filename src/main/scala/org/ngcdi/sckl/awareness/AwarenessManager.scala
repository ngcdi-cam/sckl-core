package org.ngcdi.sckl.awareness

import akka.actor.ActorSystem
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import scala.collection.mutable
import akka.actor.ActorRef
import org.ngcdi.sckl.behaviour.neighbouring.NameResolutionUtils
import akka.actor.ActorContext
import akka.util.Timeout
import org.ngcdi.sckl.Constants

case class ControllerNotFoundException(id: Int)
    extends Exception(s"Could not find controller $id", None.orNull)

case class SwitchNotFoundException(id: Int)
    extends Exception(s"Could not find switch $id", None.orNull)

class AwarenessManager(baseUrls: Seq[String]) extends Serializable {

  var controllers = Seq.empty[AwarenessController]

  var initialized = false

  var topology: AwarenessTopology = null

  val edgeLinks = Constants.awarenessCrossDomainLinks

  @transient lazy val switchActorRefCache =
    mutable.Map.empty[AwarenessSwitch, Future[ActorRef]]

  def init(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Unit] = {
    Future
      .sequence(baseUrls.zipWithIndex.map {
        case (baseUrl, controllerId) =>
          val client = new AwarenessClient(baseUrl)

          for {
            links <- client.getLinks
            accessTable <- client.getAccessTable
            ret <- Future {
              val topology =
                AwarenessTopology(links, accessTable, controllerId)
              // val accessTable = AwarenessAccessTable(
              //   controllerId,
              //   accessTableRaw,
              //   topology
              // )
              AwarenessController(
                controllerId,
                client,
                topology
              )
            }
          } yield ret
      })
      .map { x =>
        topology = AwarenessTopology.join(
          x.map(_.topology).toSeq,
          edgeLinks
        )
        controllers = x
        topology.switches.toBuffer // workaround the java.io.NotSerializableException error. No idea why...
        initialized = true
        Unit
      }
  }

  def getControllerOfSwitch(
      switch: AwarenessSwitch
  ): Option[AwarenessController] = {
    controllers.lift(switch.controllerId)
  }

  def getClientOfSwitch(
      switch: AwarenessSwitch
  ): Option[AwarenessClient] = {
    getControllerOfSwitch(switch).map(_.client)
  }

  def setSwitchWeight(switch: AwarenessSwitch, weight: Double)(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Boolean] = {
    getControllerOfSwitch(switch)
      .map(_.client.setSwitchWeights(Map(switch.dpid -> weight)))
      .getOrElse(
        Future.failed(
          new ControllerNotFoundException(switch.controllerId)
        )
      )
  }

  def getSwitchById(
      dpid: Int,
      controllerId: Int = 0
  ): Option[AwarenessSwitch] = {
    topology.getSwitchById(dpid, controllerId)
    // controllers.lift(controllerId).flatMap { controller =>
    //   controller.topology.getSwitchById(dpid, controllerId)
    // }
  }

  def getSwitchByIdAnyController(
    dpid: Int
  ): Option[AwarenessSwitch] = {
    topology.getSwitchById(dpid)
  }

  def getStats(
      controllerId: Int = 0
  )(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[AwarenessRawStatEntry]] = {
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
  ): Future[Seq[AwarenessRawStatEntry]] = {
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
  ): Future[Seq[AwarenessRawFlowEntry]] = {
    controllers
      .lift(controllerId)
      .map { x => x.client.getSwitchFlows(dpid, filter) }
      .getOrElse(Future.failed(ControllerNotFoundException(controllerId)))
  }

  def getSwitchStats(
      switch: AwarenessSwitch
  )(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[AwarenessRawStatEntry]] = {
    getSwitchStats(switch.dpid, switch.controllerId, true)
  }

  def getSwitchFlows(
      switch: AwarenessSwitch
  )(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[AwarenessRawFlowEntry]] = {
    getSwitchFlows(switch.dpid, switch.controllerId, true)
  }

  def installServices(
      services: Seq[AwarenessService],
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
      switch: AwarenessSwitch
  )(implicit actorContext: ActorContext, timeout: Timeout): Future[ActorRef] = {
    switchActorRefCache.getOrElseUpdate(
      switch, {
        NameResolutionUtils.resolveNodeName(
          NameResolutionUtils.dpidToNodeHostName(switch.dpid)
        )
      }
    )
  }

  def getPathInfo(
      srcDstPairs: Seq[Tuple2[AwarenessSwitch, AwarenessSwitch]],
      weights: Map[String, Double],
      srcIp: String,
      dstIp: String
  )(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[AwarenessRawPathInfo] = {
    val controllerId = srcDstPairs(0)._1.controllerId
    assert(srcDstPairs.filterNot { x =>
      x._1.controllerId == controllerId && x._2.controllerId == controllerId
    }.size == 0)

    val pairsReq = srcDstPairs.map { x =>
      AwarenessRawPathInfoPairRequest(x._1.dpid, x._2.dpid, srcIp, dstIp)
    }

    controllers
      .lift(controllerId)
      .map(
        _.client
          .getPathInfo(AwarenessRawPathInfoRequest(pairsReq, weights))
      )
      .getOrElse(
        Future.failed(new ControllerNotFoundException(controllerId))
      )
  }
}
