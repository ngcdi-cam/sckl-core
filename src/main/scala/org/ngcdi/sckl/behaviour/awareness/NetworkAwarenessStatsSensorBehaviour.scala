package org.ngcdi.sckl.behaviour

import org.ngcdi.sckl.msgs.NewMeasurements
import scala.util.Success
import scala.util.Failure
import scala.concurrent.duration.Duration
import org.ngcdi.sckl.Constants
import org.ngcdi.sckl.AwarenessMessages._

// Trait for getting full network awareness stats periodically
// See also: NetworkAwarenessSwitchStatsSensorBehaviour

trait NetworkAwarenessStatsSensorBehaviour
    extends NetworkAwarenessManagerReceiverBehaviour {

  private var stopRequest = false
  // @depends: NetworkAwarenessManagerReceiver finishes initialization
  def awarenessStatsSensorPrestart() = {
    doGetAwarenessStats()
  }

  def awarenessStatsSensorStop() = {
    stopRequest = true
  }

  private def doGetAwarenessStats(): Unit = {
    awarenessManager.flatMap(_.getStats()).onComplete {
      case Success(value) =>
        if (stopRequest) {
          stopRequest = false
          return
        }
        self ! NewMeasurements(value)
        context.system.scheduler.scheduleOnce(Constants.awarenessStatsStreamerSenseInterval)(doGetAwarenessStats _)
      case Failure(exception) =>
        log.error("Failed to get awareness stats: " + exception)
    }
  }
}
