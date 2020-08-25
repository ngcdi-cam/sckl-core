package org.ngcdi.sckl.behaviour

import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.ryuclient.NetworkAwarenessManager
import org.ngcdi.sckl.Config.awarenessServerUrl
import scala.util.{Success, Failure}
import akka.actor.ActorSelection
import akka.actor.ActorRef
import scala.concurrent.duration.DurationInt

trait NetworkAwarenessManagerSenderBehaviour extends ScklActor {
  implicit def funToRunnable(fun: () => Unit) =
    new Runnable() { def run() = fun() }

  val awarenessManager = new NetworkAwarenessManager(awarenessServerUrl)
  
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
      system.scheduler.scheduleOnce(
        DurationInt(5).second,
        { () =>
          log.info(
            "Awareness Manager is not initialized yet, will retry setup in 5 seconds"
          )
          setupAwarenessManager(actors)
        }
      )
    }

    actors ! SetupNetworkAwarenessManager(awarenessManager)
  }

  def setupAwarenessManager(actors: ActorRef): Unit = {
    setupAwarenessManager(ActorSelection(actors, ""))
  }
}
