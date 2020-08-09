package org.ngcdi.sckl

import akka.http.scaladsl.model.ResponseEntity
import org.vca._
//import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.model._
import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.Config._
import akka.util.ByteString

trait RESTParser extends MessageParser {
  this: ScklActor =>

  override def parseResponse(
      request: String,
      body: ByteString,
      data: String
  ): Unit = {
    log.debug("REST Processing response for query..." + request)
    log.debug("REST Processing response case is: " + restMonitoringUrlNodeName)
    val queryRoutes = "/api/get_intents" //netwUrls.head
    request match {
      case `restMonitoringUrl` =>
        log.debug("parsing intent stats")
        val newMeasurements = parseStatistics(body)
        self ! NewMeasurements(newMeasurements)

      case `restMonitoringUrlNodeName` =>
        log.debug("parsing stats")
        val newMeasurements = parseStatistics(body)
        self ! NewMeasurements(newMeasurements)

      case "/api/get_intents" =>
        log.debug("parsing get intents")
        val routes = parseRoutes(body)
        self ! UpdateRoutes(routes)

      case "/api/get_routes" =>
        log.debug("parsing get alternative routes")
        val altRoutes = parseAlternativeRoutes(data, body)
        self ! DoAlternativeRoutes(altRoutes)

      case "/api/push_intent" =>
        log.debug("parsing push intent response")
        val result = parseRerouteResult(data, body)
        self ! DoResultReroute(result)
      case _ =>
        log.debug("query response case is: " + restMonitoringUrlNodeName)
        log.debug("Unknown request:" + request)
    }
  }

  override def parseResponseExtra(
      request: String,
      body: ByteString,
      data: String,
      dataExtra: Seq[String]
  ): Unit = {}

  def parseStatistics(body: ByteString): Seq[Model]
  def parseRoutes(body: ByteString): Seq[Model]
  def parseAlternativeRoutes(
      data: String,
      body: ByteString
  ): Tuple2[String, Option[Seq[Seq[String]]]]
  def parseRerouteResult(
      data: String,
      body: ByteString
  ): Tuple2[String, Boolean]

}
