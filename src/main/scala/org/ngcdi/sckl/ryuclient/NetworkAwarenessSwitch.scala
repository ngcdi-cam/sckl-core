package org.ngcdi.sckl.ryuclient

import scala.collection.mutable

class NetworkAwarenessSwitch(
    _controllerId: Int,
    _dpid: Int,
    _ports: => Map[Int, NetworkAwarenessSwitch] = Map.empty
) extends Serializable {
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
  
  var ports = mutable.Map.empty[Int, NetworkAwarenessSwitch] ++= _ports

  def getPeers: Seq[NetworkAwarenessSwitch] = {
    ports.values.toSeq
  }

  def getPorts: Seq[Int] = {
    ports.keySet.toSeq
  }

  override def toString(): String = s"Switch($controllerId, $dpid, ports = ${ports.keySet})"
}
