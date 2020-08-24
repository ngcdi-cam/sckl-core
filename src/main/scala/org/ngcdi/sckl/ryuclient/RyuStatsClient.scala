package org.ngcdi.sckl.ryuclient

import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.unmarshalling._
import spray.json.DefaultJsonProtocol._
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import scala.concurrent.Future

class RyuStatsClient(baseUrl: String)
    extends SimpleRestClient(baseUrl) {

  implicit val matchRuleFormat = jsonFormat5(RyuMatchRule)
  implicit val flowStatFormat = jsonFormat5(RyuFlowStat)
  implicit val portStatFormat = jsonFormat4(RyuPortStat)

//   final def getFlowStats(implicit
//       ec: ExecutionContext,
//       actorSystem: ActorSystem
//   ): Future[Seq[RyuFlowStat]] = {}

}
