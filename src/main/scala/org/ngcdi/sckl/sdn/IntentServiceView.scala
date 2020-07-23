package org.ngcdi.sckl.sdn

import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.StatisticsUtils._
import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.Config._
import org.ngcdi.sckl.ClusteringConfig._
import org.ngcdi.sckl._
import org.ngcdi.sckl.adm._
import org.ngcdi.sckl.model._
import akka.actor.ActorRef
import scala.concurrent.duration._

import java.time.{ZoneOffset,LocalDateTime, Instant}


trait IntentServiceView extends ServiceView with IntentParser{

  this: ScklActor with AnomalyDetector with DecisionMaking=>

 // import context._ //imports system
  var intentRoutes:Seq[IntentPath] = Seq.empty

  override  def serviceViewPreStart(): Unit = {
    super.serviceViewPreStart()

    log.debug("NETW LIST==>"+netwUrls)

    netwUrls.head.map {
      x => log.debug("NETWURLS HEAD:"+netwUrls)
    }


    //netwUrls.stream().map( u ->log.info("NETW URL-->"+u))
    //log.debug("NETWURLS HEAD:"+netwUrls.head)

    log.debug("FINISHED ISV PRE-START!!!!")
  }



  override def svBehaviour:Receive = {
    super.svBehaviour
      .orElse[Any,Unit](parsingBehaviour)
      .orElse[Any,Unit]{
        case UpdateRoutes(intentRoutes:Seq[Model])=>
          updateRoutes(intentRoutes)
        case FindIntentsForAsset(assets:Seq[String])=>
          val assetsIntents = findIntentsForAssets(assets):Seq[Tuple2[String,String]]
          log.debug("Intents Assets:==>"+assetsIntents)
          self ! DoConfirmedMaintenanceP(assetsIntents)
          //prepareReroute()
      }
  }

 override def doAggregateLocalViews(measurements:Seq[Measurement])={
   val moi =
     measurements.foldLeft[Seq[Measurement]](Seq.empty){
       (ac,m)=>
       m.resourceId match {
         case "1234567" =>
           ac :+ m
         case s:String if soi.map(i=>i._1) contains s =>
           ac :+ m
         case x:Any =>
           ac
       }
     }


/*     measurements.foldLeft[Seq[Measurement]](Seq.empty){
      (ac,m)=>
      if(
        soi
          .map{
            s=>log.info("AGGREGATING: ==>"+s)
            (s._1,s._2,s._3)
          }
          //.filter(_._2 == m.neId)
          .filter(
            _._1 == m.resourceId ||
              1234567 == m.resourceId
          )
          .size > 0
      )
        ac :+ m
      else
        ac
    }.collect{
      case mf:Measurement => mf
    }*/

    log.info("Received from "+ measurements.head.neId + ": "+measurements.size+" Measurements")
    log.debug("Received from "+ measurements.head.neId + ": "+measurements)
    log.debug("Filtered Measurements: " + moi)

    aggregatedView = aggregatedView ++ moi

    log.debug("Aggregatedview: " +aggregatedView)

  }

  def updateRoutes(routes:Seq[Model])={
    intentRoutes = routes.collect{
      case r =>
        r.asInstanceOf[IntentPath]
    }
  //  intentRoutes
  //  .map(ir=>log.info("Intent Route==>"+ir))

  }

  /*
   * Intents of interest
   */
  override def obtainServicesOfInterest():Seq[Tuple3[String,String,String]] = {
    log.debug("key intents:"+keyServices)

    val keyHostsSrc = (1 to 6).toSeq // Source hosts
    val keyHostsDst = (7 to 9).toSeq // Destination hosts

    val keyIntents =
      keyHostsSrc.map {
        src =>
        keyHostsDst.map {
          dst =>
          ("00:00:00:00:00:0"+src+"/None00:00:00:00:00:0"+dst+"/None","","")
        }
      }.flatten

    log.debug("key_intents:"+keyIntents)
    keyIntents
  }

  /*
   * Return intents where asset is involved
   */

  def findIntentsForAssets(assets:Seq[String]):Seq[Tuple2[String,String]]={
    //TODO review to change for a foldleft
    log.debug("Assets received:"+assets)
   // var assetsIntents:Seq[Tuple2[String,String]] = Seq.empty
    val existingIntents =
      intentRoutes.map{
        ip =>
        (ip.key,ip.paths.map(p=>p.path)) // (String,Seq[String])
      }

    log.debug("Existing intents routees:"+existingIntents)

    val assetIntents = assets.foldLeft[Seq[Tuple2[String,Seq[String]]]](Seq.empty){
      (ac,a)=>
      ac :+ (a,
        existingIntents.map{
          ei=>
          ei._2.flatten match {
            case p if p contains a =>
              ei._1
            case x:Any =>
              "-"
          }
        }
      )
    }
      .map{
        asi=>
        asi._2.map{
          i=>
          (asi._1,i)
        }
      }.flatten
      .filter(_._2 != "-")

    log.debug("Intents for Assets:"+assetIntents)

    /*
    val assetsIntents =
      assets.foldLeft[Seq[Tuple2[String,String]]](Seq.empty){
        (ac,a) =>
          intentRoutes
            .map
        intentRoutes
          .map{
            ip =>
            (
              ip.key,
              ip.paths.head.path
                .filter(_ == a ) // filter only paths with this device
            )
          }
          .filter(_._2.size > 0) // filter all the intents where the device is not in any path
          .collect{
            case t:Tuple2[String,Seq[String]] =>
              (a,t._1)
          }
      }
     */

    assetIntents
  }


}
