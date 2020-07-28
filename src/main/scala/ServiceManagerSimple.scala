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
            with IntentServiceView
        )
      //ServiceManager using openflow API (Ryu )
      case _ =>
        Props(new ServiceManagerSimple()
        )
    }


  }
}

class ServiceManagerSimple() extends ScklActor
//with Clustering   //Trait that uses akka clustering framework for communication with other actors/agents
    with Remoting //Trait for connection with other actors/agents
    with ServiceView //Trait for generation of the service view
    with GroupAnomalyDetector  //Trait for detection of anomalies
    with RESTServiceActuator //Trait for trigger actions using REST API
    with PredictiveAnalytics // Trait for using data collected to make predictions
    with WebServer // Trait that expose a web server to enable servicing REST request
    {


  var reqRegistrations:Int = 0  //Counter of how many DigitalAsset (DA) agents is expecting to get connected.
  var addresses:Seq[String] = Seq.empty //Addresses of DA agents that report to this ServiceManager (SM)

  /*
  * Prestarts all the behaviours from the traits
  */
  override def preStart(): Unit = {
    // clusteringPreStart()
    connPreStart()
    serviceViewPreStart()
    actuatorPreStart()
    predictivePreStart()
  }


  def receive = {
    smBehaviour  //Specific SM behaviour defined in this class
      .orElse[Any,Unit](connBehaviour)  //connection behehaviour defined in Remoting or Clustering trait
      .orElse[Any,Unit](svBehaviour) // for generating the view of the service from DA readings in traits ending in View
      .orElse[Any,Unit](anomalyHandlerBehaviour) // for handling anomalies from traits ending in AnomalyDetector
      .orElse[Any,Unit](predictiveBehaviour) // from PredictiveAnalytics trait
      .orElse[Any,Unit](actBehaviour) // from traits ending in Actuator
      .orElse[Any,Unit](scklBehaviour) // Parent behaviour at last. This comes from ScklActor
  }


  val smBehaviour: Receive = {

    //Start message to indicate that SM is running and requires q Digital Assets
    case SMReady(q) =>
      countMsg("scklmsg")
      self ! MonitorRegistration(q)
      //triggerAction("0",Option(0),Seq.empty)
      log.info("Service Manager Ready! ("+q+")")

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
