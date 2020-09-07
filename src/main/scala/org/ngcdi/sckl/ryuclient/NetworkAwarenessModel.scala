package org.ngcdi.sckl.ryuclient

import org.ngcdi.sckl.model.Model

final case class NetworkAwarenessStatEntry (
    src: Int,
    dst: Int,
    src_port: Int,
    dst_port: Int,
    metrics: Map[String, Double]
) extends Model

final case class NetworkAwarenessFlowEntry (
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

final case class NetworkAwarenessLink (
    src: Int,
    dst: Int,
    src_port: Int,
    dst_port: Int
) extends Model

final case class NetworkAwarenessAccessTableEntry (
  host_ip: String,
  host_mac: String,
  dpid: Int,
  port: Int
) extends Model
