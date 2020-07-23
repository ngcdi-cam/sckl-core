package org.ngcdi.sckl

import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.StatisticsUtils._
import org.ngcdi.sckl.sdn.RESTClient
import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.ClusteringConfig._
import org.ngcdi.sckl.NetworkUtils._


import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.util.{ Failure, Success }
import scala.concurrent.Future
import scala.concurrent.duration._

import java.time.{ZoneOffset,LocalDateTime, ZoneId, Instant}
import java.time.format.DateTimeFormatter
import java.util.Locale



trait RInterfacing{

  this:ScklActor =>

 // implicit val timeout = Timeout(5 seconds)
//  import scala.concurrent.ExecutionContext.Implicits.global

  val rActor = context.actorOf(RUtilsActor.props(),"rActor")


  def callUpdateAnomIntent(tick:Int,labelAD:String,deviceId:String,intent:String,newData:Seq[Double]):Unit = {

    if(tick==1){

      //InitStateAnomD(labelAD)

      val future: Future[Boolean] =
        ask(rActor, InitStateAnomD(labelAD)).mapTo[Boolean]

      future.onComplete {
        case Success(t) =>
          rActor ! UpdateAnomD(tick,labelAD,deviceId,intent,newData)
        case Failure(t) => println("An error has occurred: " + t.getMessage)
      }

    }else{
      rActor ! UpdateAnomD(tick,labelAD,deviceId,intent,newData)
    }
  }

}
