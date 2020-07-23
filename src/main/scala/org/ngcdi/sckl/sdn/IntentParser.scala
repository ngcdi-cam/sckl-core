package org.ngcdi.sckl.sdn

import org.ngcdi.sckl.model._
import org.ngcdi.sckl.Config._
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
import scala.concurrent.{Await, Future, Promise }
import scala.concurrent.duration._


import spray.json.DefaultJsonProtocol._

trait IntentParser extends RESTParser with JsonSupport {
  this:ScklActor =>
  import spray.json._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  override def parseStatistics(body:ByteString):Seq[Model]={
    //Sensing date from here to be consistent among data read
    val now = getReadingTime()
    var nms:Seq[Model] = Seq.empty

    //entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach {
    //  body =>
  //    log.info("Got response, BODY: " + body.utf8String)

    //}
//    nms

    try{
      log.debug("Starting marshalling++.."+body)
      //entity.dataBytes
        //.via(JsonSupport.framingDecoder)
      // .mapAsync(1)(bytes => Unmarshal(bytes).to[ResponseStatistics])
      Unmarshal(body)
        .to[ResponseStatistics]
        .map {
          ack =>
          val newM = ack.statistics.head.intents
            .map(im => im.head ) //Only comes one key/value pair
            .map{
              im =>
              val v = im._2.map(i =>  i.value) // From IntentMetricValue to IntentMetric
              (im._1.value,v)
            }
            .map{     //filter if deviceid of interest
              im=>
              val v =
                im._2
                  .filter{
                    case i =>
                      i.deviceId == nodeName
                  }
              (im._1,v)
            }
            .filter( _._2.size > 0 ) //filter those intents that return an empty set => do not have deviceid
            .collect{
              case t =>
                new IntentMetricModel(t._2.head.id, t._2.head.bytes,t._2.head.life,t._2.head.deviceId,t._1).asInstanceOf[Model]
            }.toSeq
            nms = newM
        }
        log.debug("Ending marshalling..++" )
   //     log.debug("newMeasurement++:"+nms)
        nms
    }catch{
      case e:Exception =>
        log.error(e,"Error Marshalling!!!")
        Seq.empty
    }
  }

  override def parseRoutes(body:ByteString):Seq[Model] ={
   // log.info("Got response, PIR => " + body.utf8String)
    var routes:Seq[Model] = Seq.empty

    Unmarshal(body)
      .to[IntentRoutes]
      .map{
        ir=>
        routes = ir.routingList
      }
    routes
  }

  override def parseAlternativeRoutes(data:String,body:ByteString):Tuple2[String,Option[Seq[Seq[String]]]] ={
    log.debug("Parsing Alternative routes...")
     log.debug("Got response, P ALT_ROUTES SIZE => " + body.utf8String.size)
    //val ar = body.utf8String.parseJson.convertTo[Map[String,Any]]
    //log.info("Alt route===>"+ar)
    //altRoutes:Tuple2[String,Seq[Seq[String]]] =

    val routesReq = data.parseJson.convertTo[GetRoutesRequest]

    val result =
      if(body.utf8String.size > 6){ // (false = size 5))
        val futureRoutes:Future[AlternativeRoute] =
          Unmarshal(body).to[AlternativeRoute]

        val alternatives = Await.result(futureRoutes,1.second)

        (alternatives.key,
          Option(alternatives.routes
            .map{
              case (k,v)=> v.value //return the list of tuples (Seq[String]))
            }.toSeq)
        )
      }else
    (routesReq.key,
      None
    )
    result
  }

  def parse(routes:Seq[Tuple2[String,Seq[String]]]):String={

    val json =
      PushIntentRequest(
        netwApiKey,
        routes.map{
          route =>
          RouteEntry(RouteKey(route._1),RouteValue(route._2))
        }
      ).toJson.toString

    log.debug("PARSED REROUTE REQUEST==>"+json)

    json
  }

  def parse(intentKey:String):String={
    val json = GetRoutesRequest(api_key=netwApiKey,key=intentKey).toJson.toString
    log.debug("PARSED ROUTES REQUEST==>"+json)
    json
  }

  override def parseRerouteResult(data:String,body:ByteString):Tuple2[String,Boolean]={
     log.debug("Got response, P reoute result => " + body.utf8String)
    val rerouteReq = data.parseJson.convertTo[PushIntentRequest]

    val futureResult = Unmarshal(body).to[SimpleResult]

    val result = Await.result(futureResult,1.second)

    log.debug("Result reroute parsed==>"+result+" -->"+rerouteReq.routes.size)
    (rerouteReq.routes.size.toString(),result.success)
  }

 /* def getJsonIntent(intentKey:String):String={
    RequestIntentRoutes(intentKey).toJson.toString
    //log.info ("JSON to get av routes==>"+jsonIntent
  }

  def processReroute(entity:ResponseEntity)={
    Unmarshal(entity).to[String].map {
      ack =>
      log.debug("=====UNMARSHALED:===>")
      log.debug("Reroute Acknoledgement!!!!!!!:======>"+ack+"<=====")
      log.debug("<=====UNMARSHALED:===")
    }
  }*/

}
