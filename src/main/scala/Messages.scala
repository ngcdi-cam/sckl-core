package org.ngcdi.sckl.msgs

import akka.actor.ActorRef
import akka.actor.ActorPath
import org.ngcdi.sckl.model._
import akka.util.ByteString
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._

//import org.ngcdi.sckl.Measurement

sealed trait DigitalAssetMessage extends Serializable


object LocalProcessor {

  final case class Save (m:Measurement)
  object Consolidate
  object Consolidated

}

//object ServiceManager {
object EndReasoning
object ReportService
case class SMReady(q:Int)
case class MonitorRegistration(q:Int)

//}

object FPReady

case object StartMessage
//case class StopMessage (path:String) extends DigitalAssetMessage
//final case class AggregateLocalView(lvw:Seq[Measurement]) extends DigitalAssetMessage
final case class AgregationResult(text: String)
final case class AggregationFailed(reason: String, aggregation: AggregateLocalView)
final case class Provision(q:Int,sm:String)
/*final case class SMRegistration(s:String) extends DigitalAssetMessage{
  def toByteArray(){

  }
}*/
case object FunctionProvisionerRegistration
//case class DARegistration (assetName:String) extends DigitalAssetMessage

case class Ready (lvw:Seq[Measurement])
case object CompletedStream
case object Ack
//case class ReplaceFunction(af:String)
//case class FunctionDisposal(af:String)

object Out

case object TriggerMonitoring
case object MeasurementFilter




//final case class NotReadyYet(localView: ActorRef) extends DigitalAssetMessage
final case class NotReadyYet(measurement: Measurement) extends DigitalAssetMessage
//object Ready extends DigitalAssetMessage
final case class Record(digitalAsset: ActorRef, m:Measurement) extends DigitalAssetMessage
final case class Send(digitalAsset: ActorRef) extends DigitalAssetMessage
final case class Sent(digitalAsset: ActorRef) extends DigitalAssetMessage
final case class Report(cView:Seq[Measurement]) extends DigitalAssetMessage
//object Sense extends Serializable
//case class Sense(digiAsset:ActorRef) extends DigitalAssetMessage
case class Link(linkFileName:String)
case class Start(smp:ActorPath, lp:ActorPath) // LocalView: receiving SM path and LP path
// SimAsset messages

object Start
object Stop
object Consolidate

final case class Normalise (measurements:Any)

object CheckNextAction
case class RESTRequest(node:String)
object PullREST
object ReSense
final case class ThroughputOutREST(node:String,previous:Seq[Tuple3[String,Long,Long]], lviewPath:ActorPath)
final case class ThroughputFlow(node:String,keyHosts:Seq[String],previous:Seq[FlowStat], lviewPath:ActorPath)
final case class UpdateThroughput(newTpt:Seq[Tuple3[String,Long,Long]])
final case class UpdateThroughputFlow(newTpt:Seq[FlowStat])
final case class ReRouteIntents(deviceId:String)



//RConnection
final case class InitStateAnomD(labelAD:String)
final case class StateAnomDInitd(labelAD:String)
final case class UpdateAnomD(tick:Int,labelAD:String,deviceId:String,intent:String,newData:Seq[Double])
final case class ResultAnomD(tick:Int, labelAD:String, deviceID:String,intent:String,resultAD:Option[Int])


//Slack

final case class NotifyEngTeam(text:String)
object LocateNeighbours

//REST

final case class ParseResponseBody(request:String,body:ByteString,data:String)
final case class ParseResponseBodyExtra(request:String,body:ByteString,data:String,dataExtra:Seq[String])
final case class ParseResponse(request:String,entity:HttpEntity)
final case class ProcessResponsePost(result:String)

final case class NewMeasurements(newMeasurements:Seq[Model])

final case class GetCurrentRoutes()
final case class UpdateRoutes(routes:Seq[Model])
final case class DoAlternativeRoutes(routes:Tuple2[String,Option[Seq[Seq[String]]]])
final case class DoResultReroute(result:Tuple2[String,Boolean])

//Predictive Maintenance

final case class ToConfirmMaintenanceP(assetsToMaintain:Seq[Tuple2[String,Double]])

//final case class RunPredictiveModel(assets:Seq[Tuple2[String,Seq[Double]]])
case object RunPredictiveModel
final case class DoConfirmedMaintenanceP(assetsIntents:Seq[Tuple2[String,String]])
case object TriggerAssetPreparation
case object ActivatePredictiveAnalytics
final case class ConfirmResultReroute(result:String)
final case class FindIntentsForAsset(assets:Seq[String])
