package org.ngcdi.sckl.behaviour.neighbouring
import akka.actor.ActorPath
import org.ngcdi.sckl.ScklActor
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.pattern.after

trait TargetPathsProvider {
  this: ScklActor =>

  protected var neighbours = Seq.empty[ActorPath]

  def getTargetPaths(): Seq[ActorPath] = {
    return neighbours
  }

  def neighbouringBehaviourPrestart(): Unit = {}

  final def addTargetPath(path: ActorPath) = {
    if (!neighbours.contains(path)) {
      neighbours = neighbours :+ path
    }
  }

  final def addTargetPaths(paths: Seq[ActorPath]) = {
    paths.foreach(addTargetPath(_))
  }

  final def resolveNodeNames(nodeNames: Seq[String]): Future[Unit] = {
    val futureSeq = nodeNames.map { neighbourName =>
      NameResolutionUtils.resolveNodeName(neighbourName).map { actorRef =>
        if (!neighbours.contains(actorRef.path))
          neighbours = neighbours :+ actorRef.path
        Unit
      }
    }
    Future.sequence(futureSeq).map({ _ => Unit })
  }

  final def resolveNodeNamesWithRetry(
      nodeNames: Seq[String]
  ): Future[Unit] = {
    resolveNodeNames(nodeNames)
      .recoverWith {
        case _ =>
          log.error("Failed to resolve node names, retrying in 5 sec")
          after(5 seconds, context.system.scheduler)(resolveNodeNamesWithRetry(nodeNames))
      }
      .map { Unit =>
        log.info("Node name resolution successful")
      // log.info("Neighbours are: " + neighbours)
      }
  }

}
