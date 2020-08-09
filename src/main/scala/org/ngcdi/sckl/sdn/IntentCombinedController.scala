package org.ngcdi.sckl

import org.ngcdi.sckl.Config._
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
import org.ngcdi.sckl.Constants._

//final case class ProcessResponse(entityStr:String,entity:ResponseEntity)
//final case class ProcessResponse(entity:String)

/*
 * Controller that enables sensing and processing from different sources with Intent Monitoring
 */
trait IntentCombinedController
    extends IntentParser
    with RESTController
    with FileController {
  this: DigitalAssetBase =>

  override def query = Config.restMonitoringUrl

  override def startSensors(): Unit = {
    super[RESTController].startSensors()
    log.info("Intent restClientSen Reference Found==>" + restClientSen)
    super[FileController].startSensors()
  }

  override def updateMeasurements(newMeasurements: Seq[Model]) = {
    val now = getReadingTime()

    log.debug("newMeasurements==>" + newMeasurements)

    newMeasurements
      .map(n => n.asInstanceOf[IntentMetricModel])
      .foreach { nf =>
        lastReadings
          .map(t => t.asInstanceOf[IntentMetricModel])
          .filter(_.id == nf.id)
          .map { of =>
            getThroughputKBytes(of.bytes, nf.bytes, of.life, nf.life) match {
              case Some(v) =>
                sendToLocalView(nodeName, nf.intentId, v, now, ThroughputOut)
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

  override def sense(assetName: String, freqSensing: Int): Unit = {
    log.debug("Intent NODE_name:====>" + nodeName)
    restClientSen ! GetRequest(query, true)
    context.system.scheduler.scheduleOnce(freqSensing seconds, self, ReSense)

  }

}
