package org.ngcdi.sckl.behaviour.awareness

import org.ngcdi.sckl.Constants
import scala.concurrent.duration.FiniteDuration
import org.ngcdi.sckl.ScklActor
import scala.concurrent.Future
import scala.util.Failure


// deprecated
trait SensorBaseBehaviour extends ScklActor {

  val interval: FiniteDuration = Constants.awarenessStatsStreamerSenseInterval
  private var stopRequest = false

  def sensorPrestart() = {
    log.info("Starting base sensor")
    stopRequest = false
    // getReadingsPeriodically()
  }

  def sensorStop() = {
    stopRequest = true
  }

  // def getReadings(): Future[Unit]
  // def getReadings(): Future[Unit] = {
  //   log.info("getReadings() not overriden by superclasses")
  //   Future.failed(new Exception("getReadings() not overriden by superclasses"))
  // }

  // private def getReadingsPeriodically(): Unit = {
  //   if (stopRequest) return
  //   log.info("getReadingsPeriodically()")

  //   getReadings().onComplete { x =>
  //     context.system.scheduler.scheduleOnce(interval)(getReadingsPeriodically)

  //     x match {
  //       case Failure(exception) => 
  //         log.error("getReadingsPeriodically() error: " + exception)
  //       case _ =>
  //     }
  //   }
  // }
}