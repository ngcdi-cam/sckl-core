package org.ngcdi.sckl

import akka.actor.{Actor,ActorLogging,ActorRef, RootActorPath, ActorSelection}
import akka.util.Timeout
import scala.concurrent._
import scala.util.{ Failure, Success }
import scala.concurrent.duration._

import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.ClusteringConfig._
import org.ngcdi.sckl.Config._

//final case class RegisterRDA()

//object LocateNeighbours

trait RingNeighbouring{
  this: ScklActor  =>

  //val timeoutd = 10 seconds
  //implicit val timeout = Timeout(timeoutd)
  //import context.dispatcher

  var leftNgb:ActorRef = _
  var rightNgb:ActorRef = _
  var leftOk = false
  var rightOk = false


  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart


  def locateNeighbours(): Unit = {


    if(!leftOk){
      //val leftNgbFuture = context.system.actorSelection("akka.tcp://"+clusterName+"@"+leftNgbName+":"+nodePort+"/user/"+digitalAssetName).resolveOne()(timeout)
      val leftNgbFuture = context.system.actorSelection("akka://"+clusterName+"@"+leftNgbName+":"+nodePort+"/user/"+digitalAssetName).resolveOne()(timeout)
      leftNgbFuture.onComplete {
        case Success(r)=>
          leftNgb = r
          leftOk = true
          log.info("Got Left Neighbour!")
        case Failure(e)=>
          log.info("Left Neighbour Not Available: "+e.getMessage)
      }
    }

    if(!rightOk){
      //val rightNgbFuture = context.system.actorSelection("akka.tcp://"+clusterName+"@"+rightNgbName+":"+nodePort+"/user/"+digitalAssetName).resolveOne()(timeout)
      val rightNgbFuture = context.system.actorSelection("akka://"+clusterName+"@"+rightNgbName+":"+nodePort+"/user/"+digitalAssetName).resolveOne()(timeout)
      rightNgbFuture.onComplete {
        case Success(r)=>
          rightNgb = r
          rightOk = true
          log.info("Got Right Neighbour!")
        case Failure(e)=>
          log.info("Right Neighbour Not Available: "+e.getMessage)
      }
    }

    if(leftOk && rightOk)
      self ! SenseFlow(keyHosts)
    else
      context.system.scheduler.scheduleOnce( 10 seconds,  self, LocateNeighbours)
    log.info("Locating Ring Neighbours: "+nodeIp)
  }

  def ringNBehaviour:Receive ={

    case LocateNeighbours =>
      countMsg("scklmsg")
      locateNeighbours()
  }


}
