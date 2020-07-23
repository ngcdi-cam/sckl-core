package org.ngcdi.sckl

import akka.actor.{Actor,ActorLogging,ActorRef, RootActorPath}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.Member
import akka.cluster.MemberStatus
import akka.cluster.pubsub.DistributedPubSub
import akka.util.Timeout

import scala.concurrent.duration._

import org.ngcdi.sckl.msgs._
import org.vca._

trait Clustering extends ConnectionBehaviour{
  this: ScklActor  =>
  val cluster = Cluster(context.system)
  //implicit val timeout = Timeout(5 seconds)

  var functionProvisioner:ActorRef = ActorRef.noSender

  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def connPreStart(): Unit = {
    cluster.subscribe(self, classOf[MemberUp])
    log.debug("FINISHED Clustering PRE-START!!!!")
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  override val connBehaviour: Receive = {

    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register

    case MemberUp(m) =>
      log.debug("Has detected new Cluster Member--->"+m+"<----")
      register(m)

    case MemberDowned(m) =>
      log.debug("MEMBER REMOVED!!!!!!!!!!!!!!!!!!!!!!!!!!=====>"+m)
      //aggregatedView.filterNot(_.ne_id==m.)

    case MemberLeft(m) =>
      log.debug("MEMBER LEFT!!!!!!!!!!!!!!!!!!!!!!!!!!=====>"+m)
      //aggregatedView.filterNot(_.ne_id==m.)

    case FunctionProvisionerRegistration =>
      log.debug("FunctionProvision: Received registration request")
      context watch sender()
      functionProvisioner = sender()
  }


  def register(member: Member): Unit ={

    //import scala.concurrent.ExecutionContext.Implicits.global

    if (member.hasRole(getInterestedRoleName)){

      for(res <- context.actorSelection(RootActorPath(member.address) / "user" / getInterestedRoleName).resolveOne(5 seconds)){
        registerInterestedRole(res)
      }
    }

  }

  def getInterestedRoleName():String
  def registerInterestedRole(ref:ActorRef):Unit

}
