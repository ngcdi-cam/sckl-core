package org.ngcdi.sckl.behaviour
import akka.actor.ActorPath
import org.ngcdi.sckl.ScklActor

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
  
}