package org.ngcdi.sckl

import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.ClusteringConfig._
import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.model._

import org.ngcdi.sckl.sdn._
import org.ngcdi.sckl.NetworkUtils._
import org.ngcdi.sckl.awareness.AwarenessRawStatEntry
import org.ngcdi.sckl.behaviour.awareness.AwarenessStatsSensorBehaviour
import org.ngcdi.sckl.behaviour.awareness.AwarenessSwitchStatsSensorBehaviour
import org.ngcdi.sckl.behaviour.awareness.AwarenessSwitchFlowsSensorBehaviour
import org.ngcdi.sckl.awareness.AwarenessRawFlowEntry

//final case class ProcessResponse(entityStr:String,entity:ResponseEntity)
//final case class ProcessResponse(entity:String)

/*
 * Controller that enables sensing and processing from different sources
 */
trait CombinedController
    // extends PortParser
    extends ScklActor
    with FileController
    with AwarenessStatsSensorBehaviour
    with AwarenessSwitchStatsSensorBehaviour
    with AwarenessSwitchFlowsSensorBehaviour
    {
  this: DigitalAssetBase =>

  // override def query = Config.restMonitoringUrl + nodeName

  override def startSensors(): Unit = {
    log.info("Starting Combined!!!")
    // super[RESTController].startSensors()
    super[FileController].startSensors()

    if (awarenessStatsSensorEnabled)
      super[AwarenessStatsSensorBehaviour].sensorPrestart()
    
    if (awarenessSwitchStatsSensorEnabled)
      super[AwarenessSwitchStatsSensorBehaviour].sensorPrestart()
    
    if (awarenessSwitchFlowSensorEnabled)
      super[AwarenessSwitchFlowsSensorBehaviour].sensorPrestart()
  }

  override def updateMeasurements(newMeasurements: Seq[Model]) = {
    val now = getReadingTime()
    log.debug("Combined newMeasurements==>" + newMeasurements)
    newMeasurements
      .foreach {
        case nf: PortStat =>
          lastReadings
            .map(t => t.asInstanceOf[PortStat])
            .filter(_.port_no == nf.port_no)
            .map { of =>
              getThroughputKBytes(
                of.tx_bytes,
                nf.tx_bytes,
                of.duration_sec,
                nf.duration_sec
              ) match {
                case Some(v) =>
                  sendToLocalView(
                    nodeName,
                    nf.port_no.toString(),
                    v,
                    now,
                    ThroughputOut
                  )
                case None =>
                  log.info("No Throughput Calculated")
              }
            }
        case nf: AwarenessRawStatEntry =>
          val flow = nf.src + ":" + nf.dst // TODO: DO NOT use the string representation of the flow
          nf.metrics.foreach {
            case (metric, value) =>
              sendToLocalView(nodeName, flow, value, now, metric)
          }
        case nf: AwarenessRawFlowEntry =>
          val flow = s"${nf.src}:${nf.dst}:${nf.src_ip}:${nf.dst_ip}:${nf.src_ip_dpid}:${nf.dst_ip_dpid}"
          sendToLocalView(nodeName, flow, nf.throughput, now, awarenessFlowThroughput)
        case _ =>
      }

    lastReadings = newMeasurements
  }

  override def ctlBehaviour = {
    super.ctlBehaviour
      .orElse[Any, Unit] {
        case Link(linkFileName: String) =>
          linkViaCSV(linkFileName)
      }
  }
}
