package org.ngcdi.sckl.anomalydetector

import org.ngcdi.sckl.msgs.Measurement
import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.ryuclient.NetworkAwarenessRawFlowEntry

case class AwarenessLinkCongestion(
  throughput: Double,
  bandwidth: Double,
  usage: Double,
  flows: Seq[NetworkAwarenessRawFlowEntry]
)

case class AwarenessCongestionAnomalyDetectionResult(
    threshold: Double, // threshold of proportion of the total bandwidth
    congestedLinks: Map[Tuple2[Int, Int], AwarenessLinkCongestion],
    // congestedSwitches: Map[Tuple2[Int, Int], Tuple2[Double, Seq[Tuple5[Double, String, String, Int, Int]]]] 
    // (link_src, link_dst) -> (bandwidth_usage_proportion, (throughput, flow_src_ip, flow_dst_ip, flow_src_ip_dpid -> flow_dst_ip_dpid))
) extends AnomalyDetectionResult

class AwarenessCongestionAnomalyDetector(
    onFailure: AwarenessCongestionAnomalyDetectionResult => Unit = AnomalyDetectorUtils.noHandleFailure,
    filter: Int => Boolean = AnomalyDetectorUtils.noFilter
) extends AbstractAnomalyDetector[Double, AwarenessCongestionAnomalyDetectionResult](
      onFailure,
      filter
    ) {
  override val name: String = "AwarenessCongestionDetector"
  override val kind: AnomalyKind = SingleAnomaly

  override def check(
      tick: Int,
      sample: Seq[Measurement],
      options: Double // threshold
  ): Option[AwarenessCongestionAnomalyDetectionResult] = {
    val threshold = options

    val aggregatedSample = sample.groupBy(_.resourceId).map {
      case (link, measurements) => 
        Tuple2(
          link, 
          measurements.groupBy(_.metricName).map {
            case (metricName, measurements) =>
              Tuple2(metricName, measurements.map(_.value).sum / measurements.size)
        }.toMap)
    }.toMap

    println("aggregatedSample: " + aggregatedSample)

    val congestedLinks = aggregatedSample.filter {
      case (link, stats) =>
        stats.contains(awarenessThroughput) && stats.contains(awarenessBandwidth)
    }
    .map {
      case (link, stats) =>
        val l = link.split(":")
        assert(l.length == 2)
        val throughput = stats.get(awarenessThroughput).get
        val bandwidth = stats.get(awarenessBandwidth).get
        val usage = throughput / bandwidth
        println("link: " + link + ", usage: " + usage)
        Tuple2(Tuple2(l(0).toInt, l(1).toInt), Tuple3(throughput, bandwidth, usage))
    }.filter(_._2._3 >= threshold).toMap

    val flows = aggregatedSample.filter {
      case (link, stats) => stats.contains(awarenessFlowThroughput)
    }
    .map {
      case (link, stats) =>
        val l = link.split(":")
        assert(l.length == 6)
        val src = l(0).toInt
        val dst = l(1).toInt
        val srcIp = l(2)
        val dstIp = l(3)
        val srcIpDpid = l(4).toInt
        val dstIpDpid = l(5).toInt
        assert(stats.contains(awarenessFlowThroughput))
        val flowThroughput = stats.get(awarenessFlowThroughput).get
        Tuple2(Tuple2(src, dst), Tuple5(flowThroughput, srcIp, dstIp, srcIpDpid, dstIpDpid))
    }.toSeq

    println("flows: " + flows)

    val merged = congestedLinks.map {
      case (link, info) =>
        val throughput = info._1
        val bandwidth = info._2
        val usage = info._3
        val flowsOfLink = flows.filter(_._1 == link).map {
          case ((src, dst), (flowThroughput, srcIp, dstIp, srcIpDpid, dstIpDpid)) =>
            NetworkAwarenessRawFlowEntry(src, dst, -1, -1, srcIp, dstIp, srcIpDpid, dstIpDpid, flowThroughput)
        }
        Tuple2(link, AwarenessLinkCongestion(throughput, bandwidth, usage, flowsOfLink))
        // val bandwidth = 
        // Tuple2(link, Tuple2(usage, flows.filter(_._1 == link).map(_._2).toSeq))
    }.toMap

    if (merged.isEmpty) {
      None
    } else {
      Some(AwarenessCongestionAnomalyDetectionResult(threshold, merged))
    }
  }
}
