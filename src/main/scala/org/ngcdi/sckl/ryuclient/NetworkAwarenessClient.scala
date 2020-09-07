package org.ngcdi.sckl.ryuclient

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.unmarshalling._
import spray.json.DefaultJsonProtocol._
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import scala.concurrent.Future
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.RequestEntity
// import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
// import spray.json.DefaultJsonProtocol

trait NetworkAwarenessJsonSupport {
  @transient lazy implicit val statEntryFormat = jsonFormat5(
    NetworkAwarenessStatEntry
  );
  @transient lazy implicit val flowEntryFormat = jsonFormat9(
    NetworkAwarenessFlowEntry
  );
  @transient lazy implicit val serviceFormat = jsonFormat4(
    NetworkAwarenessService
  )
  @transient lazy implicit val accessTableEntryFormat = jsonFormat4(
    NetworkAwarenessAccessTableEntry
  )
  @transient lazy implicit val linkFormat = jsonFormat4(NetworkAwarenessLink)
}

class NetworkAwarenessClient(baseUrl: String)
    extends SimpleRestClient(baseUrl)
    with Serializable
    with NetworkAwarenessJsonSupport {

  final def getBaseUrl(): String = baseUrl

  final def getStats(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[NetworkAwarenessStatEntry]] = {
    for {
      httpResponse <- httpGet("/awareness/stats")
      statsOrig <-
        Unmarshal(httpResponse).to[Map[String, Seq[NetworkAwarenessStatEntry]]]
      stats <- Future { statsOrig.get("graph").get.toSeq }
    } yield stats
  }

  final def getSwitchStats(dpid: Int, filter: Boolean = false)(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[NetworkAwarenessStatEntry]] = {
    for {
      httpResponse <- httpGet(
        s"/awareness/stats/$dpid" + (if (filter) "?filter" else "")
      )
      statsOrig <-
        Unmarshal(httpResponse).to[Map[String, Seq[NetworkAwarenessStatEntry]]]
      stats <- Future { statsOrig.get("graph").get.toSeq }
    } yield stats
  }

  final def getSwitchFlows(dpid: Int, filter: Boolean = false)(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[NetworkAwarenessFlowEntry]] = {
    for {
      httpResponse <- httpGet(
        s"/awareness/flows/$dpid" + (if (filter) "?filter" else "")
      )
      statsOrig <-
        Unmarshal(httpResponse).to[Map[String, Seq[NetworkAwarenessFlowEntry]]]
      stats <- Future { statsOrig.get("flows").get.toSeq }
    } yield stats
  }

  final def getDefaultMetricWeights(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Map[String, Double]] = {
    for {
      httpResponse <- httpGet("/awareness/weights/default_metrics")
      weightsOrig <-
        Unmarshal(httpResponse).to[Map[String, Map[String, Double]]]
      weights <- Future { weightsOrig.get("default_metrics").get }
    } yield weights
  }

  final def setDefaultMetricWeights(
      weights: Map[String, Double],
      flush: Boolean = false
  )(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Boolean] = {
    for {
      httpRequestEntity <-
        Marshal(Map("default_metrics" -> weights)).to[RequestEntity]
      httpResponse <- {
        val httpRequest = if (flush) httpPost _ else httpPatch _
        httpRequest("/awareness/weights/default_metrics", httpRequestEntity)
      }
      success <- NetworkAwarenessClientUtils.isSuccessful(httpResponse)
    } yield success
  }

  final def getSwitchWeights(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Map[Int, Double]] = {
    for {
      httpResponse <- httpGet("/awareness/weights/switches")
      weightsOrig <-
        Unmarshal(httpResponse).to[Map[String, Map[String, Double]]]
      weights <- Future {
        weightsOrig.get("switches").get.map({ case (k, v) => (k.toInt, v) })
      }
    } yield weights
  }

  final def setSwitchWeights(
      weights: Map[Int, Double],
      flush: Boolean = false
  )(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Boolean] = {
    for {
      httpRequestEntity <- Marshal(Map("switches" -> weights.map({
        case (k, v) => (k.toString(), v)
      }))).to[RequestEntity]
      httpResponse <- {
        val httpRequest = if (flush) httpPost _ else httpPatch _
        httpRequest("/awareness/weights/switches", httpRequestEntity)
      }
      success <- NetworkAwarenessClientUtils.isSuccessful(httpResponse)
    } yield success
  }

  final def getServices(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[NetworkAwarenessService]] = {
    for {
      httpResponse <- httpGet("/awareness/services")
      servicesOrig <-
        Unmarshal(httpResponse).to[Map[String, Map[String, NetworkAwarenessService]]]
      services <- Future { servicesOrig.get("services").get.values.toSeq }
    } yield services
  }

  final def setServices(
      services: Seq[NetworkAwarenessService],
      flush: Boolean = false
  )(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Boolean] = {
    for {
      httpRequestEntity <-
        Marshal(Map("services" -> services)).to[RequestEntity]
      httpResponse <- {
        val httpRequest = if (flush) httpPost _ else httpPatch _
        httpRequest("/awareness/services", httpRequestEntity)
      }
      success <- NetworkAwarenessClientUtils.isSuccessful(httpResponse)
    } yield success
  }

  final def getLinks(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[NetworkAwarenessLink]] = {
    for {
      httpResponse <- httpGet("/awareness/links")
      linksOrig <-
        Unmarshal(httpResponse).to[Map[String, Seq[NetworkAwarenessLink]]]
      links <- Future { linksOrig.get("links").get }
    } yield links
  }

  final def getAccessTable(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[NetworkAwarenessAccessTableEntry]] = {
    for {
      httpResponse <- httpGet("/awareness/access_table")
      entriesOrig <- Unmarshal(httpResponse)
        .to[Map[String, Seq[NetworkAwarenessAccessTableEntry]]]
      entries <- Future { entriesOrig.get("access_table").get }
    } yield entries
  }
}
