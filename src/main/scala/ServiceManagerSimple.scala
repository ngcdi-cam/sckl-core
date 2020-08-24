package org.ngcdi.sckl

import akka.actor._
import scala.concurrent.duration._
import org.vca._
import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.Config._
import org.ngcdi.sckl.sdn._
import org.ngcdi.sckl.behaviour._

object ServiceManagerSimple {
  def props(smtype: String): Props = {
    smtype match {
      // case "intent+file" =>
      //   Props(
      //     new ServiceManagerSimple() with IntentServiceView
      //   )
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
    with CombinedDetectorAndActuatorBehaviour
    // with NetworkAwarenessStatsStreamerBehaviour
    // with RESTServiceActuator
    // with PredictiveAnalytics
    with NetworkAwarenessManagerReceiverBehaviour
    with WebServer {

  var reqRegistrations: Int = 0
  var addresses: Seq[String] = Seq.empty

  override def preStart(): Unit = {
    overheatingAnomalyDetectorPrestart()
    congestionAnomalyDetectorPrestart()
    
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
      .orElse(networkAwarenessManagerReceiverBehaviour)
      // .orElse(awarenessStatsStreamerBehaviour)
      .orElse(scklBehaviour) // Parent behaviour at last
  }

  val smBehaviour: Receive = {
    case SMReady(q) =>
      countMsg("scklmsg")
      self ! MonitorRegistration(q)
      //triggerAction("0",Option(0),Seq.empty)
      log.info("Service Manager Ready! (" + q + ")")

    case MonitorRegistration(q) =>
      countMsg("scklmsg")
      monitorRegistration(q)

    case InfrastructureReady(infrastructure: Seq[String]) =>
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
