package org.ngcdi.sckl

import org.ngcdi.sckl.Config._
import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.ClusteringConfig._
import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.model._

import java.nio.file.{Files, FileSystems}

import akka.stream.ActorMaterializer
import akka.actor._
import akka.NotUsed
import akka.http.scaladsl.model.ResponseEntity
import akka.stream.scaladsl.Source
import akka.stream.alpakka.file.scaladsl.FileTailSource

import scala.concurrent.duration._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.actor.{ActorRef}
import org.ngcdi.sckl.sdn._
import org.ngcdi.sckl.NetworkUtils._

//final case class ProcessResponse(entityStr:String,entity:ResponseEntity)
//final case class ProcessResponse(entity:String)

/*
 * Controller that enables sensing and processing from different sources
 */
trait CombinedController
    extends PortParser
    with FileController
    with RESTController {
  this: DigitalAssetBase =>

  override def query = Config.restMonitoringUrl + nodeName

  override def startSensors(): Unit = {
    log.info("Starting Combined!!!")
    super[RESTController].startSensors()
    super[FileController].startSensors()
  }

  override def updateMeasurements(newMeasurements: Seq[Model]) = {
    val now = getReadingTime()
    log.debug("Combined newMeasurements==>" + newMeasurements)
    newMeasurements
      .map(ps => ps.asInstanceOf[PortStat])
      .foreach { nf =>
        lastReadings
          .map(t => t.asInstanceOf[PortStat])
          .filter(_.port_no == nf.port_no)
          .map { of =>
            getThroughputKBytes(
              of.tx_bytes,
              nf.tx_bytes,
              of.duration_sec,
              nf.duration_sec
            ) match {
              case Some(v) =>
                sendToLocalView(
                  nodeName,
                  nf.port_no.toString(),
                  v,
                  now,
                  ThroughputOut
                )
              case None =>
                log.info("No Throughput Calculated")
            }
          }
      }
    lastReadings = newMeasurements
  }

  override def ctlBehaviour = {
    super.ctlBehaviour
      .orElse[Any, Unit](parsingBehaviour)
      .orElse[Any, Unit] {
        case Link(linkFileName: String) =>
          linkViaCSV(linkFileName)
      }
  }
}
