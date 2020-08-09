package org.ngcdi.sckl

import akka.actor._
import akka.serialization.{Serialization, SerializationExtension}
import akka.pattern.ask

import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import scala.math.{abs}


import org.vca._
import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.Config._
import org.ngcdi.sckl.sdn._
import org.ngcdi.sckl.adm._


object ServiceManagerSimple{
  def props(smtype:String): Props = {
    smtype match{
      case "intent+file" =>
        Props(
          new ServiceManagerSimple()
            with IntentServiceView
        )
      case _ =>
        Props(new ServiceManagerSimple()
        )
    }


  }
}

class ServiceManagerSimple() extends ScklActor
    with Remoting
    with ServiceView
    with GroupAnomalyDetector
    with RESTServiceActuator
    with PredictiveAnalytics
    with WebServer
    {

  var reqRegistrations:Int = 0
  var addresses:Seq[String] = Seq.empty


  override def preStart(): Unit = {
    connPreStart()
    serviceViewPreStart()
    actuatorPreStart()
    predictivePreStart()
  }

  def receive = {
    smBehaviour
      .orElse[Any,Unit](connBehaviour)
      .orElse[Any,Unit](svBehaviour)
      .orElse[Any,Unit](anomalyHandlerBehaviour)
      .orElse[Any,Unit](predictiveBehaviour)
      .orElse[Any,Unit](actBehaviour)
      .orElse[Any,Unit](scklBehaviour) // Parent behaviour at last
  }


  val smBehaviour: Receive = {

    case SMReady(q) =>
      countMsg("scklmsg")
      self ! MonitorRegistration(q)
      //triggerAction("0",Option(0),Seq.empty)
      log.info("Service Manager Ready! ("+q+")")

    case MonitorRegistration(q) =>
      countMsg("scklmsg")
      monitorRegistration(q)

    case InfrastructureReady(infrastructure:Seq[String]) =>
      countMsg("scklmsg")
      log.info("Infrastructure received")
      addresses = addresses ++ infrastructure
      infrastructure.foreach{
        a =>
        log.info("Sent START message to: "+a.toString)
        context.actorSelection(a) ! SenseFlow (keyHosts)
      }

  }

  /*
   *  Monitors until Function Provisioner is Ready to provision q assets
   */
  
  def monitorRegistration(q:Int) = {
    //log.debug("reqRegistrations=>"+reqRegistrations+" --- functionProv => "+ FunctionProvisioner)
    log.debug("FP==>"+functionProvisioner+"<==")
    if(functionProvisioner == ActorRef.noSender ||
      functionProvisioner == null
    ){ // Wait until one function provisioner is ready
      system.scheduler.scheduleOnce(3 seconds, self, MonitorRegistration(q))
    }else
       functionProvisioner ! NewInfrastructure(q)
  }

}
