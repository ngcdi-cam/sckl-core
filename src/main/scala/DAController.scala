package org.ngcdi.sckl

import org.ngcdi.sckl.ClusteringConfig._
import scala.concurrent.duration._
import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.DigitalAssetBase
import org.ngcdi.sckl.model.Model
import Constants._

trait DAController {
  this: DigitalAssetBase =>
  var lastReadings: Seq[Model] = Seq.empty
  var sensingStarted: Boolean = false

  def startSensors(): Unit = {
    log.info("Start Sensors..")
  }

  def startSensing(): Unit = {
    log.info("StartSensing...")

    startSensors
    lView = startLocalView()
    sensingStarted = true
    lViewPath = lView.path
    sense(nodeName, frequencySensing)
  }

  def sense(assetName: String, freqSensing: Int): Unit = {
    log.debug("ERROR sense not implemented")
  }

  def ctlBehaviour: Receive = {
    case NewMeasurements(newMeasurements: Seq[Model]) =>
      countMsg("scklmsg")
      log.debug("Processing NewMeasurement")
      updateMeasurements(newMeasurements)
  }

  def updateMeasurements(newMeasurements: Seq[Model]) = {
    log.debug("ERROR NewMeasurement is NOT Processed")
  }

  def sendToLocalView(
      node: String,
      flow: String,
      value: Double,
      time: String,
      metric: String
  ): Unit = {
    context
      .actorSelection(lViewPath) ! createRecord(node, flow, value, time, metric)
  }

  def createRecord(
      node: String,
      flow: String,
      value: Double,
      time: String,
      metric: String
  ): String = {
    log.debug(
      "New Record TO CREATE  ===>" + node + ", " + flow + "," + value + ", " + time + "<====="
    )
    val dateStr = time.substring(0, 10)
    val rcd =
      node + "," + "1," + flow + ", " + time + "," + value + "," + dateStr + "," + metric
    log.debug("New Record CREATED ===>" + rcd + "<=====")
    rcd
  }
}
