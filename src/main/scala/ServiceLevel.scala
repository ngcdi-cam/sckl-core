package org.ngcdi.sckl

//import java.time.LocalDateTime

case class ServiceLevel(
  serviceid: Long,
  lowThreshold: Double,
  highThreshold: Double,
  tolerance: Double,

  metricName: String
  //expires: LocalDateTime,
)
