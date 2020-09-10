package org.ngcdi.sckl.ryuclient

import scala.collection.mutable
import org.ngcdi.sckl.Constants
import akka.http.scaladsl.server.util.Tuple

object NetworkAwarenessTopology {
  // def fromStats(
  //     stats: Seq[NetworkAwarenessStatEntry],
  //     controllerId: Int
  // ): NetworkAwarenessTopology = {
  //   val links = stats
  //     .filter { p =>
  //       (p.metrics.getOrElse(
  //         Constants.awarenessHop,
  //         0
  //       ) == 1) && (p.src_port != -1) && (p.dst_port != -1)
  //     }
  //     .map { p =>
  //       NetworkAwarenessLink(p.src, p.dst, p.src_port, p.dst_port)
  //     }

  //   fromLinks(links, controllerId)
  // }

  def apply(
      links: Seq[NetworkAwarenessRawLink],
      accessTables: Seq[NetworkAwarenessRawAccessTableEntry],
      controllerId: Int
  ): NetworkAwarenessTopology = {
    val topoRaw = mutable.Map.empty[Int, mutable.Map[Int, Int]]
    links.foreach { f =>
      val src_switch_ports =
        topoRaw.getOrElseUpdate(f.src, mutable.Map.empty[Int, Int])
      val dst_switch_ports =
        topoRaw.getOrElseUpdate(f.dst, mutable.Map.empty[Int, Int])

      src_switch_ports.update(f.src_port, f.dst)
      dst_switch_ports.update(f.dst_port, f.src)
    }

    val switches = mutable.Map.empty[Int, NetworkAwarenessSwitch]
    topoRaw.keySet.foreach { f =>
      switches.update(f, new NetworkAwarenessSwitch(controllerId, f))
    }

    var hosts = Seq.empty[NetworkAwarenessHost]

    accessTables.foreach {
      case NetworkAwarenessRawAccessTableEntry(host_ip, host_mac, dpid, port) =>
        val switch = switches.get(dpid).get
        val host = NetworkAwarenessHost(host_ip, host_mac, switch)
        switch.ports.update(port, host)
        hosts = hosts.+:(host)
    }

    topoRaw.foreach {
      case (switch, ports) =>
        ports.foreach {
          case (port, remote_switch) =>
            switches
              .get(switch)
              .get
              .ports
              .update(port, switches.get(remote_switch).get)
        }
    }

    new NetworkAwarenessTopology(switches.values.toSeq, hosts)
  }

  // join topologies of different domains as a grand topology
  def join(
      topologies: Seq[NetworkAwarenessTopology],
      edgeLinks: Seq[NetworkAwarenessRawCrossDomainLink]
  ): NetworkAwarenessTopology = {
    val allSwitches =
      mutable.Map.empty ++= topologies.flatMap(_.idToSwitchMap).toMap
    edgeLinks.foreach {
      case NetworkAwarenessRawCrossDomainLink(
            srcDpid,
            srcPort,
            srcCtrlId,
            dstDpid,
            dstPort,
            dstCtrlId
          ) =>
        val switch1 = allSwitches.get(Tuple2(srcDpid, srcCtrlId)).get
        val switch2 = allSwitches.get(Tuple2(dstDpid, dstCtrlId)).get
        switch1.ports.update(srcPort, switch2)
        switch2.ports.update(dstPort, switch1)
    }
    val hosts = allSwitches.flatMap { switch =>
      switch._2.ports.collect {
        case (portId, host: NetworkAwarenessHost) => host
      }
    }
    new NetworkAwarenessTopology(allSwitches.values.toSeq, hosts.toSeq)
  }
}

class NetworkAwarenessTopology(
    _switches: Seq[NetworkAwarenessSwitch],
    _hosts: Seq[NetworkAwarenessHost]
) extends Serializable {

  val switches = _switches
  val hosts = _hosts

  @transient private lazy val idToSwitchMap = switches.map { x =>
    Tuple2(Tuple2(x.dpid, x.controllerId), x)
  }.toMap
  // (dpid, controllerId) -> switch

  @transient private lazy val ipToHostMap = hosts.groupBy(_.ip)

  @transient private lazy val edgeLinks = switches
    .flatMap { x =>
      x.getPeers
        .collect {
          case switch: NetworkAwarenessSwitch
              if switch.controllerId > x.controllerId =>
            switch
        }
        .map(Tuple2(x, _))
    }

  //Map[Tuple2[Int, Int], Seq[
  //   NetworkAwarenessSwitchLink
  // ]]]

  @transient private lazy val domainConnectivityGraph
      : Map[Tuple2[Int, Int], Seq[NetworkAwarenessSwitchLink]] = switches
    .flatMap { srcSwitch =>
      srcSwitch.ports
        .collect {
          case (port, switch: NetworkAwarenessSwitch)
              if switch.controllerId > srcSwitch.controllerId =>
            Tuple2(port, switch)
        }
        .map { dst =>
          val srcPort = dst._1
          val dstPort = dst._2.ports.find(_._2 == srcSwitch).get._1
          val dstSwitch = dst._2
          NetworkAwarenessSwitchLink(srcSwitch, dstSwitch, srcPort, dstPort)
        }
    }
    .groupBy { link =>
      Tuple2(link.src.controllerId, link.dst.controllerId)
    }

  @transient private lazy val domainConnectivityGraphSimple =
    domainConnectivityGraph.map(_._1).toMap

  def getSwitchById(
      dpid: Int,
      controllerId: Int
  ): Option[NetworkAwarenessSwitch] = {
    idToSwitchMap.get(Tuple2(dpid, controllerId))
  }

  def getHostByIp(
      ip: String
  ): Option[NetworkAwarenessHost] = {
    getHostsByIp(ip).lift(0)
  }

  def getHostsByIp(
      ip: String
  ): Seq[NetworkAwarenessHost] = {
    ipToHostMap.getOrElse(ip, Seq.empty)
  }

  def getEdgeLinks
      : Seq[Tuple2[NetworkAwarenessSwitch, NetworkAwarenessSwitch]] = edgeLinks

  def getPortFromLink(
      src: NetworkAwarenessSwitch,
      dst: NetworkAwarenessSwitch
  ): Tuple2[Int, Int] = {
    Tuple2(
      src.ports.collect {
        case Tuple2(portId, switch: NetworkAwarenessSwitch) if switch == dst =>
          portId
      }.head,
      dst.ports.collect {
        case Tuple2(portId, switch: NetworkAwarenessSwitch) if switch == src =>
          portId
      }.head
    )
  }

  def getDomainConnectivityGraph() = domainConnectivityGraph
  def getDomainConnectivityGraphSimple(): Map[Int, Int] =
    domainConnectivityGraphSimple
}

// case class NetworkAwarenessTopology(switches: Map[Int, NetworkAwarenessSwitch])
