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

trait Neighbouring{
  this: ScklActor  =>

  //import context.dispatcher

  var ngbs:Seq[Tuple2[String,ActorRef]] = Seq.empty[Tuple2[String,ActorRef]]
  var doneNgbs:Seq[String] = Seq.empty[String]
  def locateNeighbours(): Unit = {

    ngbNames.foreach{
      n =>
      //countMsg("scklmsg")
      if(!doneNgbs.contains(n)){
        doneNgbs = doneNgbs :+ n
        //val notRecorded = ngbs
        //  .filter(_._1 == n)
        //  .collect{
         //   case f:Tuple2[String,ActorRef] =>
         //     f._2
         // }.toSeq.isEmpty

        //if(notRecorded){
          //log.debug("My name: "+nodeIp+" Node name:"+n)
          if( n != nodeIp ) {
            //val ngbFuture = context.system.actorSelection("akka.tcp://"+clusterName+"@"+n+":"+nodePort+"/user/"+digitalAssetName).resolveOne()(timeout)
            val ngbFuture = context.system.actorSelection("akka://"+clusterName+"@"+n+":"+nodePort+"/user/"+digitalAssetName).resolveOne()(timeout)
            ngbFuture.onComplete {
              case Success(r)=>
                ngbs = ngbs :+ Tuple2(n,r)
                log.info("Got_Neighbour! :"+n)
              case Failure(e)=>
                doneNgbs = doneNgbs.filterNot(_==n).collect{case x:String => x}.toSeq
                log.info("Neighbour Not Available: "+e.getMessage+"==>"+doneNgbs)
            }
          }else
             ngbs = ngbs :+ Tuple2(n,self)
        //}
      }


    }
    log.info("donengbs==>"+doneNgbs+"==>"+ngbs+"<=")
    if(ngbs.size > 7)
      self ! SenseFlow(keyHosts)
    else
      context.system.scheduler.scheduleOnce( 10 seconds,  self, LocateNeighbours)
      log.info("Locating Neighbours for: "+nodeIp)
  }

  def neighbouringBehaviour:Receive ={

    case LocateNeighbours =>
      countMsg("scklmsg")
      locateNeighbours()
  }


}
