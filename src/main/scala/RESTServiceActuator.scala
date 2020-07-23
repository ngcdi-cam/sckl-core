package org.ngcdi.sckl

import org.ngcdi.sckl.Config._
import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.sdn._
import org.ngcdi.sckl.msgs._
import org.vca._
import scala.concurrent.duration._

import akka.actor._



trait RESTServiceActuator extends Actuator with IntentParser{
  this: ScklActor with PredictiveAnalytics with ServiceView =>

  var restClientAct:ActorRef = _
  var restUIClient:ActorRef = _
  var pendingReroutes:Seq[Tuple2[String,Seq[String]]]=Seq.empty
  var nPendingReroutes:Int = 0
  var pendingAssetsMaintain:Seq[String]=Seq.empty

  def actuatorPreStart():Unit={
    restClientAct = context.actorOf(RESTClient.props(netwServer,netwPort), name = "restClientAct")
    restUIClient = context.actorOf(RESTClient.props(uiServer,uiPort), name = "restUIclient")
    doGetCurrentRoutes()
    //self ! ActivatePredictiveAnalytics
  }

  override def triggerAction(deviceId:String,resultAD:Option[Int],details:Seq[String])={
    val newContentProvider = "h5"
    val actionUrl = "/nodes"+"/"+newContentProvider+"/cmd"
    val client = "h2"
    val data = "iperf -u -t 300 -c "+client+" -b 50000000"

    restClientAct ! PostRequest(actionUrl,data,true,"")
  }


  def actBehaviour:Receive = {

    case ProcessResponsePost(result:String) =>
      countMsg("scklmsg")
      processResponse(result)

    case GetCurrentRoutes()=>
      doGetCurrentRoutes()
    case ToConfirmMaintenanceP(assetsToMaintain:Seq[Tuple2[String,Double]])=>
      doToConfirmMaintenaceP(assetsToMaintain)
    case DoAlternativeRoutes(routes:Tuple2[String,Option[Seq[Seq[String]]]])=>
      doAlternativeRoutes(routes)
    case DoResultReroute(result:Tuple2[String,Boolean])=>
      doResultReroute(result)
    case DoConfirmedMaintenanceP(assetsToMaintain:Seq[Tuple2[String,String]]) =>
      doConfirmedMaintenanceP(assetsToMaintain)
    case TriggerAssetPreparation =>
      log.debug("pending assets maintain===>"+pendingAssetsMaintain)
      self ! FindIntentsForAsset(pendingAssetsMaintain)
    case ActivatePredictiveAnalytics =>
      system.scheduler.schedule(10 seconds, 30 seconds, self, RunPredictiveModel)
      doConfirmActivation()
  }


  def doConfirmActivation()={
    log.debug("Confirming Activation...")
      val msg =
        "{\"text\":\"Predictive Maintenance Started..\"}"
      log.debug("Confirmation msg:"+msg)

      val query = slkChannelURL

      restUIClient ! PostExtraRequest(query,msg,Seq.empty,false,"")
      pendingReroutes= Seq.empty
      nPendingReroutes = 0

  }

  def doGetCurrentRoutes()={
    restClientAct ! SingleGetRequest("/api/get_intents",true)
  }

  def doResultReroute(result:Tuple2[String,Boolean])={
    log.info("Processing DoResultRoutes..")
    if(result._2){
      log.info("Intents rerouted successfully=>"+result._1)

      val namedAssets = pendingAssetsMaintain.collect{case a=>switches(a)}

      val msg =
        "{\"text\":\"Traffic reroute to avoid assets: [*"+namedAssets.mkString(",")+"*], "+nPendingReroutes+" flows were rerouted *based on asset condition*.\"}"
      log.info("Confirmation msg:"+msg)

      val query = slkChannelURL

      restUIClient ! PostExtraRequest(query,msg,Seq.empty,false,"")
      pendingReroutes= Seq.empty
      nPendingReroutes = 0

    }else{
       log.info("Intents reroute did not work=>"+result._1)
      self ! ConfirmResultReroute("failed")
    }
  }

