package org.ngcdi.sckl.ryuclient

case class NetworkAwarenessAccessTable(
    hosts: Map[String, NetworkAwarenessSwitch] // host_ip -> switch
)

object NetworkAwarenessAccessTable {
  def apply(
      table: Seq[NetworkAwarenessAccessTableEntry],
      topology: NetworkAwarenessTopology
  ): NetworkAwarenessAccessTable = {
    NetworkAwarenessAccessTable(table.map { entry =>
      Tuple2(entry.host_ip, topology.switches.get(entry.dpid).get)
    }.toMap)
  }
}
