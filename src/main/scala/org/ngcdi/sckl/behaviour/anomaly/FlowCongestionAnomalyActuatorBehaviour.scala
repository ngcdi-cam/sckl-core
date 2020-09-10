package org.ngcdi.sckl.behaviour

import org.ngcdi.sckl.actuator.CongestionActuator
import org.ngcdi.sckl.Constants
import org.ngcdi.sckl.AnomalyMessages._
import org.ngcdi.sckl.anomalydetector.AwarenessCongestionAnomalyDetectionResult
import scala.util.Success
import scala.util.Failure
import org.ngcdi.sckl.behaviour.neighbouring.NameResolutionUtils
import scala.concurrent.Future
import org.ngcdi.sckl.ryuclient.NetworkAwarenessRawFlowEntry
import org.ngcdi.sckl.behaviour.awareness.NetworkAwarenessSwitchProvider

case class FlowCongestionDetected(
    src_ip_dpid: Int,
    flows: Seq[NetworkAwarenessRawFlowEntry]
)

trait FlowCongestionAnomalyActuatorBehaviour
    extends AnomalyActuatorBehaviour
    with NetworkAwarenessManagerReceiverBehaviour
    with NetworkAwarenessSwitchProvider {

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
          targetActor.map { actor =>
            log.info(
              s"Sending FlowCongestionDetected to ${actor.path}, src_ip_dpid is ${flow._1}"
            )
            actor ! FlowCongestionDetected(flow._1, flow._2.toSeq)
            Unit
          }
        })
      } yield ret
  }
}
