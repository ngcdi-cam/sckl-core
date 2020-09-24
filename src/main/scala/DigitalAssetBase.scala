package org.ngcdi.sckl

import akka.actor._
import ClusteringConfig._
import msgs._
import org.vca._
import org.ngcdi.sckl.behaviour.neighbouring.TargetPathsProvider
import org.ngcdi.sckl.behaviour.messagetransport.AbstractTransportBehaviour

abstract class DigitalAssetBase(
    id: String,
    localProcessor: ActorRef,
    _awarenessStatsSensorEnabled: Boolean = false,
    _awarenessSwitchStatsSensorEnabled: Boolean = true,
    _awarenessSwitchFlowSensorEnabled: Boolean = true
) extends AbstractTransportBehaviour
    with DAController
    with ConnectionBehaviour
    with TargetPathsProvider {

  var lView: ActorRef = _
  var lViewPath: ActorPath = _

  val awarenessStatsSensorEnabled = _awarenessStatsSensorEnabled
  val awarenessSwitchStatsSensorEnabled = _awarenessSwitchStatsSensorEnabled
  val awarenessSwitchFlowSensorEnabled = _awarenessSwitchFlowSensorEnabled

  def getLocalProcessorPath(): ActorPath = {
    return localProcessor.path
  }

  def startLocalView(): ActorRef = {
    log.info("EJECUTANDO START LOCAL VIEW")
    val lView = context.actorOf(
      LocalView.props(
        self,
        20, // 20 seconds to report,
        getTargetPaths,
        getLocalProcessorPath
      ),
      name = "localView" + nodeName
    )

    return lView
  }

  def getLocalViewPath(): ActorPath = {
    return lViewPath
  }

  def setLocalViewPath(p: ActorPath): Unit = {
    lViewPath = p
  }

  def baseBehaviour: Receive = {
    case AnomalyDetected(timestamps: Seq[String], resultAD: String) =>
      countMsg("scklmsg")
      timestamps.foreach { x =>
        log.debug("anomaly_detected:" + x)
        stopTimer(x, "anomd")
      }
  }
}
