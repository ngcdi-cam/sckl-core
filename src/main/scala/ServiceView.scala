package org.ngcdi.sckl

import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.StatisticsUtils._
import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.Config._

import scala.concurrent.duration._

import java.time.{ZoneOffset,LocalDateTime, Instant}

trait ServiceView {

  this: ScklActor with AnomalyDetector =>

//  import context._ //imports system

  var nRuns = 0 // a.k.a. tick
  //var restClient:ActorRef = _

  var notificationSent = false
  val soi = obtainServicesOfInterest
  val slas = obtainReferenceSLAs
  var aggregatedView:Seq[Measurement] = Seq.empty
  var lastReportTo:Instant =
    Instant.now()


  def serviceViewPreStart(): Unit = {
   // restClient = context.actorOf(RESTClient.props(oRerouteUrl), name = "restClient")
    anomalyDetectorPreStart()
    triggerReporting()
    log.debug("FINISHED SV PRE-START!!!!")
  }


  def svBehaviour: Receive = {

    case AggregateLocalView(measurements:Seq[Measurement]) =>
      countMsg("scklmsg")
      doAggregateLocalViews(measurements)
    case ReportService =>
      countMsg("scklmsg")
      doReportService()
  }

  def doAggregateLocalViews(measurements:Seq[Measurement])={
    val moi = measurements.foldLeft[Seq[Measurement]](Seq.empty){
      (ac,m)=>
      if(
        soi
          .filter(_._2 == m.neId)
          .filter(_._3 == m.resourceId)
          .size > 0
      )
        ac :+ m
      else
        ac
    }.collect{
      case mf:Measurement => mf
    }

    log.debug("Received from "+ measurements.head.neId + ": "+measurements.size+" Measurements")
    log.info("Received from "+ measurements.head.neId + ": "+measurements)
    log.debug("Filtered Measurements: " + moi)

    aggregatedView = aggregatedView ++ moi

    log.debug("Aggregatedview: " +aggregatedView)

  }


  def doReportService() ={
    if(!aggregatedView.isEmpty){

      val from = lastReportTo
      val to = Instant.now()
       log.debug("window from: "+LocalDateTime.ofInstant(from,ZoneOffset.UTC).format(formatterTime) +" - to:"+LocalDateTime.ofInstant(to,ZoneOffset.UTC).format(formatterTime))
      log.debug("window from: "+from.getEpochSecond+" - "+from.getNano+" - to:"+to.getEpochSecond+" - "+to.getNano)

    //monitorWindowSLAsIntentsNode(from,to)
      monitorWindowSLAs(from,to)
      lastReportTo = to

    }
  }


  def obtainServicesOfInterest():Seq[Tuple3[String,String,String]] = {
    log.debug("key_services:"+keyServices)
    //val ks = keyServices
    //    .foldLeft[Seq[Tuple3[String,String,String]]](Seq.empty){
    //      (ac,v) =>
    //      ac ++ servicePaths
    //        .filter(_._1 == v)
     //   }.collect{
     //     case r:Tuple3[String,String,String] => r
     //   }
    val ks = servicePaths
    log.debug("key_servicePaths:"+ks)
    ks
  }

  def obtainReferenceSLAs():Seq[ServiceLevel] = {
    val slas = Seq(
      ServiceLevel(1,200000,300000,100000,"bandwidth")
        ,ServiceLevel(1,200000,300000,100000,Temperature)
        ,ServiceLevel(1,200000,300000,100000,OPT_POW)
        ,ServiceLevel(1,200000,300000,100000,CPU_USG)
        ,ServiceLevel(1,200000,300000,100000,RAM_USG)
        ,ServiceLevel(1,200000,300000,100000,SYS_ERR)
        //,ServiceLevel(1,0.9,1,0.05,"availability")
    )
    slas
  }


  def getSample(from:Instant,to:Instant,currentView:Seq[Measurement],metricName:String):Seq[Measurement] = {
    currentView
      .filter(_.metricName == metricName)
      .filter(
        _.seconds < to.getEpochSecond
      )
      .filter(
        _.seconds >= from.getEpochSecond
      )
      .collect{
        case m:Measurement =>m
      }
  }


  /*
   *  Monitors SLAs without Intents
   */

  def monitorWindowSLAs(from:Instant,to:Instant):Unit = {
    nRuns = nRuns + 1
    log.debug("ticks==>"+nRuns)
    slas.foreach{
      sc =>

      val a = sc.metricName

      val sample = getSample(from,to,aggregatedView.toSeq,sc.metricName)

      if (sample.size >0){
        //Process per device
        runAnomalyDetection(nRuns,sample,a,sc.tolerance)
        calculatePerService(sample,a,"s1")
      }
    }
  }

  def calculatePerService(sample:Seq[Measurement], metricName:String, serviceLabel:String):Unit={
    val smean = calculateMean(
      sample.map(_.value).collect{case v:Double => v})


    val ssd = calculateSD(
      sample.map(_.value).collect{case v:Double => v})

    publish("ngcdi.service.mean",metricName,"flow",serviceLabel,smean)
    publish("ngcdi.service.sd",metricName,"flow",serviceLabel,ssd)

  }

  def triggerReporting()={
    system.scheduler.schedule(initialServiceReportDelay, frequencyServiceReport seconds, self, ReportService)
  }


  def getConditionOnlyView():Seq[Measurement]={
    aggregatedView
      .filter{
        m => conditionKPIs contains(m.metricName)
      }
  }

}
