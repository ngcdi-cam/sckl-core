package org.ngcdi.sckl.ryuclient

import org.ngcdi.sckl.model.Model

final case class NetworkAwarenessStatEntry (
    src: Int,
    dst: Int,
    src_port: Int,
    dst_port: Int,
    metrics: Map[String, Double]
) extends Model

final case class NetworkAwarenessService (
    id: Int,
    src: Int,
    dst: Int,
    weights: Map[String, Double]
) extends Model

final case class NetworkAwarenessLink (
    src: Int,
    dst: Int,
    src_port: Int,
    dst_port: Int
) extends Model