package org.ngcdi.sckl

import akka.actor._
import msgs._
//import akka.cluster.Cluster
//import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.ClusterEvent.MemberUp
//import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.Member
//import akka.cluster.MemberStatus
import scala.concurrent.duration._

import ClusteringConfig._

class FunctionProvisioner extends ScklActor
//    with Clustering
{

//  import context._
  import Constants._

  //val mediator = DistributedPubSub(context.system).mediator
  var assets:List[(String,ActorRef,Boolean)] = List()


  var provisionedAssets = 0

 // override def getInterestedRoleName():String = {
 //   return org.ngcdi.sckl.Constants.digitalAssetName
 // }
 // override def registerInterestedRole(ref:ActorRef):Unit = {
 //   val daAddr = ref.path.address.toString
 //   val daName = daAddr.substring(daAddr.indexOf("@")+1, daAddr.lastIndexOf(":"))
 ////   assets = assets :+ (daName,ref, false)
   //  log.debug("Recorded DA: "+ daName)
  //}

  override def preStart(): Unit = {

   // clusteringPreStart()
  }


  def receive = {
    fpBehaviour
      //.orElse[Any,Unit](clusteringBehavior)
      .orElse[Any,Unit](scklBehaviour)
  }

  val fpBehaviour: Receive = {
    case NewInfrastructure(q:Int)=>
      countMsg("scklmsg")
      val sm = sender().path.toSerializationFormat
      log.info("SM Requesting infrastructure: -->"+sm+"<--")
      self ! Provision(q, sm)

    case Provision (q:Int, sm:String) =>
      countMsg("scklmsg")
      //log.debug("Current assets:"+assets.size)

      var infrastructure:Array[String] = new Array[String](q)

     // val inactiveAssets =
     //   assets
     //     .filter(!_._3)
     //     .sortBy(_._2.path.toString)

      //log.info("No. of Inactive assets: "+inactiveAssets.size)

      // if( inactiveAssets.size < q){
      if( assets.size < q){
        system.scheduler.scheduleOnce(5 seconds,self ,Provision(q,sm) )
      }else{
        log.info("Provisioning New infrastructure")
        for(i<-0 until q){
          //assets = assets.filterNot(_ == inactiveAssets(i))
          //assets = assets :+ (inactiveAssets(i)._1,inactiveAssets(i)._2,true)
          //infrastructure(i)= inactiveAssets(i)._2.path.toSerializationFormat
          infrastructure(i) = assets(i)._2.path.toSerializationFormat
        }

        infrastructure.foreach{
          x=>log.info("Infrastructure is ready: "+ x)
        }

        //inactiveAssets.foreach{
        //  x=>log.info("Inactive assets: "+ x)
        //}

        log.debug("Sending to: -->"+sm+"<--")

        context.actorSelection(sm ) ! InfrastructureReady(infrastructure)


      }

    case ReplaceFunction(af:String,sm:String) =>
      countMsg("scklmsg")
      log.info("REPLACEMENT!!!! New function requested because of "+af)
      self ! Provision(1,sm)
      self ! FunctionDisposal(af)

    case FunctionDisposal(af) =>
      countMsg("scklmsg")
      log.debug("Function disposal "+ af)

      val toDispose = assets
        .filter(_._1==af)
      .head

      assets = assets.filterNot(_ == toDispose)
      log.info("New asset list size:"+ assets.size)
      toDispose._2 ! StopMessage(af)

    case DARegistration(assetName:String) =>
      countMsg("scklmsg")
      assets = assets :+ (assetName,sender,true)

    case FPReady =>
      countMsg("scklmsg")
      system.scheduler.scheduleOnce( 30 seconds, self, FPReady)
      log.info("Provisioner Listening...")

      //val inactiveAssets =
      //  assets
      //    .filter(!_._3)
      //    .sortBy(_._2.path.toString)

      //log.info("No. of Inactive assets: "+inactiveAssets.size)


  }


}
