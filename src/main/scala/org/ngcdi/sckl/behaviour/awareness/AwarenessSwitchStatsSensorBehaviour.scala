package org.ngcdi.sckl.behaviour.awareness

import org.ngcdi.sckl.msgs.NewMeasurements
import scala.util.Success
import scala.util.Failure
import scala.concurrent.duration.Duration
import org.ngcdi.sckl.Constants
import org.ngcdi.sckl.AwarenessMessages._
import org.ngcdi.sckl.behaviour.awareness.AwarenessSwitchProvider
import scala.concurrent.Future
import org.ngcdi.sckl.behaviour.awareness.SensorBaseBehaviour
import org.ngcdi.sckl.ScklActor

// Trait for getting for network awareness switch stats periodically
// See also: AwarenessSwitchSensorBehaviour

trait AwarenessSwitchStatsSensorBehaviour
    extends SensorBaseBehaviour
    with AwarenessManagerReceiverBehaviour
    with AwarenessSwitchProvider {

  this: ScklActor =>

  override def sensorPrestart(): Unit = {
    log.info("Starting awareness stats sensor")
    // stopRequest = false
    getReadingsPeriodically()
  }

  private def getReadingsPeriodically(): Unit = {
    // if (stopRequest) return
    // log.info("getReadingsPeriodically()")

    getSwitchStats().onComplete { x =>
      context.system.scheduler.scheduleOnce(interval)(getReadingsPeriodically)

      x match {
        case Failure(exception) => 
          log.error("getReadingsPeriodically() error: " + exception)
        case _ =>
      }
    }
  }
  
  def getSwitchStats(): Future[Unit] = {
    // log.info("getReadings() in AwarenessSwitchStatsSensorBehaviour")
    for {
      manager <- awarenessManager
      switch <- awarenessSwitch
      stats <- manager.getSwitchStats(switch)
      _ <- Future {
        // log.info("getReadings(): ok: " + stats)
        self ! NewMeasurements(stats)
      }
    } yield Unit
  }
}
