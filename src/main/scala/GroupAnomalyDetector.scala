package org.ngcdi.sckl

import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.StatisticsUtils._
import ClusteringConfig._
import Constants._
import scala.util.{Failure, Success}
import akka.actor._
import scala.math.{abs}

//Detects anomaly comparing within a group
trait GroupAnomalyDetector extends AnomalyDetector {

  this: ScklActor with Actuator =>

  def isAllDigits(x: String): Boolean = x forall Character.isDigit

  override def anomalyDetectorPreStart() = {
    anomalyHandled = Map.empty
  }

  override def runAnomalyDetection(
      tick: Int,
      sample: Seq[Measurement],
      metricName: String,
      threshold: Double
  ): Unit = {

    val agOthers = calculateOthers(sample, metricName)
    val agDevices = calculatePerDevice(sample, metricName)
    val tstamps = obtainTimestamptsSample(sample)

    agDevices.foreach { agr =>
      val avgOthers = agOthers
        .filter(_._1 == agr._1)
        .collect { case v: Tuple3[String, Double, Double] => v._2 }
        .head
      val diff = agr._2 - avgOthers

      if (agr._2 != Double.NaN && avgOthers != Double.NaN) {
        log.debug(
          "==>Ag. Value for " + agr._1 + " is " + agr._2 + " - Others' value is: " + avgOthers + ", difference:  " + diff + " and threshold: " + threshold + "<=="
        )

        if (diff < 0 && abs(diff) > threshold)
          self ! ResultAD(tick, agr._1, Option(2), tstamps)
        else
          self ! ResultAD(tick, agr._1, Option(0), tstamps)
      } else
        self ! ResultAD(tick, agr._1, Option(0), tstamps)

    }

  }

  override def processResultAD(
      tick: Int,
      deviceId: String,
      resultAD: Option[Int],
      details: Seq[Tuple2[String, String]]
  ) = {
    resultAD match {
      case ad: Some[Int] =>
        log.debug(
          tick + ", " + deviceId + ", " + ad.get + ",OK,==>" + anomalyHandled + "<=="
        )
        if (
          tick > monitorFrom
          && ad.get > 1
          && !anomalyHandled.keySet.contains(deviceId)
        ) {
          log.debug("Anomaly_detected: " + tick + "->" + deviceId)
          triggerAction(deviceId, resultAD, Seq.empty)
          anomalyHandled += (deviceId -> tick)
          log.debug("anom_handled:" + anomalyHandled)
        } else {
          log.debug("No anomaly was detected")
        }

        val filteredts = details
          .filter(_._1 == deviceId)
          .collect {
            case ts: Tuple2[String, String] =>
              ts._2
          }

        log.debug("timetamps:" + filteredts)

        val nodeAgentAddress =
          "akka://" + clusterName + "@" + "c" + deviceId + ":" + nodePort + "/user/" + digitalAssetName
        log.debug("looking_anom_node_address:" + nodeAgentAddress)
        val nodeAgent =
          context.system.actorSelection(nodeAgentAddress).resolveOne()(timeout)

        nodeAgent.onComplete {
          case Success(r) =>
            r ! AnomalyDetected(filteredts, resultAD.get.toString)
            log.debug(
              "Got anom_node_address: " + r + "--->" + filteredts + "--->" + resultAD.get.toString
            )
          case Failure(e) =>
            //log.error("ERROR getting_anom_node_address: "+e.getMessage)
            log.debug("Anomaly handling: Switched off")
        }

      case None =>
        log.error(tick + "," + deviceId + ",ERROR")
    }
  }

  def calculateOthers(
      sample: Seq[Measurement],
      metricName: String
  ): Seq[Tuple3[String, Double, Double]] = {
    val deviceGroup = sample
      .groupBy(_.neId)
      .map { neGrp =>
        val othersGroup = sample
          .filter(_.neId != neGrp._1)
          .collect { case m: Measurement => m.value }

        val t = Tuple3(
          neGrp._1,
          calculateMean(othersGroup),
          calculateSD(othersGroup)
        )
        publish("ngcdi.others.mean", metricName, "flow", t._1, t._2)
        publish("ngcdi.others.sd", metricName, "flow", t._1, t._3)
        t
      }
      .collect { case t: Tuple3[String, Double, Double] => t }
      .toSeq
    deviceGroup
  }

  def calculatePerDevice(
      sample: Seq[Measurement],
      metricName: String
  ): Seq[Tuple3[String, Double, Double]] = {
    val deviceGroup = sample
      .groupBy(_.neId)
      .map { neGrp =>
        val deviceGroup = neGrp._2.collect { case m: Measurement => m.value }
        val t = Tuple3(
          neGrp._1,
          calculateMean(deviceGroup),
          calculateSD(deviceGroup)
        )
        //publish("ngcdi.device.mean",a,"device",t._1,t._2)
        //publish("ngcdi.device.sd",a,"device",t._1,t._3)
        publish("ngcdi.device.mean", metricName, "flow", t._1, t._2)
        publish("ngcdi.device.sd", metricName, "flow", t._1, t._3)
        t
      }
      .collect { case t: Tuple3[String, Double, Double] => t }
      .toSeq

    deviceGroup
  }
}
