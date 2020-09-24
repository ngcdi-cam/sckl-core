package org.ngcdi.sckl.awareness

import scala.collection.mutable

sealed trait AwarenessNode // switch or host

class AwarenessSwitch(
    _controllerId: Int,
    _dpid: Int,
    _ports: => Map[Int, AwarenessNode] = Map.empty
) extends Serializable
    with AwarenessNode {

  val controllerId = _controllerId
  val dpid = _dpid

  override def hashCode(): Int = {
    controllerId ^ dpid
  }

  override def equals(x: Any): Boolean = {
    x match {
      case s: AwarenessSwitch =>
        controllerId == s.controllerId && dpid == s.dpid
      case _ =>
        false
    }
  }

  var ports = mutable.Map.empty[Int, AwarenessNode] ++= _ports

  def getNeighbours: Seq[AwarenessNode] = {
    ports.values.toSeq
  }

  def getNeighbourSwitches: Seq[AwarenessSwitch] = {
    getNeighbours.collect { case x: AwarenessSwitch => x }.toSeq
  }

  def getPorts: Seq[Int] = {
    ports.keySet.toSeq
  }

  override def toString(): String =
    s"AwarenessSwitch($controllerId,$dpid)"
}

case class AwarenessHost(
    ip: String,
    mac: String,
    switch: AwarenessSwitch
) extends AwarenessNode

case class AwarenessSwitchLink(
  src: AwarenessSwitch,
  dst: AwarenessSwitch,
  srcPort: Int,
  dstPort: Int
)