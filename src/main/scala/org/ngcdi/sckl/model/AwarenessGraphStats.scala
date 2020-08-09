package org.ngcdi.sckl.model

//   "src": 2,
//   "dst": 1,
//   "weight": 1,
//   "delay": 0.10070955753326416,
//   "lldpdelay": 0.10052132606506348,
//   "free_bandwidth": 1998.9530128146869,
//   "bandwidth": 2000,
//   "throughput": 1.0468920392584604


final case class AwarenessGraphStats(
    src: Int,
    dst: Int,
    weight: Int,
    delay: Double,
    free_bandwidth: Double,
    bandwidth: Double,
    throughput: Double
) extends Model

final case class AwarenessGraphStatsKey(value: String) extends AnyVal
