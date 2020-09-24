package org.ngcdi.sckl.awareness

import scala.collection.mutable

object AwarenessTopology {
  // def fromStats(
  //     stats: Seq[AwarenessStatEntry],
  //     controllerId: Int
  // ): AwarenessTopology = {
  //   val links = stats
  //     .filter { p =>
  //       (p.metrics.getOrElse(
  //         Constants.awarenessHop,
  //         0
  //       ) == 1) && (p.src_port != -1) && (p.dst_port != -1)
  //     }
  //     .map { p =>
  //       AwarenessLink(p.src, p.dst, p.src_port, p.dst_port)
  //     }

  //   fromLinks(links, controllerId)
  // }

  def apply(
      links: Seq[AwarenessRawLink],
      accessTables: Seq[AwarenessRawAccessTableEntry],
      controllerId: Int
  ): AwarenessTopology = {
    val topoRaw = mutable.Map.empty[Int, mutable.Map[Int, Int]]
    links.foreach { f =>
      val src_switch_ports =
        topoRaw.getOrElseUpdate(f.src, mutable.Map.empty[Int, Int])
      val dst_switch_ports =
        topoRaw.getOrElseUpdate(f.dst, mutable.Map.empty[Int, Int])

      src_switch_ports.update(f.src_port, f.dst)
      dst_switch_ports.update(f.dst_port, f.src)
    }

    val switches = mutable.Map.empty[Int, AwarenessSwitch]
    topoRaw.keySet.foreach { f =>
      switches.update(f, new AwarenessSwitch(controllerId, f))
    }

    var hosts = Seq.empty[AwarenessHost]

    accessTables.foreach {
      case AwarenessRawAccessTableEntry(host_ip, host_mac, dpid, port) =>
        val switch = switches.get(dpid).get
        val host = AwarenessHost(host_ip, host_mac, switch)
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

    new AwarenessTopology(switches.values.toSeq, hosts)
  }

  // join topologies of different domains as a grand topology
  def join(
      topologies: Seq[AwarenessTopology],
      edgeLinks: Seq[AwarenessRawCrossDomainLink]
  ): AwarenessTopology = {
    val allSwitches =
      mutable.Map.empty ++= topologies.flatMap(_.dpidCtrlIdToSwitchMap).toMap
    edgeLinks.foreach {
      case AwarenessRawCrossDomainLink(
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
        case (portId, host: AwarenessHost) => host
      }
    }
    new AwarenessTopology(allSwitches.values.toSeq, hosts.toSeq)
  }
}

class AwarenessTopology(
    _switches: Seq[AwarenessSwitch],
    _hosts: Seq[AwarenessHost]
) extends Serializable {

  val switches = _switches
  val hosts = _hosts

  @transient private lazy val dpidCtrlIdToSwitchMap = switches.map { x =>
    Tuple2(Tuple2(x.dpid, x.controllerId), x)
  }.toMap
  // (dpid, controllerId) -> switch

  @transient private lazy val dpidToSwitchMap = switches.map { x =>
    Tuple2(x.dpid, x)
  }.toMap


  @transient private lazy val ipToHostMap = hosts.groupBy(_.ip)

  @transient private lazy val edgeLinks = switches
    .flatMap { x =>
      x.getNeighbours
        .collect {
          case switch: AwarenessSwitch
              if switch.controllerId > x.controllerId =>
            switch
        }
        .map(Tuple2(x, _))
    }

  @transient lazy val domainConnectivityGraph
      : Map[Tuple2[Int, Int], Seq[AwarenessSwitchLink]] = switches
    .flatMap { srcSwitch =>
      srcSwitch.ports
        .collect {
          case (port, switch: AwarenessSwitch)
              if switch.controllerId > srcSwitch.controllerId =>
            Tuple2(port, switch)
        }
        .map { dst =>
          val srcPort = dst._1
          val dstPort = dst._2.ports.find(_._2 == srcSwitch).get._1
          val dstSwitch = dst._2
          AwarenessSwitchLink(srcSwitch, dstSwitch, srcPort, dstPort)
        }
    }
    .groupBy { link =>
      Tuple2(link.src.controllerId, link.dst.controllerId)
    }

  @transient lazy val domainConnectivityGraphSimple =
    domainConnectivityGraph.map(_._1).toMap

  def getSwitchById(
      dpid: Int,
      controllerId: Int
  ): Option[AwarenessSwitch] = {
    dpidCtrlIdToSwitchMap.get(Tuple2(dpid, controllerId))
  }

  // If there are multiple switches having the same dpid, the output of this function is undefined.
  def getSwitchById(
    dpid: Int
  ): Option[AwarenessSwitch] = {
    dpidToSwitchMap.get(dpid)
  }

  def getHostByIp(
      ip: String
  ): Option[AwarenessHost] = {
    getHostsByIp(ip).lift(0)
  }

  def getHostsByIp(
      ip: String
  ): Seq[AwarenessHost] = {
    ipToHostMap.getOrElse(ip, Seq.empty)
  }

  def getEdgeLinks
      : Seq[Tuple2[AwarenessSwitch, AwarenessSwitch]] = edgeLinks

  def getPortFromLink(
      src: AwarenessSwitch,
      dst: AwarenessSwitch
  ): Tuple2[Int, Int] = {
    Tuple2(
      src.ports.collect {
        case Tuple2(portId, switch: AwarenessSwitch) if switch == dst =>
          portId
      }.head,
      dst.ports.collect {
        case Tuple2(portId, switch: AwarenessSwitch) if switch == src =>
          portId
      }.head
    )
  }

  // def getDomainConnectivityGraph() = domainConnectivityGraph
  // def getDomainConnectivityGraphSimple(): Map[Int, Int] =
  //   domainConnectivityGraphSimple
}

// case class AwarenessTopology(switches: Map[Int, AwarenessSwitch])
