package org.ngcdi.sckl
import akka.actor._

import kamon.Kamon
import kamon.metric.Timer.Started



import org.ngcdi.sckl.NetworkUtils._
import Constants._
import ClusteringConfig._

object LogginMsg

trait ScklLogging extends ActorLogging {

  this:Actor  =>

  var timers:Seq[Tuple3[String,String,Started]] =  Seq.empty
  val msgCount = Kamon.counter("ngcdi.received_msgs.count")

  //msgTypeCount = Map.empty[String,Counter]
  //msgTypeCount += "scklmsg" -> msgCount.refine("type" -> "scklmsg")

  def countMsg(label:String):Unit={
    //TODO: Use label for different types of messages
    msgCount.withoutTags().increment()
  }

  /*
   * Publish metric to logging and kamon
   */
  def publish(label:String,metric:String,typeMetric:String,key:String,value:Double):Unit={

    val metricStr = "metric"
    val typeStr = "type"
    val keyStr = "key"

    val adjustedValue = (value*adjReport).toLong

    val friendlyKey = typeMetric match {
      case "intent" =>

        getFormattedIntent(key)

      case "device" =>
        switches(key)
      case _ =>
        key
    }

    //val mapProps: Map[String,String] = Map(
    //  metricStr->metric,
    //  typeStr->typeMetric,
    //  keyStr->friendlyKey
    //)


    Kamon
      .gauge(label)
      .withTag(metricStr,metric)
      .withTag(typeStr,typeMetric)
      .withTag(keyStr,friendlyKey)
      .update(adjustedValue)

    log.debug(label+" {"+metricStr+": "+metric+", "+typeStr+": "+typeMetric+", "+keyStr+": "+friendlyKey+"} -->"+adjustedValue)


  }

  def startTimer(timestamp:String, operation:String)={

    val timerLabel = "ngcdi."+nodeIp+".timer"

    val latencyMetric = Kamon.timer(timerLabel)
      .withTag("operation", "detection")

    timers = timers :+ Tuple3(timestamp,operation,latencyMetric.start())

    log.debug(timerLabel+" -> "+operation+" ->"+timestamp+"timer_start")
    log.debug("timer_start_timers"+timers)
  }

  def stopTimer(timestamp:String, operation:String)={
    log.debug("ts:"+timestamp+","+operation+" timers ==>"+timers+"<==")
    val latencyMetrics = timers.filter(_._1 == timestamp).collect{case t:Tuple3[String,String,Started] => t._3}
    if(!latencyMetrics.isEmpty){
      val latencyMetric = latencyMetrics.head
      log.debug("latency_metric:"+latencyMetric)
      latencyMetric.stop()
      log.debug(" -> "+operation+" ->"+timestamp+"timer_stop")
    }else
       log.debug(" -> "+operation+" ->"+timestamp+" NOT FOUND")
  }

}
