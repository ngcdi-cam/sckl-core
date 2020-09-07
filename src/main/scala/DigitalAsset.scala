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
import org.ngcdi.sckl.behaviour.NetworkAwarenessManagerReceiverBehaviour
import org.ngcdi.sckl.behaviour.neighbouring.EnvListNeighbouringBehaviour
import org.ngcdi.sckl.behaviour.neighbouring.TargetPathsProvider
import org.ngcdi.sckl.behaviour.SimpleCombinedDetectorAndActuatorBehaviour
import org.ngcdi.sckl.behaviour.neighbouring.AwarenessNeighbouringBehaviour
import org.ngcdi.sckl.behaviour.forwarder.MessageForwardingBehaviour
import org.ngcdi.sckl.behaviour.awareness.AwarenessServiceManagerBehaviour
import org.ngcdi.sckl.behaviour.FlowCombinedDetectorAndActuatorBehaviour

object DigitalAsset {
  def props(datype: String, id: String, localProcessor: ActorRef): Props = {
    datype match {
      case "intent+file" =>
        Props(
          new DigitalAsset(id, localProcessor)
            with IntentCombinedController
            with Remoting
        )
      case "standalone" =>
        Props(
          new DigitalAsset(id, localProcessor)
            with CombinedController
            // with EnvListNeighbouringBehaviour
            with AwarenessNeighbouringBehaviour
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
    extends DigitalAssetBase(id, localProcessor)
    with NetworkAwarenessManagerReceiverBehaviour
    with TargetPathsProvider
    with ServiceView
    with FlowCombinedDetectorAndActuatorBehaviour
    // with SimpleCombinedDetectorAndActuatorBehaviour
    with MessageForwardingBehaviour
    with AwarenessServiceManagerBehaviour {

  var keyHosts: Seq[String] = _

  override def preStart(): Unit = {
    connPreStart()
    combinedDetectorAndActuatorPrestart()
    neighbouringBehaviourPrestart()
    serviceViewPreStart()
    awarenessServiceManagerPrestart()
  }

  override def getLocalProcessorPath(): ActorPath = {
    return localProcessor.path
  }

  def receive = {
    daBehaviour
      .orElse(messageForwardingBehaviour)
      .orElse[Any, Unit](connBehaviour)
      .orElse(svBehaviour)
      .orElse[Any, Unit](networkAwarenessManagerReceiverBehaviour)
      .orElse(anomalyActuatorBehaviour)
      .orElse(anomalyDetectorBehaviour)
      .orElse(awarenessServiceManagerBehaviour)
      .orElse[Any, Unit](ctlBehaviour)
      .orElse[Any, Unit](baseBehaviour)
      .orElse[Any, Unit](scklBehaviour)
  }

  val daBehaviour: Receive = {
    //TODO replace name to intialise (id) receiving asset id
    case SenseFlow(kh) =>
      countMsg("scklmsg")
      log.info("Received Initialisation order from SM: " + sender)
      addTargetPath(sender().path)
      keyHosts = kh
      log.debug("started_sensing:" + sensingStarted)
      if (!sensingStarted)
        startSensing()

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
