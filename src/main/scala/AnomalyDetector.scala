package org.ngcdi.sckl

import org.ngcdi.sckl.msgs._

final case class ResultAD(tick:Int,deviceId:String,resultAD:Option[Int], details:Seq[Tuple2[String,String]])

/*
* Super trait for anomaly detection implementations
*/
trait AnomalyDetector {

  this: ScklActor  =>

  //Map for tracking when anomalies are handled using the discrete time (ticks/cycles)
  // This is to avoid handling (triggering actions) for the same anomaly more than once.
  // Map[String,Int] where String is the deviceId and Int in the tick when anomaly was handled
  var anomalyHandled:Map[String,Int]=_
  val anomalyHandlerBehaviour:Receive = {

    //To process result of anomaly detection
    //resultAD is Option as there could be different types of anomalies detected
    case ResultAD(tick:Int,deviceId:String,resultAD:Option[Int], details:Seq[String])=>
      countMsg("scklmsg")
      processResultAD(tick,deviceId,resultAD, details)
  }

  /*
  * Runs anomaly detection for a given tick, sample of measurements (from DA readings)
  * metric and using as reference the given threshold. Usually when that threshold is
  * reached the anomaly is triggered.
  */
  def runAnomalyDetection(tick:Int, sample:Seq[Measurement],metricName:String,threshold:Double):Unit

  /*
  * Send the msg to the actor in charge of processing the results of the AnomalyDetector
  * When implemented in a ScklActor, this should have an Actuator behaviour that will
  * determine the action to be taken given the resultAD.
  */
  def processResultAD(tick:Int,deviceId:String,resultAD:Option[Int], details:Seq[Tuple2[String,String]]):Unit

  /*
  *
  */
  def anomalyDetectorPreStart():Unit
}
