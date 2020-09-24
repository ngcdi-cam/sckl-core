package org.ngcdi.sckl.behaviour.messagetransport

import org.ngcdi.sckl.ClusteringConfig
import org.ngcdi.sckl.Constants
import org.ngcdi.sckl.behaviour.neighbouring.NameResolutionUtils
import scala.concurrent.Promise
import scala.concurrent.Future
import org.ngcdi.sckl.ScklActor
import akka.actor.ActorRef
import scala.util.Success
import scala.util.Failure

trait EnvListTransportTopologyBehaviour extends TransportTopologyProvider {
  private val nodeName = ClusteringConfig.nodeIp
  log.info(s"Transport topology: ${Constants.transportTopology}")
  private val transportNeighbourNames = Constants.transportTopology.get(nodeName).get

  override def transportTopologyProviderPrestart() = {
    Future.sequence(
      transportNeighbourNames.map(NameResolutionUtils.resolveNodeName(_))
    ).onComplete { 
      case Success(value) => 
        log.info("My transport neighbours: " + value)
        transportNeighbourRefsPromise.success(value)
      case Failure(exception) => 
        transportNeighbourRefsPromise.failure(exception)
        log.error("Failed to resolve transport topology neighbour names")
        exception.printStackTrace()
    }
  }
}
