package org.ngcdi.sckl.model
//rx_packets	:	509
//tx_packets	:	510
//rx_bytes	:	30540
//tx_bytes	:	30600
//rx_dropped	:	0
//tx_dropped	:	0
//rx_errors	:	0
//tx_errors	:	0
//rx_frame_err	:	0
//rx_over_err	:	0
//rx_crc_err	:	0
//collisions	:	0
//duration_sec	:	459
//duration_nsec	:	134000000
//port_no	:	3

final case class PortStat(
  //rx_packets:Long,
  rx_bytes:Long,
    //tx_packets:Long,
  tx_bytes:Long,
  duration_sec:Long,
  //rx_errors:Long,
  //tx_errors:Long,
  port_no:String
)extends Model

final case class PortStatsKey(value: String) extends AnyVal
//final case class PortStatsValue(value: Set[PortStat]) extends AnyVal
