package org.ngcdi.sckl

import org.ngcdi.sckl.sdn._
import org.ngcdi.sckl.Config._
import org.ngcdi.sckl.ClusteringConfig._
import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.model._

import akka.stream.ActorMaterializer
import akka.actor._

import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer

trait RESTController extends DAController {
  this: DigitalAssetBase =>

  var restClientSen: ActorRef = _
  def query: String

  // var lastReadings:Seq[FlowStat]= _

  override def startSensors(): Unit = {
    log.info("REST Start Sensors..")
    restClientSen = context.actorOf(
      RESTClient.props(sdncServer, sdncPort),
      name = "restClientSen"
    )
    log.info("restClientSen Reference Found==>" + restClientSen)
  }

  override def sense(assetName: String, freqSensing: Int): Unit = {
    log.debug("NODE_name:====>" + nodeName + "<->query->" + query + "<--")
    restClientSen ! GetRequest(query, false)
    context.system.scheduler.scheduleOnce(freqSensing seconds, self, ReSense)
  }
}
