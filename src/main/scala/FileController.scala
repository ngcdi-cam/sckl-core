package org.ngcdi.sckl

import org.ngcdi.sckl.Config._
import org.ngcdi.sckl.ClusteringConfig._
import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.model._

import java.nio.file.{Files, FileSystems}

import akka.stream.ActorMaterializer
import akka.actor._
import akka.NotUsed
import akka.http.scaladsl.model.ResponseEntity
import akka.stream.scaladsl.Source
import akka.stream.alpakka.file.scaladsl.FileTailSource

import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer

trait FileController extends DAController {
  //this: ScklActor =>
  this: DigitalAssetBase =>

  override def startSensors(): Unit = {
    log.info("File Start Sensors..")
    //Sensing is decoupled from DigitalAsset.
    //For simulation, we trigger the sensing
    val simAsset = RootActorPath(self.path.address) / "user" / "sim-" + nodeIp
    context.system.actorSelection(simAsset) ! Start
    linkViaCSV(nodeIp + ".csv")

    //lastReadings = Seq.empty
  }

  /*
   * It activates link with asset through the file passed as argument
   * It assumes status of the asset is gathered through different sensor readings and recorded in the given file.
   */

  def linkViaCSV(fileInStr: String): Unit = {

    val fs = FileSystems.getDefault

    val linkFile = fs.getPath("logs/" + fileInStr)

    log.debug("Trying to link file")

    // try{
    if (!Files.notExists(linkFile)) {
      log.debug("File has been created already")
      //val daSink =
      //  Sink.actorRef(localView,CompletedStream) // Send readings to LocalView (Sensor Actor)

      val readings: Source[String, NotUsed] = FileTailSource.lines(
        path = linkFile,
        maxLineSize = 8192,
        pollingInterval = 250.millis
      )

      readings.runForeach { l =>
        context.actorSelection(lViewPath) ! l
        log.debug("DETECTED====>" + l)
      }
      log.info("Finished linkage via: " + fileInStr)
    } else {
      log.debug("No file created yet")
      system.scheduler.scheduleOnce(5.seconds, self, Link(fileInStr))
    }

  }

}
