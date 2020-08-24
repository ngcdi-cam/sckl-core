package org.ngcdi.sckl.sdn

import org.ngcdi.sckl.model._

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory

object OFJsonProtocol extends DefaultJsonProtocol {

  val log = LoggerFactory.getLogger("org.ngcdi.sckl.sdn.OFJsonProtocol")

  def createMatchRule() = {}

  implicit object matchRuleJsonFormat extends RootJsonFormat[MatchRule] {
    override def read(json: JsValue): MatchRule =
      json.asJsObject.getFields(
        "in_port",
        "dl_src",
        "dl_dst",
        "dl_type",
        "nw_src"
      ) match {
        case Seq(
              JsNumber(in_port),
              JsString(dl_src),
              JsString(dl_dst),
              JsNumber(dl_type)
            ) =>
          log.debug(
            "Rule1-->" + in_port + ", " + dl_src + ", " + dl_dst + ", " + dl_type + "<-"
          )
          MatchRule(
            Option(in_port.toInt),
            Option(dl_src),
            Option(dl_dst),
            Option(dl_type.toInt),
            Option("nw_src")
          )
        case Seq(JsString(dl_src), JsString(dl_dst)) =>
          log.debug("Rule2-->" + dl_src + ", " + dl_dst + "<-")
          MatchRule(None, Option(dl_src), Option(dl_dst), None, None)
        case Seq(JsString(dl_dst), JsNumber(dl_type)) =>
          log.debug("Rule3-->" + dl_dst + ", " + dl_type + "<--")
          MatchRule(None, None, Option(dl_dst), Option(dl_type.toInt), None)
        case Seq(JsNumber(in_port), JsString(dl_src), JsString(dl_dst)) =>
          log.debug("Rule4-->" + in_port + ", " + dl_src + ", " + dl_dst + "<-")
          MatchRule(
            Option(in_port.toInt),
            Option(dl_src),
            Option(dl_dst),
            None,
            None
          )
        case Seq(JsNumber(in_port), JsString(dl_dst), JsNumber(dl_type)) =>
          log.debug(
            "Rule5-->" + in_port + ", " + Option(
              dl_dst
            ).get + ", " + dl_type + "<--"
          )
          MatchRule(
            Option(in_port.toInt),
            None,
            Option(dl_dst),
            Option(dl_type.toInt),
            None
          )
        case Seq(JsNumber(in_port), JsNumber(dl_type), JsString(nw_src)) =>
          log.debug(
            "Rule6-->" + in_port + ", " + dl_type + "," + nw_src + "<--"
          )
          MatchRule(
            Option(in_port.toInt),
            None,
            None,
            Option(dl_type.toInt),
            Option(nw_src)
          )
        case x: Any =>
          log.debug("Rule7-->" + x + "<--")
          null
        case _ =>
          throw new DeserializationException("MatchRule expected")
      }
    override def write(obj: MatchRule): JsValue =
      obj.in_port match {
        case Some(a) =>
          obj.dl_src match {
            case Some(b) =>
              obj.dl_type match {
                case Some(c) =>
                  obj.dl_dst match {
                    case Some(d) =>
                      JsObject(
                        "in_port" -> JsNumber(a),
                        "dl_src" -> JsString(b),
                        "dl_dst" -> JsString(d),
                        "dl_type" -> JsNumber(c)
                      )
                    case None =>
                      JsObject(
                        "in_port" -> JsNumber(a),
                        "dl_src" -> JsString(b),
                        "dl_type" -> JsNumber(c)
                      )
                  }
                case None =>
                  obj.dl_dst match {
                    case Some(d) =>
                      JsObject(
                        "in_port" -> JsNumber(a),
                        "dl_src" -> JsString(b),
                        "dl_dst" -> JsString(d)
                      )
                    case None =>
                      JsObject(
                        "in_port" -> JsNumber(a),
                        "dl_src" -> JsString(b)
                      )
                  }
              }
            case None =>
              obj.dl_type match {
                case Some(c) =>
                  JsObject(
                    "in_port" -> JsNumber(a),
                    "dl_type" -> JsNumber(c)
                  )
                case None =>
                  obj.nw_src match {
                    case Some(f) =>
                      JsObject(
                        "nw_src" -> JsNumber(f)
                      )
                    case None =>
                      JsObject(
                        "in_port" -> JsNumber(a)
                      )
                  }
              }
          }
        case None =>
          obj.dl_src match {
            case Some(b) =>
              obj.dl_type match {
                case Some(c) =>
                  obj.dl_dst match {
                    case Some(d) =>
                      JsObject(
                        "dl_src" -> JsString(b),
                        "dl_dst" -> JsString(d),
                        "dl_type" -> JsNumber(c)
                      )
                    case None =>
                      JsObject(
                        "dl_src" -> JsString(b),
                        "dl_type" -> JsNumber(c)
                      )
                  }
                case None =>
                  obj.dl_dst match {
                    case Some(d) =>
                      JsObject(
                        "dl_src" -> JsString(b),
                        "dl_dst" -> JsString(d)
                      )
                    case None =>
                      JsObject(
                        "dl_src" -> JsString(b)
                      )
                  }
              }
            case None =>
              obj.dl_type match {
                case Some(c) =>
                  obj.dl_dst match {
                    case Some(d) =>
                      JsObject(
                        "dl_type" -> JsNumber(c),
                        "dl_dst" -> JsString(d)
                      )
                    case None =>
                      obj.nw_src match {
                        case Some(f) =>
                          JsObject(
                            "dl_type" -> JsNumber(c),
                            "nw_src" -> JsNumber(c)
                          )
                        case None =>
                          JsObject(
                            "dl_type" -> JsNumber(c)
                          )
                      }

                  }

                case None =>
                  obj.nw_src match {
                    case Some(f) =>
                      JsObject(
                        "nw_src" -> JsNumber(f)
                      )
                  }
              }
          }
      }
  }

