package org.slack

import org.ngcdi.sckl.model._

//#json-support
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
//import org.slf4j.LoggerFactory
import org.ngcdi.sckl._

trait JsonSlackSupport extends SprayJsonSupport with DefaultJsonProtocol{
  this:ScklActor =>

  // ****Watch out the order of the format mappings as implicit used types should have been defined before***


  implicit val reponseChallengeJsonFormat = jsonFormat1(ResponseChallenge)

  implicit object challengeJsonFormat extends RootJsonFormat[Challenge] {
  def write(c: Challenge) =
    JsObject(
      "token"->JsString(c.token),
      "challenge"->JsString(c.challenge),
      "type"->JsString(c.typeu)
    )

  def read(value: JsValue) = {
    value.asJsObject.getFields("token", "challenge", "type") match {
      case Seq(
        JsString(token),
        JsString(challenge),
        JsString(typeu)
      ) =>
        val c = new Challenge(token,challenge,typeu)
        c
      case x:Any =>
        log.info("received unknown JSON {}",x)
        throw new DeserializationException("(A) Challenge expected")

    }
  }
  }

   implicit object eventFormat extends JsonFormat[Event] {
   def write(e: Event) =
   JsObject(
   "text"->e.toJson
   )

   def read(value: JsValue) = {
   value.asJsObject.getFields(
    "client_msg_id",
    "type",
   "text",
    "user",
   "ts",
   "team",
   "blocks",
    "channel",
    "event_ts",
   "channel_type"
   ) match {
   case Seq(
   JsString(cmid),
   JsString(typee),
   JsString(text),
   JsString(user),
   JsNumber(ts),
   JsString(team),
   JsArray(elements),
   JsString(channel),
   JsNumber(eventTs),
   JsString(channelType)
   ) =>
       log.debug("Deserializing slack...")
   val e = Event(text)
   e
   case x:Any =>
   log.info("Ev received unknown JSON {}",x)
   throw new DeserializationException("(A) Event expected")

   }
   }
   }


  implicit object payloadFormat extends RootJsonFormat[EventPayload] {
    def write(p: EventPayload) =
      JsObject(
        "event"->p.toJson
      )

    def read(value: JsValue) = {
      log.debug("JSON received from slack==>"+value)
      value.asJsObject.getFields("event") match {
        case Seq(
          //JsString(token),
          //JsString(teamId),
          //JsString(apiAppId),
          //JsString(typee),
          //JsString(eventId),
          //JsNumber(eventTime),
           JsObject(ev)
 //         JsString(text)
          //JsArray(elements)
        ) =>
         // log.debug("Deserializing eventpayload slack..."+ev)
          val ep = new EventPayload(
            //token,
            //teamId,
            //apiAppId,
            //typee,
            //eventId,
            //eventTime.toLong,
            //event.convertTo[Event]
            //Event(event.get("text").get.convertTo[String])
            //Event(text)

            ev.get("subtype") match{
              case Some(st)=>
                new Event("")
              case None => // Ignore when JSON has subtype which is bot-msg
                new Event(ev.get("text").get.convertTo[String])
            }
//                ,elements.map(_.convertTo[String]).toSeq
          )
          //log.debug("END Deserializing eventpayload slack...")
          ep
        case x:Any =>
          log.info("EP received unknown JSON {}",x)
          throw new DeserializationException("(A) EventPayload expected")

      }
    }
  }





}
