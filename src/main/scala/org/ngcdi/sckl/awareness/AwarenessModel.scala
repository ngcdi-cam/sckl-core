package org.ngcdi.sckl.awareness

import org.ngcdi.sckl.model.Model

final case class AwarenessRawStatEntry (
    src: Int,
    dst: Int,
    src_port: Int,
    dst_port: Int,
    metrics: Map[String, Double]
) extends Model

final case class AwarenessRawFlowEntry (
  src: Int,
  dst: Int,
  src_port: Int,
  dst_port: Int,
  src_ip: String,
  dst_ip: String,
  src_ip_dpid: Int,
  dst_ip_dpid: Int,
  throughput: Double
) extends Model

final case class AwarenessService (
    id: Int,
    src: String,
    dst: String,
    weights: Map[String, Double]
) extends Model

final case class AwarenessRawLink (
    src: Int,
    dst: Int,
    src_port: Int,
    dst_port: Int
) extends Model

final case class AwarenessRawCrossDomainLink (
  src_dpid: Int,
  src_port: Int,
  src_controllerId: Int,
  dst_dpid: Int,
  dst_port: Int,
  dst_controllerId: Int
) extends Model

final case class AwarenessRawAccessTableEntry (
  host_ip: String,
  host_mac: String,
  dpid: Int,
  port: Int
) extends Model

final case class AwarenessRawPathInfoPre (
  stats: Seq[AwarenessRawPathPairInfo],
  switch_weights: Map[String, Double],
) extends Model

final case class AwarenessRawPathInfo (
  stats: Seq[AwarenessRawPathPairInfo],
  switch_weights: Map[Int, Double],
) extends Model

final case class AwarenessRawPathPairInfo (
  src: Int,
  dst: Int,
  metrics: Map[String, Double],
) extends Model

final case class AwarenessRawAccessTableEntryPinning (
  ip: String,
  dpid: Int,
  port: Int
) extends Model

final case class AwarenessRawPathInfoPairRequest (
  src: Int,
  dst: Int,
  src_ip: String,
  dst_ip: String
) extends Model

final case class AwarenessRawPathInfoRequest (
  src_dst_pairs: Seq[AwarenessRawPathInfoPairRequest],
  weights: Map[String, Double]
) extends Model