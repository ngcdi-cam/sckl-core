package org.ngcdi.sckl.behaviour

import org.ngcdi.sckl.msgs.NewMeasurements
import scala.util.Success
import scala.util.Failure
import scala.concurrent.duration.Duration
import org.ngcdi.sckl.Constants
import org.ngcdi.sckl.AwarenessMessages._

trait NetworkAwarenessStatsStreamerBehaviour
    extends NetworkAwarenessManagerReceiverBehaviour {

  // @depends: NetworkAwarenessManagerReceiver finishes initialization
  def awarenessStatsStreamerPrestart() = {
    context.system.scheduler.scheduleWithFixedDelay(
      Duration.Zero,
      Constants.awarenessStatsStreamerSenseInterval,
      self,
      DoGetAwarenessStats
    )
  }

  def awarenessStatsStreamerBehaviour(): Receive = {
    case DoGetAwarenessStats =>
      doGetAwarenessStats()
  }

  private def doGetAwarenessStats() = {
    if (awarenessManager == null) {
      log.info("Awareness manager not set up, ignoring request")
    } else {
      awarenessManager.getStats().onComplete {
        case Success(value) =>
          self ! NewMeasurements(value)
        case Failure(exception) =>
          log.error("Failed to get awareness stats: " + exception)
      }
    }
  }
}
