package org.ngcdi.sckl.sdn

import org.ngcdi.sckl.model._
import org.ngcdi.sckl.Config._
import org.ngcdi.sckl.DigitalAssetBase
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ResponseEntity
import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.ClusteringConfig._
import org.ngcdi.sckl.NetworkUtils._
import org.ngcdi.sckl.RESTParser
import akka.stream.scaladsl.Source
import akka.util.ByteString
import scala.concurrent.{Future}
import org.ngcdi.sckl.DAController
import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.msgs._
import akka.util.ByteString

trait PortParser extends RESTParser with OFJsonSupport {
  this:ScklActor =>

  def parseStatistics(body:ByteString):Seq[Model]={
    //Sensing date from here to be consistent among data read
    val now = getReadingTime()
    var nms:Seq[Model] = Seq.empty
    try{
      log.debug("Starting marshalling++..")

      Unmarshal(body).to[Map[String,Set[PortStat]]].map {
        fs =>
        nms = fs.get(nodeName).get.toSeq
      }
      log.debug("Ending marshalling..++=>"+nms )
      nms
    }catch{
      case e:Exception =>
        log.error(e,"Error Marshalling!!!")
        nms
    }
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
