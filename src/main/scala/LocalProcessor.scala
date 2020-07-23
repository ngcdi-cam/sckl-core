package org.ngcdi.sckl

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

import akka.actor._

import msgs._
import Constants._
import ClusteringConfig._

/*
 *  It performs basic processing operations for a given asset
 */

class LocalProcessor() extends ScklActor{

  override def postStop(): Unit = {
    ()
  }

  def receive = {
    case view:Seq[Measurement] =>
      sender ! Report(aggregate(view))

    case x:Any  => log.info("received unknown message: {}",x )
  }

  /*
  * It performs operations on local data collected during the time window
  */
  def aggregate(view:Seq[Measurement]):Seq[Measurement]={

    log.debug("Starting aggregation of {}",view)
    //val formatterTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm ss.SSSSSS")
    //val now = Instant.now

    val groups:Map[String,Seq[Measurement]] = view.groupBy(_.metricName)



    val  vAggregated:Set[Measurement] =

    groups.keySet.map{
      case k1:String =>

        val groupsResources = groups(k1).groupBy(_.resourceId)

        log.debug("--INTENT GROUPS----KEYS-->"+groupsResources.keySet+"<-----")
        //log.info("GROUPS----VALUES-->"+groupsResources.values+"<-----")

        groupsResources.keySet.map{

          case k2:String =>
            reportingValue match{
              case reportFromDAAverage =>
                aggregateByMean(groupsResources(k2),view.head.seconds,view.head.nanos,view.head.measurementDate)
              case reportFromDALast =>
                groupsResources(k2).last //report last metrics
            }
        }
        //.collect {
        //  case m:Measurement => m
        //}.toSeq
          .toList




    }.collect {
      case m:List[Measurement] =>
        m
    }.toList.flatten.toSet

    vAggregated.toSeq
  }

  /*
   * Calculates mean of measurements over the time window
   */
  def aggregateByMean(measurements:Seq[Measurement], seconds:Long,nano:Int,mdate:String):Measurement = {

    val sum = measurements
      .foldLeft(0.0) {
        (z, i) => z + i.value
      }

    val mean = sum / measurements.length

    new Measurement(
      measurements.head.neId,
      measurements.head.metricId,
      measurements.head.resourceId, // TODO Replace without assuming they are all the same resource
      seconds,//now.getEpochSecond,
      nano,//now.getNano,
      mean,
      mdate,//LocalDateTime.ofInstant(now, ZoneOffset.UTC).format(formatterTime).substring(0,10),
      getLabelMetricAggregation(measurements.head.metricName),
      nodeIp
    )

  }


  /*
   * It queries the label for aggregation of raw measurements
   */

  def getLabelMetricAggregation(rawLabel:String):String ={
    rawLabel match {
      case Temperature =>
        rawLabel
      case ThroughputIn =>
        rawLabel
      case ThroughputOut =>
        "bandwidth"
      case x:Any =>
        log.debug("received unknown metric {}, sender: {}",x,sender)
        rawLabel

    }

  }


  //def flatten(l: List[Measurement]): List[Measurement] = l match {
  //  case Nil => Nil
  //  case (h:List[Measurement])::tail => flatten(h):::flatten(tail)
  //  case h::tail => h::flatten(tail)
  //}

}
