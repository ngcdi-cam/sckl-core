package org.ngcdi.sckl

import org.ngcdi.sckl.Constants._
import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory

case object NetworkUtils {

  val log = LoggerFactory.getLogger("org.ngcdi.sckl.NetworkUtils")

  /*
   * Calculates the throughput (byterate)) of given list of elements
   * x_i: elements
   * n: size of sample
   */

  def getThroughputBytes (oldBytes:Long,currentBytes:Long,oldLife:Long,currentLife:Long):Option[Double] = {

    try{
      log.debug("cb->"+currentBytes+"--ob-->"+oldBytes+"<--")
      val deltaBytes = currentBytes -  oldBytes
      val deltaLife = currentLife - oldLife
      log.debug("deltaBytes -->"+deltaBytes+"<--| deltaLife -->"+deltaLife)

      if (deltaLife > 0 && deltaBytes >= 0)
        Option(8 * 1.0 * deltaBytes / deltaLife)
      else
        None
    }catch{
      case ex:Exception => {
        log.error("Error calculating Throughput", ex)
        None
      }
    }
  }


  def getThroughputKBytes (oldBytes:Long,currentBytes:Long,oldLife:Long,currentLife:Long):Option[Double] = {
    getThroughputBytes (oldBytes:Long,currentBytes:Long,oldLife:Long,currentLife:Long) match {
      case d:Some[Double] =>
        Option(d.get / 1000)
      case None =>
        None
    }
  }

  def getFormattedIntent(intentId:String):String = {
    if(hosts.contains(intentId.substring(0,22)) && hosts.contains(intentId.substring(22,intentId.length())))
      "p2p: "+hosts(intentId.substring(0,22))+" -> "+hosts(intentId.substring(22,intentId.length()))
   else
     ""
  }

  def getSimplifiedIntent(intentId:String):String = {
    if(simplifiedIntents.contains(intentId))
      simplifiedIntents(intentId)
    else
      ""
  }
}
