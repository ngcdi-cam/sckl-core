package org.ngcdi.sckl.ryu

import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.unmarshalling._
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.actor.ActorSystem
import scala.concurrent.Future
import scala.collection.mutable
import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.base.SimpleRestClient

object RyuStatsClient {
  lazy val lastReadings = mutable.Map.empty[Int, RyuSwitchStat]
  final private def getSwitchStatsFromPortStats(dpid: Int, stats: Seq[RyuPortStat]): RyuSwitchStat = {
    val metrics = mutable.Map.empty[String, Double]
    stats.foreach { stat => 
      metrics.update(ryuRxBytes, metrics.getOrElse(ryuRxBytes, 0.0) + stat.rx_bytes)
      metrics.update(ryuTxBytes, metrics.getOrElse(ryuTxBytes, 0.0) + stat.tx_bytes)
      metrics.update(ryuRxPackets, metrics.getOrElse(ryuRxPackets, 0.0) + stat.rx_packets)
      metrics.update(ryuTxPackets, metrics.getOrElse(ryuTxPackets, 0.0) + stat.tx_packets)
      metrics.update(ryuRxDropped, metrics.getOrElse(ryuRxDropped, 0.0) + stat.rx_dropped)
      metrics.update(ryuTxDropped, metrics.getOrElse(ryuTxDropped, 0.0) + stat.tx_dropped)
      metrics.update(ryuRxErrors, metrics.getOrElse(ryuRxErrors, 0.0) + stat.rx_errors)
      metrics.update(ryuTxErrors, metrics.getOrElse(ryuTxErrors, 0.0) + stat.tx_errors)
    }
    RyuSwitchStat(dpid, metrics.toMap)
  }

  final private def getSwtichStatsDelta(newStats: RyuSwitchStat): RyuSwitchStat = {
    val dpid = newStats.dpid
    val oldStats = lastReadings.getOrElse(dpid, RyuSwitchStat(dpid, Map.empty))
    lastReadings.update(dpid, newStats)
    getSwitchStatsDelta(oldStats, newStats)
  }

  final private def getSwitchStatsDelta(oldStats: RyuSwitchStat, newStats: RyuSwitchStat): RyuSwitchStat = {
    RyuSwitchStat(oldStats.dpid, newStats.metrics.map {
      case (name, value) =>
        Tuple2(name, value - oldStats.metrics.getOrElse(name, 0.0))
    })
  }
}

class RyuStatsClient(baseUrl: String)
    extends SimpleRestClient(baseUrl) {

  implicit val matchRuleFormat = jsonFormat5(RyuMatchRule)
  implicit val flowStatFormat = jsonFormat5(RyuFlowStat)
  implicit val portStatFormat = jsonFormat8(RyuPortStat)

  final def getPortStats(dpid: Int)(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[RyuPortStat]] = {
    for {
      httpResponse <- httpGet(s"/stats/port/$dpid")
      statsOrig <- Unmarshal(httpResponse).to[Map[String, Seq[RyuPortStat]]]
      stats <- Future { statsOrig.get(dpid.toString()).get }
    } yield stats
  }

  final def getSwitchStatsDelta(dpid: Int)(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[RyuSwitchStat] = {
    getPortStats(dpid).map { portStats =>
      val switchStats = RyuStatsClient.getSwitchStatsFromPortStats(dpid, portStats)
      RyuStatsClient.getSwtichStatsDelta(switchStats)
    }
  }

}
