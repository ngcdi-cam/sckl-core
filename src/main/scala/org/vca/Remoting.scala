package org.vca

import akka.actor.{Actor, ActorLogging, ActorRef, RootActorPath, ActorSelection}
import akka.util.Timeout

import scala.concurrent.duration._

import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.ClusteringConfig._
import org.ngcdi.sckl.ScklActor

//final case class RegisterRDA()

trait Remoting extends ConnectionBehaviour {
  this: ScklActor =>
  var functionProvisioner: ActorSelection =
    _ // In this case is like a directory
  //implicit val timeout = Timeout(5 seconds)

  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def connPreStart(): Unit = {
    val fpName = "prov1"
    val fpPort = 1600
    //val url = "akka.tcp://"+clusterName+"@"+fpName+":"+fpPort+"/user/"+functionProvisionerName  //dl4j
    val url =
      "akka://" + clusterName + "@" + fpName + ":" + fpPort + "/user/" + functionProvisionerName
    log.info("Locating: =>" + url + "<=")
    functionProvisioner = context.actorSelection(url)
    log.info("Remoting prestart: " + nodeIp)
    nodeIp match {
      case s: String if s.startsWith("c") =>
        functionProvisioner ! DARegistration(nodeIp)
        log.info("registration sent to:" + functionProvisioner)
      case _ =>
    }

    log.debug("FINISHED Remoting PRE-START!!!!")
  }
}
