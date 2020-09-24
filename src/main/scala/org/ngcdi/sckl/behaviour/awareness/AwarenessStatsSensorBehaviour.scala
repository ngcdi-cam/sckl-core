package org.ngcdi.sckl.behaviour.awareness

import org.ngcdi.sckl.msgs.NewMeasurements
import scala.util.Success
import scala.util.Failure
import org.ngcdi.sckl.Constants
import org.ngcdi.sckl.ClusteringConfig
import scala.concurrent.Future
import org.ngcdi.sckl.awareness.AwarenessRawStatEntry

// Trait for getting full network awareness stats periodically
// See also: AwarenessSwitchStatsSensorBehaviour

trait AwarenessStatsSensorBehaviour
    extends SensorBaseBehaviour
    with AwarenessManagerReceiverBehaviour {

  // @depends: AwarenessManagerReceiver finishes initialization
  override def sensorPrestart(): Unit = {
    if (ClusteringConfig.nodeIp == "c1") {
      log.info("AwarenessStatsSensorBehaviour is enabled")
      getStatsPeriodically()
    } else {
      log.info("AwarenessStatsSensorBehaviour is disabled")
    }

  }

  private def getStats(): Future[Unit] = {
    for {
      manager <- awarenessManager
      stats <- manager.getStats()
      _ <- Future {
        self ! NewMeasurements(stats)
      }
    } yield Unit
  }

  private def getStatsPeriodically(): Unit = {
    getStats().onComplete { x =>
      context.system.scheduler.scheduleOnce(interval)(getStatsPeriodically)

      x match {
        case Failure(exception) =>
          log.error("Failed to get awareness stats: " + exception)
        case _ =>
      }
    }
  }
}