  implicit object FlowStatJsonFormat extends RootJsonFormat[FlowStat] {
    def write(f: FlowStat) =
      JsObject(
        "byte_count" -> JsNumber(f.byte_count),
        "packet_count" -> JsNumber(f.packet_count),
        "duration_sec" -> JsNumber(f.duration_sec),
        "actions" -> f.actions.toJson,
        "match" -> f.matchr.toJson
      )

    def read(value: JsValue) = {
      value.asJsObject.getFields(
        "byte_count",
        "packet_count",
        "duration_sec",
        "actions",
        "match"
      ) match {
        case Seq(
              JsNumber(byte_count),
              JsNumber(packet_count),
              JsNumber(duration_sec),
              JsArray(actions),
              matchr
            ) =>
          new FlowStat(
            byte_count.toLong,
            duration_sec.toLong,
            packet_count.toLong,
            actions.map(_.convertTo[String]).toSeq,
            matchr.convertTo[MatchRule]
          )
        case _ => throw new DeserializationException("FlowStat expected")
      }
    }
  }

  implicit object PortStatJsonFormat extends RootJsonFormat[PortStat] {
    def write(f: PortStat) =
      JsObject(
        "tx_bytes" -> JsNumber(f.tx_bytes),
        "rx_bytes" -> JsNumber(f.rx_bytes),
        "duration_sec" -> JsNumber(f.duration_sec),
        "port_no" -> JsString(f.port_no)
      )

    def read(value: JsValue) = {
      value.asJsObject.getFields(
        "rx_bytes",
        "tx_bytes",
        "duration_sec",
        "port_no"
      ) match {
        case Seq(
              //JsNumber(collisions),
              //JsNumber(duration_nsec),
              JsNumber(rx_bytes),
              JsNumber(tx_bytes),
              JsNumber(duration_sec),
              JsString(port_no)
              //JsNumber(rx_crc_err),
              //JsNumber(rx_dropped),
              //JsNumber(rx_errors),
              //JsNumber(rx_frame_err),
              //JsNumber(rx_over_err),
              //JsNumber(rx_packets),
              //JsNumber(tx_dropped),
              //JsNumber(tx_errors),
              //JsNumber(tx_packets)
            ) =>
          val o = new PortStat(
            rx_bytes.toLong,
            tx_bytes.toLong,
            duration_sec.toLong,
            port_no.toString
          )
          o
        case Seq(
              //JsNumber(collisions),
              //JsNumber(duration_nsec),
              JsNumber(rx_bytes),
              JsNumber(tx_bytes),
              JsNumber(duration_sec),
              JsNumber(port_no)
              //JsNumber(rx_crc_err),
              //JsNumber(rx_dropped),
              //JsNumber(rx_errors),
              //JsNumber(rx_frame_err),
              //JsNumber(rx_over_err),
              //JsNumber(rx_packets),
              //JsNumber(tx_dropped),
              //JsNumber(tx_errors),
              //JsNumber(tx_packets)
            ) =>
          val o = new PortStat(
            rx_bytes.toLong,
            tx_bytes.toLong,
            duration_sec.toLong,
            port_no.toInt + ""
          )
          o
        case x: Any =>
          log.info("received unknown JSON {}", x)
          throw new DeserializationException("(A) PortStat expected")
        //case _ =>
        //  throw new DeserializationException("(B) PortStat expected")
      }
    }
  }

}

// object AwarenessGraphStatsJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
//   implicit val awarenessGraphStatsFormat = jsonFormat7(AwarenessGraphStats)
// }
