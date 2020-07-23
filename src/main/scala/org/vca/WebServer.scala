package org.vca

import org.ngcdi.sckl.ClusteringConfig._
import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.sdn._
import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.adm._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import org.ngcdi.sckl._
import org.slack._
import akka.actor._
import scala.concurrent.duration._



trait WebServer extends JsonSlackSupport{
  this:ScklActor=>


  /*
   *  Case insestive pattern matching
   */

  implicit class CaseInsensitiveRegex(sc: StringContext) {
    def ci = ( "(?i)" + sc.parts.mkString ).r
  }

  def doStringMatch(str: String) = str match {
    case ci"String1" => 1
    case ci"String2" => 2
    case _ => 0
  }



  val route = concat(
    path("hello") {
      log.info("Request to hello received")
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    },

    path("slkregister") {
      log.info("request to slack/register received")
      post {

        var isChallenge:Boolean = false

        extractStrictEntity(3.seconds) { entity =>
          complete{
            log.info("received from slack: "+entity.data.utf8String)
            if (entity.data.utf8String.contains("challenge")) isChallenge = true
            entity.data.utf8String
          }
        }

        if(isChallenge)
          entity(as[Challenge]){
            c =>
            complete{
              HttpResponse(entity = c.challenge)
            }
          }
        else
          entity(as[EventPayload]){
            ep =>
            complete{
              log.debug("Message received from Slack===>"+ep.event.text)
              ep.event.text match{
                case s:String if (
                  s.toLowerCase().contains("ok") ||
                    s.toLowerCase() == "sure" ||
                    s.toLowerCase() == "yes"
                )=>
                  self ! TriggerAssetPreparation
                case s:String  if s.toLowerCase.contains("activate")=>
                  self ! ActivatePredictiveAnalytics
                case x:Any =>
                  log.debug("Nothing to do =>"+x)

           }

           HttpResponse()
         }


         }

              }
    }

  )

  val bindingFuture = Http().bindAndHandle(route,"0.0.0.0" , 7780)

  log.info(s"Server online at seed1: "+bindingFuture.isCompleted)
    bindingFuture
   // .flatMap(_.unbind()) // trigger unbinding from the port
    //.onComplete(_ => system.terminate()) // and shutdown when done
    .onComplete(_=>log.info("Server finished"))

}
