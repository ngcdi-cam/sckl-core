package org.ngcdi.sckl

import akka.actor._
import scala.concurrent.duration._
import org.vca._
import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.Config._
import org.ngcdi.sckl.sdn._
import org.ngcdi.sckl.adm._
import org.ngcdi.sckl.behaviour.awareness.AwarenessManagerReceiverBehaviour
import org.ngcdi.sckl.behaviour.anomaly.SimpleCombinedDetectorAndActuatorBehaviour

/*
* Companion object for actor class. It matches incoming parameter from ServiceManagerLauncher
* to determine which ServiceManagerSimple actor to create. So far, it implements
* one that uses intents API from ONOS and another that use basic openflow (Ryu based).
*/
object ServiceManagerSimple{
  def props(smtype:String): Props = {
    smtype match{
      //ServiceManager using Intent API - ONOS
      case "intent+file" =>
        Props(
          new ServiceManagerSimple()
            // with IntentServiceView
        )
      //ServiceManager using openflow API (Ryu )
      case _ =>
        Props(new ServiceManagerSimple())
    }
  }
}

class ServiceManagerSimple()
    extends ScklActor
    with Remoting
    with ServiceView
    // with GroupAnomalyDetector
    with SimpleCombinedDetectorAndActuatorBehaviour
    // with AwarenessStatsStreamerBehaviour
    // with RESTServiceActuator
    // with PredictiveAnalytics
    with AwarenessManagerReceiverBehaviour
    with WebServer {

  var reqRegistrations: Int = 0
  var addresses: Seq[String] = Seq.empty

  /*
  * Prestarts all the behaviours from the traits
  */
  override def preStart(): Unit = {
    combinedDetectorAndActuatorPrestart()
    
    connPreStart()
    serviceViewPreStart()

    // awarenessStatsStreamerPrestart()
    // actuatorPreStart()
    // predictivePreStart()
  }

  override def receive = {
    smBehaviour
      .orElse(connBehaviour)
      .orElse(svBehaviour)
      .orElse(anomalyDetectorBehaviour)
      .orElse(anomalyActuatorBehaviour)
      // .orElse(anomalyHandlerBehaviour)
      // .orElse(predictiveBehaviour)
      // .orElse(actBehaviour)
      .orElse(awarenessManagerReceiverBehaviour)
      // .orElse(awarenessStatsStreamerBehaviour)
      .orElse(scklBehaviour) // Parent behaviour at last
  }

  val smBehaviour: Receive = {

    //Start message to indicate that SM is running and requires q Digital Assets
    case SMReady(q) =>
      countMsg("scklmsg")
      self ! MonitorRegistration(q)
      //triggerAction("0",Option(0),Seq.empty)
      log.info("Service Manager Ready! (" + q + ")")

    //To wait until FunctionProvisioner is ready so SM can send request of DAs
    case MonitorRegistration(q) =>
      countMsg("scklmsg")
      monitorRegistration(q)

    //FunctionProvisioner sends this msg when q DA agents requested are up and registered
    // So SM can start them.
    case InfrastructureReady(infrastructure:Seq[String]) =>
      countMsg("scklmsg")
      log.info("Infrastructure received")
      addresses = addresses ++ infrastructure
      infrastructure.foreach { a =>
        log.info("Sent START message to: " + a.toString)
        context.actorSelection(a) ! SenseFlow(keyHosts)
      }

  }

  /*
   *  Monitors until Function Provisioner is Ready to provision q assets
   */

  def monitorRegistration(q: Int) = {
    //log.debug("reqRegistrations=>"+reqRegistrations+" --- functionProv => "+ FunctionProvisioner)
    log.debug("FP==>" + functionProvisioner + "<==")
    if (
      functionProvisioner == ActorRef.noSender ||
      functionProvisioner == null
    ) { // Wait until one function provisioner is ready
      system.scheduler.scheduleOnce(3 seconds, self, MonitorRegistration(q))
    } else
      functionProvisioner ! NewInfrastructure(q)
  }

}
