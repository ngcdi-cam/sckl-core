package org.ngcdi.sckl

import org.ngcdi.sckl.model.PortStat
import java.nio.file.{Files,FileSystems}

import scala.concurrent.duration._

import akka.NotUsed
import akka.actor._
//import akka.stream.alpakka.file.scaladsl.FileTailSource
//import _root_.akka.stream.{ActorMaterializer}
import akka.stream.scaladsl._
//import akka.cluster.Cluster
//import akka.cluster.ClusterEvent._
//import akka.cluster.Member

import kamon.Kamon

import ClusteringConfig._
import msgs._
import Constants._
import org.vca._
import org.ngcdi.sckl.model._

abstract class DigitalAssetBase(id: String, localProcessor:ActorRef) extends ScklActor
    with DAController
    with ConnectionBehaviour
{

 // import context._
  //implicit val system: ActorSystem = context.system
  //implicit val materializer = ActorMaterializer()
  var lView:ActorRef = _
  var lViewPath:ActorPath = _

  //Metrics
  //val msgCount = Kamon.counter("ngcdi.da._received_msgs.count")

 def getLocalProcessorPath():ActorPath = {
    return localProcessor.path
  }

  def getTargetPaths():Seq[ActorPath]

  def startLocalView():ActorRef = {
    log.info("EJECUTANDO START LOCAL VIEW")
    val lView = context.actorOf(LocalView.props(
      20,  // 20 seconds to report,
      getTargetPaths,
      getLocalProcessorPath
    ), name = "localView"+nodeName)

    return lView
  }


  def getLocalViewPath():ActorPath ={
    return lViewPath
  }

  def SetLocalViewPath(p:ActorPath):Unit ={
    lViewPath = p
  }


  def baseBehaviour:Receive ={
    case AnomalyDetected(timestamps:Seq[String], resultAD:String) =>
      countMsg("scklmsg")
      timestamps.foreach {
        x =>
        log.debug("anomaly_detected:"+x)
        stopTimer(x,"anomd")
      }
  }

}
