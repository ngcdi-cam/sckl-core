package org.ngcdi.sckl.behaviour.awareness

import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.awareness.AwarenessManager
import org.ngcdi.sckl.Config.awarenessServerUrl
import scala.util.{Success, Failure}
import akka.actor.ActorSelection
import akka.actor.ActorRef
import scala.concurrent.duration.DurationInt

trait AwarenessManagerSenderBehaviour extends ScklActor {
  implicit def funToRunnable(fun: () => Unit) =
    new Runnable() { def run() = fun() }

  val awarenessManager = new AwarenessManager(awarenessServerUrl)
  
  awarenessManager.init.onComplete {
    case Success(value) =>
      log.debug("Network awareness manager initialized")

    case Failure(exception) =>
      log.error(
        "Failed to initialize network awareness manager: " + exception
      )
  }

  def setupAwarenessManager(actors: ActorSelection): Unit = {
    if (!awarenessManager.initialized) {
      log.info("Awareness Manager is not initialized yet, will retry setup in 5 seconds")
      system.scheduler.scheduleOnce(
        DurationInt(5).second, { () =>
          setupAwarenessManager(actors)
        }
      )
    }
    else {
      actors ! SetupAwarenessManager(awarenessManager)
    }
  }

  def setupAwarenessManager(actors: ActorRef): Unit = {
    setupAwarenessManager(ActorSelection(actors, ""))
  }
}