  def doAlternativeRoutes(routes:Tuple2[String,Option[Seq[Seq[String]]]])={
    log.debug("Processing DoAlternativeRoutes..")
    routes._2 match {
      case Some(r)=>
        val sar = selectAlternativeRoute(routes, pendingAssetsMaintain, aggregatedView,conditionCriteria)
        log.debug("Best reoute is===>"+sar)
        pendingReroutes = pendingReroutes :+ sar
        log.debug("Resulting pending reroutes is===>"+pendingReroutes)
        if(pendingReroutes.size == nPendingReroutes)
          triggerReroute()
      case None=>
        nPendingReroutes = nPendingReroutes - 1
    }
    log.debug("pending routes:"+pendingReroutes.size+" -- "+nPendingReroutes)
    if(pendingReroutes.size == nPendingReroutes)
      triggerReroute()
  }

  def prepareReroute(intentsToReroute:Seq[String])={
    val query = "/api/get_routes"
    //log.info("Existing intents ==>"+intentRoutes.size)
    log.info("No. Intents To Reroute ==>"+intentsToReroute.size)
    log.info("Intents To Reroute ==>"+intentsToReroute)
    pendingReroutes = Seq.empty
    nPendingReroutes = intentsToReroute.size
    //nPendingReroutes = 1

    intentsToReroute.map{
      i =>
      val jsonIntent =
        parse(i)
      //parse("00:00:00:00:00:02/None00:00:00:00:00:08/None")
      restClientAct ! PostRequest(query,jsonIntent,false,"")
      log.info("Getting alternative routes to reroute intent==>"+jsonIntent)
    }
  }



  def triggerReroute()={
    val query = "/api/push_intent"
    val jsonIntent =
      parse(pendingReroutes)
      /*parse(Seq(
        Tuple2(
          "00:00:00:00:00:01/None00:00:00:00:00:07/None",
          Seq(
            "00:00:00:00:00:01/None",
            "of:0000000000000001",
            "of:0000000000000007",
            "of:000000000000000c",
            "00:00:00:00:00:07/None"
          )
        ),

      ))*/
    log.info("Pending intents to reroute==>"+pendingReroutes)
    restClientAct ! PostRequest(query,jsonIntent,false,"")
    log.info("Triggering reroute intent==>"+jsonIntent)
  }



  def doToConfirmMaintenaceP(assets:Seq[Tuple2[String,Double]]):Unit={
    log.debug("Sending request for confirmation..."+assets)
    val query = slkChannelURL
    val namedAssets = assets.collect{case a=>switches(a._1)}

    val data =
      "{\"text\":\"We require to *re-plan maintenance* for assets: [*"+namedAssets.mkString(",")+"*]. These are _predicted_ to fail in less than *8* days!. Please confirm? \", \"type\":\"mrkdwn\"}"

    val tokenHeader = "" //Token to include in the header

    val onlyAssets:Seq[String] =
      assets
      .collect{case a => a._1}

    pendingAssetsMaintain = onlyAssets
    restUIClient ! PostExtraRequest(query,data,onlyAssets,false,tokenHeader)
  }


  /*
   * Once user has confirmed it triggers reroute
   */

  def doConfirmedMaintenanceP(assetsToMaintain:Seq[Tuple2[String,String]])={
    log.info("Ready to prepare asset for maintenance..."+assetsToMaintain)
    prepareReroute(
      assetsToMaintain
        .map(
          assetIntent =>
          assetIntent._2 //Only the intent
        )
        .distinct //Unique values
        .map(
          intentToReroute =>
          //log.info("TO prepare reroute of intent: "+intentToReroute)
          intentToReroute
        )
    )
  }

    def processResponse(result:String)={
    countMsg("scklmsg")
    log.info("Content provided")
  }
}
