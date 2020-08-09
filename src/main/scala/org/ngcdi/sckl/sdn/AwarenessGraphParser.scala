package org.ngcdi.sckl.sdn

import org.ngcdi.sckl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ResponseEntity
import org.ngcdi.sckl.RESTParser
import akka.util.ByteString
import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.model.AwarenessGraphStats

import akka.util.ByteString

object AwarenessGraphStatsJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val awarenessGraphStatsFormat = jsonFormat7(AwarenessGraphStats)
}


trait AwarenessGraphParser extends RESTParser with OFJsonSupport {
  this: ScklActor =>

  def parseStatistics(body: ByteString): Seq[Model] = {
    //Sensing date from here to be consistent among data read
    val now = getReadingTime()
    var nms: Seq[Model] = Seq.empty
    try {
      log.debug("Starting marshalling++..")

      Unmarshal(body).to[Map[String, Set[AwarenessGraphStats]]].map { fs =>
        nms = fs.get("graph").get.toSeq
      }
      log.debug("Ending marshalling..++=>" + nms)
      nms
    } catch {
      case e: Exception =>
        log.error(e, "Error Marshalling!!!")
        nms
    }
  }
}