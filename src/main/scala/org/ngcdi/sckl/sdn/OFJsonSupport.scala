package org.ngcdi.sckl.sdn

import org.ngcdi.sckl.model._

//#json-support
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

trait OFJsonSupport extends SprayJsonSupport {
  import org.ngcdi.sckl.sdn.OFJsonProtocol._

  implicit val flowStatsSetFormat = new RootJsonFormat[Set[FlowStat]] {
    def write(items: Set[FlowStat]) = JsArray(items.map(_.toJson).toVector)
    def read(value: JsValue) =
      value match {
        case JsArray(elements) =>
          elements.map(_.convertTo[FlowStat]).toSet[FlowStat]
        case x =>
          deserializationError("Expected Array as JsArray, but got " + x)
      }
  }

  //implicit val portJsonFormat = jsonFormat8(PortStat)

  implicit val portStatsSetFormat = new RootJsonFormat[Set[PortStat]] {
    def write(items: Set[PortStat]) = JsArray(items.map(_.toJson).toVector)
    def read(value: JsValue) =
      value match {
        case JsArray(elements) =>
          try {
            val u = elements.map(_.convertTo[PortStat]).toSet[PortStat]
            u
          } catch {
            case e: Exception =>
              e.printStackTrace
              null
          }
        case x =>
          deserializationError("Expected Array as JsArray, but got " + x)
      }
  }

  implicit val flowStatsMapFormat =
    DefaultJsonProtocol.mapFormat[String, Set[FlowStat]]

  implicit val portStatsMapFormat =
    DefaultJsonProtocol.mapFormat[String, Set[org.ngcdi.sckl.model.PortStat]]

  
}
