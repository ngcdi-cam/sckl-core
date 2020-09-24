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
import org.ngcdi.sckl.behaviour.awareness.AwarenessManagerReceiverBehaviour
import org.ngcdi.sckl.behaviour.neighbouring.EnvListNeighbouringBehaviour
import org.ngcdi.sckl.behaviour.neighbouring.TargetPathsProvider
import org.ngcdi.sckl.behaviour.anomaly.SimpleCombinedDetectorAndActuatorBehaviour
import org.ngcdi.sckl.behaviour.neighbouring.AwarenessNeighbouringBehaviour
import org.ngcdi.sckl.behaviour.forwarder.MessageForwardingBehaviour
import org.ngcdi.sckl.behaviour.awareness.AwarenessServiceManagerBehaviour
import org.ngcdi.sckl.behaviour.anomaly.FlowCombinedDetectorAndActuatorBehaviour
import org.ngcdi.sckl.behaviour.messagetransport.DirectTransportBehaviour
import org.ngcdi.sckl.behaviour.messagetransport.AbstractTransportBehaviour
import org.ngcdi.sckl.behaviour.messagetransport.IndirectTransportBehaviour
import org.ngcdi.sckl.behaviour.anomaly.AbstractCombinedDetectorAndActuatorBehaviour
import org.ngcdi.sckl.behaviour.messagetransport.EnvListTransportTopologyBehaviour

object DigitalAsset {
  def props(datype: String, id: String, localProcessor: ActorRef): Props = {
    datype match {
      case "intent+file" =>
        Props(
          new DigitalAsset(id, localProcessor)
            with IntentCombinedController
            with Remoting
        )
      // sdn_type,network_topology,agent_topology,ext_conf
      case "awareness,agtopo1" =>
        // Also set
        // neighbours: ""
        // sm_q: 1
        Props(
          new DigitalAsset(id, localProcessor, true, false, false)
            with CombinedController
            with Remoting
            with DirectTransportBehaviour
            // with EnvListNeighbouringBehaviour
        )
      case "awareness,agtopo2" =>
        // Also set
        // neighbours: "c1"
        // sm_q: 0
        Props(
          new DigitalAsset(id, localProcessor, true, false, false)
            with CombinedController
            with Remoting
            with DirectTransportBehaviour
            with EnvListNeighbouringBehaviour
            with SimpleCombinedDetectorAndActuatorBehaviour
        )
      case "awareness,agtopo3" =>
        Props(
          new DigitalAsset(id, localProcessor, false, true, false)
            with CombinedController
            with Remoting
            with DirectTransportBehaviour
            with AwarenessNeighbouringBehaviour
            with SimpleCombinedDetectorAndActuatorBehaviour
        )
      case "awareness,agtopo4" =>
        Props(
          new DigitalAsset(id, localProcessor, false, true, true)
            with CombinedController
            with Remoting
            with DirectTransportBehaviour
            with AwarenessNeighbouringBehaviour
            with FlowCombinedDetectorAndActuatorBehaviour
        )
      
      case "awareness,agtopo4,indirect" =>
        Props(
          new DigitalAsset(id, localProcessor, false, true, true)
            with CombinedController
            with Remoting
            with IndirectTransportBehaviour
            with EnvListTransportTopologyBehaviour
            with AwarenessNeighbouringBehaviour
            with FlowCombinedDetectorAndActuatorBehaviour
        )

      // case "awareness,mesh,agtopo4" =>
      // case "awareness,mesh,agtopo4,multisdn" =>
      // case "awareness,mesh,agtopo5" =>
      // case "awareness,custom,agtopo4" =>
      // case "awareness,ring,agtopo4" =>
      // case "awareness,complete_bipartite,agtopo4" =>
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

class DigitalAsset(
    id: String,
    localProcessor: ActorRef,
    _awarenessStatsSensorEnabled: Boolean = false,
    _awarenessSwitchStatsSensorEnabled: Boolean = true,
    _awarenessSwitchFlowSensorEnabled: Boolean = true
) extends DigitalAssetBase(
      id,
      localProcessor,
      _awarenessStatsSensorEnabled,
      _awarenessSwitchStatsSensorEnabled,
      _awarenessSwitchFlowSensorEnabled
    )

    with AwarenessManagerReceiverBehaviour
    with TargetPathsProvider
    with ServiceView
    with AbstractCombinedDetectorAndActuatorBehaviour
    // with FlowCombinedDetectorAndActuatorBehaviour // for topology 4-5
    // with SimpleCombinedDetectorAndActuatorBehaviour // for topology 1-3

    // with MessageForwardingBehaviour // deprecated
    // with DirectTransportBehaviour // for topology 1-4
    // with IndirectTransportBehaviour // for topology 5, use with mesh_indirect topo in sckl-run
    with AbstractTransportBehaviour
    with AwarenessServiceManagerBehaviour {

  var keyHosts: Seq[String] = _

  override def preStart(): Unit = {
    connPreStart()
    combinedDetectorAndActuatorPrestart()
    neighbouringBehaviourPrestart()
    serviceViewPreStart()
    awarenessServiceManagerPrestart()
    transportPrestart()
  }

  override def getLocalProcessorPath(): ActorPath = {
    return localProcessor.path
  }

  def receive = {
    daBehaviour
      .orElse(transportBehaviour)
      .orElse(connBehaviour)
      .orElse(svBehaviour)
      .orElse(awarenessManagerReceiverBehaviour)
      .orElse(anomalyActuatorBehaviour)
      .orElse(anomalyDetectorBehaviour)
      .orElse(awarenessServiceManagerBehaviour)
      .orElse(ctlBehaviour)
      .orElse(baseBehaviour)
      .orElse(scklBehaviour)
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
        // cluster.down(cluster.selfAddress)
        context.stop(localProcessor)
        context.stop(self)
      }

  }

  def getKeyHosts(): Seq[String] = {
    return keyHosts
  }

  //It triggers initialisation of sensors and linkage
}
