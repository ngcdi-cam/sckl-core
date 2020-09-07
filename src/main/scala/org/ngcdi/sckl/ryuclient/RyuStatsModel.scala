package org.ngcdi.sckl.ryuclient

import org.ngcdi.sckl.model.Model

final case class RyuMatchRule(
    in_port: Option[Int],
    dl_src: Option[String],
    dl_dst: Option[String],
    dl_type: Option[Int],
    nw_src: Option[String]
) {
  override def toString(): String = {
    return (
      dl_src match {
        case Some(s) =>
          dl_dst match {
            case Some(d) =>
              s + " -> " + d
            case None =>
              "dl_src:" + s
          }
        case None =>
          dl_dst match {
            case Some(d) =>
              "dl_dst:" + d
            case None =>
              ""
          }
      }
    )
  }
}

final case class RyuFlowStat(
    byte_count: Long,
    duration_sec: Long,
    packet_count: Long,
    actions: Seq[String],
    matchr: RyuMatchRule
) extends Model

final case class RyuPortStat(
    rx_packets: Long,
    rx_bytes: Long,
    tx_packets: Long,
    tx_bytes: Long,
    rx_errors: Long,
    tx_errors: Long,
    rx_dropped: Long,
    tx_dropped: Long,
    // port_no: Option[Int]
) extends Model

final case class RyuSwitchStat(
  dpid: Int,
  metrics: Map[String, Double]
)