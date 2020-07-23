package org.ngcdi.sckl.model

import spray.json._



//object		{13}
//priority	:	65535
//cookie	:	0
//idle_timeout	:	0
//hard_timeout	:	0
//byte_count	:	2569020
//duration_sec	:	38588
//duration_nsec	:	619000000
//packet_count	:	42817
//length	:	96
//flags	:	0
//actions		[1]
//0	:	OUTPUT:CONTROLLER//
//  match		{2}
//dl_dst	:	01:80:c2:00:00:0e
//dl_type	:	35020
//table_id	:	0

final case class MatchRule(in_port:Option[Int],dl_src:Option[String],dl_dst:Option[String], dl_type:Option[Int], nw_src:Option[String]
  //,nw_dst:Option[String], nw_proto:Option[String], tp_src:Option[String], tp_dst:Option[String]
){
  override def toString():String={
    return (
      dl_src match {
        case Some(s) =>
          dl_dst match {
            case Some(d) =>
              s +" -> "+ d
            case None =>
              "dl_src:"+s
          }
        case None =>
          dl_dst match {
            case Some(d) =>
              "dl_dst:"+d
            case None =>
              ""
          }
      }
    )
  }

}
final case class FlowStat(
  byte_count:Long,
  duration_sec:Long,
  packet_count:Long,
  actions:Seq[String],
  matchr:MatchRule
) extends Model
final case class FlowStats(flowStats:Seq[FlowStat]) extends Model
final case class FlowStatsKey(value: String) extends AnyVal
final case class FlowStatsValue(value: Set[FlowStat]) extends AnyVal
final case class FlowStatsMap(key:FlowStatsKey,value:FlowStatsValue)
