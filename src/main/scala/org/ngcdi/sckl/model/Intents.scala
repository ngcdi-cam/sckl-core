package org.ngcdi.sckl.model

import spray.json._

//import akka.actor.{ Actor, ActorLogging, Props }

final case class Intent(key: String, inElements: List[String], outElements:List[String])
final case class IntentMetric(id: String,bytes:Long,life:Long,deviceId:String)
final case class IntentMetricModel(id: String,bytes:Long,life:Long,deviceId:String,intentId:String) extends Model

//final case class FlowStatsValue(byte_count:Long,duration_sec:Long,deviceId:String)
//final case class IntentMetric(id:String,bytes:Int)

//final case class IntentMetric(
//  id: String,
//  tableId: String,
//  appId:String,
//  groupId:Int,
//  priority:Int,
//  timeout:Int,
//  isPermanent:Boolean,
//  deviceId:String,
//  state:String,
  //life:Int,
//  packets:Long,
//  bytes:Int
 // , liveType:String
//  , lastSeen:Long
//)

final case class IntentMetrics(intentMetrics: Seq[IntentMetric])

//final case class IStatistics(istatistics:Map[String,Seq[IntentMetric]])


final case class IntentMetricKey(value: String) extends AnyVal
final case class IntentMetricValue(value: IntentMetric) extends AnyVal

//final case class IntentMetricEntries(value: Map[IntentMetricKey,String]) extends AnyVal

final case class ResponseStatistic(id:Int,name:String,intents:Seq[Map[IntentMetricKey,Set[IntentMetricValue]]])

final case class ResponseStatistics(statistics:Seq[ResponseStatistic])

final case class Path (path:Seq[String],weight:Double)

final case class Paths(paths: Seq[Path])

final case class IntentPath (paths:Seq[Path],key:String,appId:OnosApp) extends Model

final case class IntentRoutes(routingList:Seq[IntentPath])

final case class OnosApp (id:Int,name:String)

//Network Controller Statistics
final case class NCStatistic(id:Int,name:String,intents:Seq[Map[IntentMetricKey,Set[IntentMetricValue]]])

final case class NCStatistics(statistics:Seq[NCStatistic])

final case class AlternativeRoute(routes:Map[RouteKey,RouteValue],key:String,num_routes:String) extends Model
//final case class RouteMap(routeKey:RouteKey,routeValue:RouteValue) extends Model
final case class RouteKey(value: String) extends AnyVal
final case class RouteValue(value: Seq[String]) extends AnyVal
final case class RouteEntry(key:RouteKey, route:RouteValue) extends Model

final case class PushIntentRequest(api_key:String,routes:Seq[RouteEntry])
final case class GetRoutesRequest(api_key:String,key:String)
final case class SimpleResult(success:Boolean)

//final case class RequestIntentRoutes(key:String) extends Model

//object IntentsActor {
//  final case class IntentMetric(intent: String)
//}
/*
class UserRegistryActor extends Actor with ActorLogging {
  import UserRegistryActor._

  var users = Set.empty[User]

  def receive: Receive = {
    case GetUsers =>
      sender() ! Users(users.toSeq)
    case CreateUser(user) =>
      users += user
      sender() ! ActionPerformed(s"User ${user.name} created.")
    case GetUser(name) =>
      sender() ! users.find(_.name == name)
    case DeleteUser(name) =>
      users.find(_.name == name) foreach { user => users -= user }
      sender() ! ActionPerformed(s"User ${name} deleted.")
  }
}
 */
//#user-registry-actor

final case class Notification(text:String)
