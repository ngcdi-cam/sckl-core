package org.ngcdi.sckl.ryuclient

import scala.collection.mutable

sealed trait NetworkAwarenessNode // switch or host

class NetworkAwarenessSwitch(
    _controllerId: Int,
    _dpid: Int,
    _ports: => Map[Int, NetworkAwarenessNode] = Map.empty
) extends Serializable
    with NetworkAwarenessNode {

  val controllerId = _controllerId
  val dpid = _dpid

  override def hashCode(): Int = {
    controllerId ^ dpid
  }

  override def equals(x: Any): Boolean = {
    x match {
      case s: NetworkAwarenessSwitch =>
        controllerId == s.controllerId && dpid == s.dpid
      case _ =>
        false
    }
  }

  var ports = mutable.Map.empty[Int, NetworkAwarenessNode] ++= _ports

  def getPeers: Seq[NetworkAwarenessNode] = {
    ports.values.toSeq
  }

  def getPeerSwitches: Seq[NetworkAwarenessSwitch] = {
    getPeers.collect { case x: NetworkAwarenessSwitch => x }.toSeq
  }

  def getPorts: Seq[Int] = {
    ports.keySet.toSeq
  }

  override def toString(): String =
    s"NetworkAwarenessSwitch($controllerId,$dpid)"
}

case class NetworkAwarenessHost(
    ip: String,
    mac: String,
    switch: NetworkAwarenessSwitch
) extends NetworkAwarenessNode

case class NetworkAwarenessSwitchLink(
  src: NetworkAwarenessSwitch,
  dst: NetworkAwarenessSwitch,
  srcPort: Int,
  dstPort: Int
)