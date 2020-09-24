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

// Trait for getting for network awareness switch flows periodically
// See also: AwarenessSwitchStatsSensorBehaviour

trait AwarenessSwitchFlowsSensorBehaviour
    extends SensorBaseBehaviour
    with AwarenessManagerReceiverBehaviour
    with AwarenessSwitchProvider {
  
  override def sensorPrestart(): Unit = {
    log.info("Starting awareness flows sensor")
    // stopRequest = false
    getReadingsPeriodically()
  }

  private def getReadingsPeriodically(): Unit = {
    // if (stopRequest) return
    // log.info("getReadingsPeriodically()")

    getSwitchFlows().onComplete { x =>
      context.system.scheduler.scheduleOnce(interval)(getReadingsPeriodically)

      x match {
        case Failure(exception) => 
          log.error("getReadingsPeriodically() error: " + exception)
        case _ =>
      }
    }
  }

  def getSwitchFlows(): Future[Unit] = {
    // log.info("getReadings() in AwarenessSwitchFlowsSensorBehaviour")
    for {
      manager <- awarenessManager
      switch <- awarenessSwitch
      flows <- manager.getSwitchFlows(switch)
      _ <- Future {
        self ! NewMeasurements(flows)
      }
    } yield Unit
  }
}
