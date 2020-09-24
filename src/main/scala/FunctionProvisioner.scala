package org.ngcdi.sckl

import akka.actor._
import msgs._
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.Member
import scala.concurrent.duration._

import ClusteringConfig._
import org.ngcdi.sckl.behaviour.awareness.AwarenessManagerSenderBehaviour

class FunctionProvisioner
    extends AwarenessManagerSenderBehaviour
    {

  import Constants._

  var assets: List[(String, ActorRef, Boolean)] = List()

  var provisionedAssets = 0

  def receive = {
    fpBehaviour
      .orElse[Any, Unit](scklBehaviour)
  }

  val fpBehaviour: Receive = {
    case NewInfrastructure(q: Int) =>
      countMsg("scklmsg")
      val sm = sender().path.toSerializationFormat
      log.info("SM Requesting infrastructure: -->" + sm + "<--")
      self ! Provision(q, sm)

    case Provision(q: Int, sm: String) =>
      countMsg("scklmsg")
      var infrastructure: Array[String] = new Array[String](q)

      if (assets.size < q) {
        system.scheduler.scheduleOnce(5 seconds, self, Provision(q, sm))
      } else {
        log.info("Provisioning New infrastructure")
        for (i <- 0 until q) {
          infrastructure(i) = assets(i)._2.path.toSerializationFormat
        }

        infrastructure.foreach { x =>
          log.info("Infrastructure is ready: " + x)
        }

        log.debug("Sending to: -->" + sm + "<--")
        val serviceManager = context.actorSelection(sm)
        serviceManager ! InfrastructureReady(infrastructure)
        setupAwarenessManager(serviceManager)
      }

    case ReplaceFunction(af: String, sm: String) =>
      countMsg("scklmsg")
      log.info("REPLACEMENT!!!! New function requested because of " + af)
      self ! Provision(1, sm)
      self ! FunctionDisposal(af)

    case FunctionDisposal(af) =>
      countMsg("scklmsg")
      log.debug("Function disposal " + af)

      val toDispose = assets
        .filter(_._1 == af)
        .head

      assets = assets.filterNot(_ == toDispose)
      log.info("New asset list size:" + assets.size)
      toDispose._2 ! StopMessage(af)

    case DARegistration(assetName: String) =>
      countMsg("scklmsg")
      assets = assets :+ (assetName, sender, true)
      setupAwarenessManager(sender)

    case FPReady =>
      countMsg("scklmsg")
      system.scheduler.scheduleOnce(30 seconds, self, FPReady)
      log.info("Provisioner Listening...")

  }

}
