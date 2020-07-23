package org.ngcdi.sckl.sdn

import org.ngcdi.sckl.model._
import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.DigitalAsset

import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.actor.{ActorRef,ActorPath}

import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.Config._
import org.ngcdi.sckl.ClusteringConfig._
import org.ngcdi.sckl.NetworkUtils._
import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.RESTParser

import java.time.{Instant, LocalDateTime, ZoneOffset, ZoneId}
import java.util.Locale
import java.time.format.DateTimeFormatter
import org.ngcdi.sckl.RESTController
import spray.json._
import akka.util.ByteString

trait FlowProcessor extends RESTParser with OFJsonSupport {


  this:DigitalAsset with RESTController =>


  def filterRelevantFlows(keyHosts:Seq[String],flows:Seq[FlowStat]):Seq[FlowStat]={

    log.debug("new flows unfiltered: -->"+flows)

    try{
      val newFlows:Seq[FlowStat] =
        keyHosts.foldLeft[Seq[FlowStat]](Seq.empty){
          (ac, h) =>
          ac ++ flows
          .filter(
              !_.actions.contains("OUTPUT:CONTROLLER")
            )
            .filter(
              _.matchr != null
            )
            .filter(
              x =>
              x.matchr.dl_type match {
                case Some(t) =>
                  if(t == 2048)  // Type of flow
                    true
                  else
                    false
                case None =>
                  false
              }
            )
            .filter(
              x =>
              x.matchr.dl_dst match {
                case Some(d) =>
                  if (d.equals(h))
                    true
                  else
                    x.matchr.dl_src match {
                      case Some(s) =>
                        if(s.equals(h))
                          true
                        else
                          false
                      case None =>
                        false
                    }
                case None =>
                  x.matchr.dl_src match {
                    case Some(s) =>
                      if(s.equals(h))
                        true
                      else
                        false
                    case None =>
                      false
                  }
              }
            )
            .collect{
              case f:FlowStat => f
            }
        }.distinct

      log.debug("NEW FLOWS FILTERED: -->"+newFlows)
      return newFlows
    }catch{
      case e:Exception =>
        log.error("ERROR filtering",e)
        return null
    }
  }


  def parseStatistics(body:ByteString):Seq[Model]={
    //Sensing date from here to be consistent among data read
    val now = getReadingTime()
    var nms:Seq[Model] = Seq.empty

    Unmarshal(body).to[Map[String,Set[FlowStat]]].map {
      fs =>

      fs.get(nodeName).map {
        f =>

        val ff = filterRelevantFlows(keyHosts,f.toSeq)

        ff.foreach {
          nf =>

          lastReadings
            .map(
              t=>t.asInstanceOf[FlowStat]
            )
            .filter(_.matchr==nf.matchr)
            .map{
              of =>
              getThroughputKBytes(of.byte_count,nf.byte_count,of.duration_sec,nf.duration_sec) match{
                case Some(v) =>
                  log.info("TODO sendToLocalView as CombinedController")
                  //context.actorSelection(lViewPath) ! createTptRecord(nodeName,nf.matchr.toString(), v, now)
                case None =>
                  log.info("No Throughput Calculated")
              }
            }
        }

        lastReadings = ff.toSeq
      }
    }
    nms
  }


  def processReroute(entity:ResponseEntity)={
    Unmarshal(entity).to[String].map {
      ack =>
      log.debug("=====UNMARSHALED:===>")
      log.debug("Reroute Acknoledgement!!!!!!!:======>"+ack+"<=====")
      log.debug("<=====UNMARSHALED:===")
    }
  }

 override def parseRoutes(body:ByteString):Seq[Model]={
    log.error("ERROR parseRoutes NOT IMPLEMENTED")
    Seq.empty
 }

  override def parseAlternativeRoutes(data:String,body:ByteString):Tuple2[String,Option[Seq[Seq[String]]]]={
    log.error("ERROR parseRoutes NOT IMPLEMENTED")
    ("",None)
  }

  override def parseRerouteResult(data:String,body:ByteString):Tuple2[String,Boolean]={
    log.error("ERROR parseBoolean NOT IMPLEMENTED")
    ("",false)
  }
}
