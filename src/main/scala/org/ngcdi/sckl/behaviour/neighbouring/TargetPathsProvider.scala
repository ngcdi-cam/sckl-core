package org.ngcdi.sckl.behaviour.neighbouring
import akka.actor.ActorPath
import org.ngcdi.sckl.ScklActor
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.pattern.retry
import scala.util.Failure
import scala.util.Success

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
    val future: Future[Unit] = Future.sequence(futureSeq).map( _ => Unit )
    future.onComplete {
      case Success(value) => 
        log.info("Target path name resolution successful")
      case Failure(exception) => 
        log.error("Failed to resolve names of target paths: " + exception)
    }
    future
  }
}
