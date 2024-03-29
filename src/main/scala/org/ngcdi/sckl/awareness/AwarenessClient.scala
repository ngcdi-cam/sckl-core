package org.ngcdi.sckl.awareness

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.unmarshalling._
import spray.json.DefaultJsonProtocol._
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import scala.concurrent.Future
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.RequestEntity
import org.ngcdi.sckl.base.SimpleRestClient
// import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
// import spray.json.DefaultJsonProtocol

trait AwarenessJsonSupport {
  @transient lazy implicit val statEntryFormat = jsonFormat5(
    AwarenessRawStatEntry
  );
  @transient lazy implicit val flowEntryFormat = jsonFormat9(
    AwarenessRawFlowEntry
  );
  @transient lazy implicit val serviceFormat = jsonFormat4(
    AwarenessService
  )
  @transient lazy implicit val accessTableEntryFormat = jsonFormat4(
    AwarenessRawAccessTableEntry
  )
  @transient lazy implicit val linkFormat = jsonFormat4(
    AwarenessRawLink
  )
  @transient lazy implicit val pathInfoPairFormat = jsonFormat3(
    AwarenessRawPathPairInfo
  )
  @transient lazy implicit val pathInfoPreFormat = jsonFormat2(
    AwarenessRawPathInfoPre
  )
  @transient lazy implicit val accessTableEntryPinningFormat = jsonFormat3(
    AwarenessRawAccessTableEntryPinning
  )
  @transient lazy implicit val pathInfoPairRequestFormat = jsonFormat4(
    AwarenessRawPathInfoPairRequest
  )
  @transient lazy implicit val pathInfoRequestFormat = jsonFormat2(
    AwarenessRawPathInfoRequest
  )
}

class AwarenessClient(baseUrl: String)
    extends SimpleRestClient(baseUrl)
    with Serializable
    with AwarenessJsonSupport {

  final def getBaseUrl(): String = baseUrl

  final def getStats(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[AwarenessRawStatEntry]] = {
    for {
      httpResponse <- httpGet("/awareness/stats")
      statsOrig <- Unmarshal(httpResponse)
        .to[Map[String, Seq[AwarenessRawStatEntry]]]
      stats <- Future { statsOrig.get("graph").get.toSeq }
    } yield stats
  }

  final def getSwitchStats(dpid: Int, filter: Boolean = false)(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[AwarenessRawStatEntry]] = {
    for {
      httpResponse <- httpGet(
        s"/awareness/stats/$dpid" + (if (filter) "?filter" else "")
      )
      statsOrig <- Unmarshal(httpResponse)
        .to[Map[String, Seq[AwarenessRawStatEntry]]]
      stats <- Future { statsOrig.get("graph").get.toSeq }
    } yield stats
  }

  final def getSwitchFlows(dpid: Int, filter: Boolean = false)(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[AwarenessRawFlowEntry]] = {
    for {
      httpResponse <- httpGet(
        s"/awareness/flows/$dpid" + (if (filter) "?filter" else "")
      )
      statsOrig <- Unmarshal(httpResponse)
        .to[Map[String, Seq[AwarenessRawFlowEntry]]]
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
      success <- AwarenessClientUtils.isSuccessful(httpResponse)
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
      success <- AwarenessClientUtils.isSuccessful(httpResponse)
    } yield success
  }

  final def getServices(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[AwarenessService]] = {
    for {
      httpResponse <- httpGet("/awareness/services")
      servicesOrig <- Unmarshal(httpResponse)
        .to[Map[String, Map[String, AwarenessService]]]
      services <- Future { servicesOrig.get("services").get.values.toSeq }
    } yield services
  }

  final def setServices(
      services: Seq[AwarenessService],
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
      success <- AwarenessClientUtils.isSuccessful(httpResponse)
    } yield success
  }

  final def getLinks(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[AwarenessRawLink]] = {
    for {
      httpResponse <- httpGet("/awareness/links")
      linksOrig <-
        Unmarshal(httpResponse).to[Map[String, Seq[AwarenessRawLink]]]
      links <- Future { linksOrig.get("links").get }
    } yield links
  }

  final def getAccessTable(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Seq[AwarenessRawAccessTableEntry]] = {
    for {
      httpResponse <- httpGet("/awareness/access_table")
      entriesOrig <- Unmarshal(httpResponse)
        .to[Map[String, Seq[AwarenessRawAccessTableEntry]]]
      entries <- Future { entriesOrig.get("access_table").get }
    } yield entries
  }

  final def getPathInfo(
      r: AwarenessRawPathInfoRequest
  )(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[AwarenessRawPathInfo] = {
    for {
      httpRequestEntity <- Marshal(r).to[RequestEntity]
      httpResponse <- httpPost(s"/awareness/path_info", httpRequestEntity)
      infoPre <- Unmarshal(httpResponse).to[AwarenessRawPathInfoPre]
      info <- Future {
        AwarenessRawPathInfo(infoPre.stats, infoPre.switch_weights.map { case (k, v) => (k.toInt, v) })
      }
    } yield info
  }

  final def setAccessTableEntryPinning(
      pinnings: Seq[AwarenessRawAccessTableEntryPinning],
      flush: Boolean = false
  )(implicit
      ec: ExecutionContext,
      actorSystem: ActorSystem
  ): Future[Boolean] = {
    for {
      httpRequestEntity <-
        Marshal(Map("pinnings" -> pinnings)).to[RequestEntity]
      httpResponse <- {
        val httpRequest = if (flush) httpPost _ else httpPatch _
        httpRequest("/awareness/access_table_entry_pinnings", httpRequestEntity)
      }
      success <- AwarenessClientUtils.isSuccessful(httpResponse)
    } yield success
  }
}
