package org.ngcdi.sckl.ryuclient

import org.ngcdi.sckl.model.Model

final case class NetworkAwarenessRawStatEntry (
    src: Int,
    dst: Int,
    src_port: Int,
    dst_port: Int,
    metrics: Map[String, Double]
) extends Model

final case class NetworkAwarenessRawFlowEntry (
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

final case class NetworkAwarenessService (
    id: Int,
    src: String,
    dst: String,
    weights: Map[String, Double]
) extends Model

final case class NetworkAwarenessRawLink (
    src: Int,
    dst: Int,
    src_port: Int,
    dst_port: Int
) extends Model

final case class NetworkAwarenessRawCrossDomainLink (
  src_dpid: Int,
  src_port: Int,
  src_controllerId: Int,
  dst_dpid: Int,
  dst_port: Int,
  dst_controllerId: Int
) extends Model

final case class NetworkAwarenessRawAccessTableEntry (
  host_ip: String,
  host_mac: String,
  dpid: Int,
  port: Int
) extends Model

final case class NetworkAwarenessRawPathInfo (
  stats: Seq[NetworkAwarenessRawPathPairInfo],
  switch_weights: Map[Int, Double],
) extends Model

final case class NetworkAwarenessRawPathPairInfo (
  src: Int,
  dst: Int,
  metrics: Map[String, Double],
) extends Model

final case class NetworkAwarenessRawAccessTableEntryPinning (
  ip: String,
  dpid: Int,
  port: Int
) extends Model

final case class NetworkAwarenessRawPathInfoPairRequest (
  src: Int,
  dst: Int,
  src_ip: String,
  dst_ip: String
) extends Model

final case class NetworkAwarenessRawPathInfoRequest (
  src_dst_pairs: Seq[NetworkAwarenessRawPathInfoPairRequest],
  weights: Map[String, Double]
) extends Model