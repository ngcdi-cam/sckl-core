package org.ngcdi.sckl.adm

import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.StatisticsUtils._
import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.Config._
import org.ngcdi.sckl.ClusteringConfig._
import org.ngcdi.sckl._
import org.ngcdi.sckl.model._
import akka.actor.ActorRef
import scala.concurrent.duration._

import java.time.{ZoneOffset,LocalDateTime, Instant}


trait DecisionMaking {
  this:ScklActor =>
  /*
   * Selects the optimal alternative route considering performance and condition
   */
  def selectAlternativeRoute(
    altRoutes:Tuple2[String,Option[Seq[Seq[String]]]],
    assetsToAvoid:Seq[String],
    measurements:Seq[Measurement],
    criteria:String
  ):Tuple2[String,Seq[String]]={

    log.debug("Available routes==>"+altRoutes)
    log.info("Available routes size==>"+altRoutes._2.get.size)
    log.info("Assets to avoid in rerouting==>"+assetsToAvoid)
    log.info ("Total view size:"+measurements.size)
    log.debug ("Total view:"+measurements)
    val view = criteria match {
      case `conditionCriteria` =>
        measurements
          .filter{
            m => conditionKPIs contains(m.metricName)
          }
      case _ =>
        measurements
    }

    log.info ("Condition-only size:"+view.size)
    log.debug ("Condition-only view:"+view)

    val validRoutes = altRoutes._2 match {
      case Some(routes)=>
        routes
          .filter{
            route =>
            (route intersect assetsToAvoid).size == 0
          }
      case None =>
        log.info("No alternative routes" )
        Seq.empty
    }

    log.info ("Only valid routes:"+validRoutes)
    log.info("Only valid routes size==>"+validRoutes.size)

    val rankedRoutes =
    validRoutes
      .map{
        vr =>
        (vr,calculateRouteHealth(vr,view))
      }
      .sortBy(_._2) //Ascending order so must take last element

    log.info("Ranked routes==>"+rankedRoutes)
    log.info("Chosen route===>"+rankedRoutes.last._1)

    (altRoutes._1,
      rankedRoutes.last._1)


//      (altRoutes._1,
 //       altRoutes._2.get.head //dummy implementation
 //     )
  }


  def calculateRouteHealth(route:Seq[String],view:Seq[Measurement]):Double
  def calculateRULAsset(conditionView:Seq[Measurement]):Double

}
