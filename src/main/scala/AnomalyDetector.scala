package org.ngcdi.sckl

import org.ngcdi.sckl.msgs._

final case class ResultAD(
    tick: Int,
    deviceId: String,
    resultAD: Option[Int],
    details: Seq[Tuple2[String, String]]
)

trait AnomalyDetector {
  this: ScklActor =>
  var anomalyHandled: Map[String, Int] = _

  val anomalyHandlerBehaviour: Receive = {

    case ResultAD(
          tick: Int,
          deviceId: String,
          resultAD: Option[Int],
          details: Seq[String]
        ) =>
      countMsg("scklmsg")
      processResultAD(tick, deviceId, resultAD, details)
  }

  def runAnomalyDetection(
      tick: Int,
      sample: Seq[Measurement],
      metricName: String,
      threshold: Double
  ): Unit
  def processResultAD(
      tick: Int,
      deviceId: String,
      resultAD: Option[Int],
      details: Seq[Tuple2[String, String]]
  ): Unit
  def anomalyDetectorPreStart(): Unit

  def obtainTimestamptsSample(
      sample: Seq[Measurement]
  ): Seq[Tuple2[String, String]] = {
    val timeStamps = sample
      .map(m => Tuple2(m.neId, m.nanos.toString()))
      .collect { case t: Tuple2[String, String] => t }
      .toSeq
    timeStamps
  }
}
