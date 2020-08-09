package org.ngcdi.sckl

import org.vca._
import org.ngcdi.sckl.sdn._
import org.ngcdi.sckl.model.PortStat
import java.nio.file.{Files, FileSystems}

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

object DigitalAsset {
  def props(datype: String, id: String, localProcessor: ActorRef): Props = {
    datype match {
      case "intent+file" =>
        Props(
          new DigitalAsset(id, localProcessor)
            with IntentCombinedController
            with Remoting
        )
      case _ =>
        Props(
          new DigitalAsset(id, localProcessor)
            with CombinedController
            //ORDER MATTERS FOR TRAITS!!!
            //with PortProcessor with FileProcessor
            with Remoting
        )
    }
  }
}

class DigitalAsset(id: String, localProcessor: ActorRef)
    extends DigitalAssetBase(id, localProcessor) {

  var keyHosts: Seq[String] = _

  var serviceManagers = IndexedSeq.empty[ActorRef]

  override def preStart(): Unit = {
    connPreStart()
  }

  override def getLocalProcessorPath(): ActorPath = {
    return localProcessor.path
  }

  override def getTargetPaths(): Seq[ActorPath] = {
    val tp = serviceManagers.map(_.path).collect {
      case tp: ActorPath => tp
    }

    log.debug("converted_tps:" + tp)
    tp
  }

  def receive = {
    daBehaviour
      .orElse[Any, Unit](connBehaviour)
      .orElse[Any, Unit](ctlBehaviour)
      .orElse[Any, Unit](baseBehaviour)
      .orElse[Any, Unit](scklBehaviour)
  }

  val daBehaviour: Receive = {
    //TODO replace name to intialise (id) receiving asset id
    case SenseFlow(kh) =>
      countMsg("scklmsg")
      log.info("Received Initialisation order from SM: " + sender)
      if (!serviceManagers.contains(sender())) {
        //context watch sender()
        serviceManagers = serviceManagers :+ sender()
      }
      log.debug("serviceManagers==>" + serviceManagers)

      keyHosts = kh

      log.debug("started_sensing:" + sensingStarted)
      if (!sensingStarted)
        startSensing()
      else
        lView ! UpdateTargets(Seq(sender.path))

    // Sense function Msgs
    case ReSense =>
      countMsg("scklmsg")
      sense(nodeName, frequencySensing)
    case StopMessage(ap: String) =>
      countMsg("scklmsg")
      val actdel = context.actorSelection(ap)
      log.debug(
        "RECEIVED STOP MESSAGE!!!!!!===>" + actdel.pathString + "<===>" + self.path.toSerializationFormat + "<===="
      )
      if (self.path.toSerializationFormat.contains(actdel.pathString)) {
        context.actorSelection(lViewPath) ! PoisonPill
//        cluster.down(cluster.selfAddress)
        context.stop(localProcessor)
        context.stop(self)
      }

  }

  def getKeyHosts(): Seq[String] = {
    return keyHosts
  }

  //It triggers initialisation of sensors and linkage

}
