package org.ngcdi.sckl.behaviour.anomaly

import org.ngcdi.sckl.AnomalyMessages._
import org.ngcdi.sckl.anomalydetector.AwarenessCongestionAnomalyDetectionResult
import scala.concurrent.Future
import org.ngcdi.sckl.awareness.AwarenessRawFlowEntry
import org.ngcdi.sckl.behaviour.awareness.AwarenessSwitchProvider
import org.ngcdi.sckl.behaviour.awareness.AwarenessManagerReceiverBehaviour

case class FlowCongestionDetected(
    src_ip_dpid: Int,
    flows: Seq[AwarenessRawFlowEntry]
)

trait FlowCongestionAnomalyActuatorBehaviour
    extends AnomalyActuatorBehaviour
    with AwarenessManagerReceiverBehaviour
    with AwarenessSwitchProvider {

  override def anomalyActuatorBehaviour: Receive = {
    case AnomalyDetected(anomaly: AwarenessCongestionAnomalyDetectionResult) =>
      val flows = anomaly.congestedLinks
        .flatMap(_._2.flows)
        .groupBy(_.src_ip_dpid) // send anomaly to the head of path for now

      log.info("flows = " + flows)

      for {
        switch <- awarenessSwitch
        manager <- awarenessManager
        ret <- Future.sequence(flows.map { flow =>
          val controllerId = switch.controllerId
          val targetActor = manager.getActorOfSwitch(
            manager.getSwitchById(flow._1, controllerId).get
          )
          targetActor.flatMap { actor =>
            log.info(
              s"Sending FlowCongestionDetected to ${actor.path}, src_ip_dpid is ${flow._1}"
            )
            actor ! FlowCongestionDetected(flow._1, flow._2.toSeq)

            val globalEndpointSwitch = manager.topology.hosts
              .find { host =>
                flow._2.map(_.src_ip).toSeq.distinct.contains(host.ip)
              }
              .get
              .switch

            val globalServiceManagerActor =
              manager.getActorOfSwitch(globalEndpointSwitch)

            globalServiceManagerActor.map { actor =>
              log.info(
                s"Sending FlowCongestionDetected to ${actor.path}, src_ip_dpid is ${flow._1}"
              )
              actor ! FlowCongestionDetected(flow._1, flow._2.toSeq)
              Unit
            }
          }
        })
      } yield ret
  }
}
