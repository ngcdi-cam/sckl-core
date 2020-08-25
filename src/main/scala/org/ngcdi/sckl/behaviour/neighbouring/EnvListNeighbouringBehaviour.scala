package org.ngcdi.sckl.behaviour

import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.Config
import scala.concurrent.Future
import org.ngcdi.sckl.ClusteringConfig
import org.ngcdi.sckl.Constants
import scala.concurrent.duration._
import akka.pattern.after
import org.ngcdi.sckl.msgs.SenseFlow

trait EnvListNeighbouringBehaviour extends TargetPathsProvider {
  this: ScklActor =>

  private def resolveNeighbours(): Future[Unit] = {
    val futureSeq = Config.ngbNames.map { neighbourName =>
      val address =
        s"akka://${ClusteringConfig.clusterName}@$neighbourName:${ClusteringConfig.nodePort}/user/${Constants.digitalAssetName}"
      context.system.actorSelection(address).resolveOne().map { actorRef =>
        if (!neighbours.contains(actorRef.path))
          neighbours = neighbours :+ actorRef.path
        Unit
      }
    }
    Future.sequence(futureSeq).map({ x => Unit })
  }

  private def resolveNeighboursWithRetry(): Future[Unit] = {
    resolveNeighbours()
      .recoverWith {
        case _ =>
          log.error("Failed to resolve neighbours, retrying in 5 sec")
          after(5 seconds, context.system.scheduler)(resolveNeighboursWithRetry)
      }
      .map { Unit => 
        log.info("Neighbour resolution successful")
        self ! SenseFlow(Config.keyHosts)
        // log.info("Neighbours are: " + neighbours)
     }
  }

  override def neighbouringBehaviourPrestart(): Unit = {
    resolveNeighboursWithRetry()
  }
}
