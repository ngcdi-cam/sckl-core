package org.ngcdi.sckl.ryuclient

import scala.collection.mutable

object NetworkAwarenessTopology {
  def fromStats(
      stats: Seq[NetworkAwarenessStatEntry],
      controllerId: Int
  ): NetworkAwarenessTopology = {
    val links = stats
      .filter { p =>
        (p.metrics.getOrElse(
          "weight",
          0
        ) == 1) && (p.src_port != -1) && (p.dst_port != -1)
      }
      .map { p =>
        NetworkAwarenessLink(p.src, p.dst, p.src_port, p.dst_port)
      }

    fromLinks(links, controllerId)
  }

  def fromLinks(
      links: Seq[NetworkAwarenessLink],
      controllerId: Int
  ): NetworkAwarenessTopology = {
    val topo_raw = mutable.Map.empty[Int, mutable.Map[Int, Int]]
    links.foreach { f =>
      val src_switch_ports =
        topo_raw.getOrElseUpdate(f.src, mutable.Map.empty[Int, Int])
      val dst_switch_ports =
        topo_raw.getOrElseUpdate(f.dst, mutable.Map.empty[Int, Int])

      src_switch_ports.update(f.src_port, f.dst)
      dst_switch_ports.update(f.dst_port, f.src)
    }

    val switches = mutable.Map.empty[Int, NetworkAwarenessSwitch]
    topo_raw.keySet.foreach { f =>
      switches.update(f, new NetworkAwarenessSwitch(controllerId, f))
    }
    topo_raw.foreach {
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

    new NetworkAwarenessTopology(switches.toMap)
  }
}

case class NetworkAwarenessTopology(switches: Map[Int, NetworkAwarenessSwitch])
