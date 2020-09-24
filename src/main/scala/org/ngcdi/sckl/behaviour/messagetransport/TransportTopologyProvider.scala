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

trait TransportTopologyProvider extends ScklActor {
  val transportNeighbourRefsPromise = Promise[Seq[ActorRef]]
  val transportNeighbourRefs = transportNeighbourRefsPromise.future

  def transportTopologyProviderPrestart()
}
