// package org.ngcdi.sckl.ryuclient

// case class NetworkAwarenessDomainAccessTable(
//     hosts: Map[String, NetworkAwarenessSwitch] // host_ip -> switch
// )

// case class NetworkAwarenessGlobalAccessTable(
//   hosts: Map[String, NetworkAwarenessSwitch] // host_ip -> switch
// )

// object NetworkAwarenessDomainAccessTable {
//   def apply(
//       controllerId: Int,
//       table: Seq[NetworkAwarenessAccessTableEntry],
//       topology: NetworkAwarenessTopology
//   ): NetworkAwarenessDomainAccessTable = {
//     NetworkAwarenessDomainAccessTable(table.map { entry =>
//       Tuple2(entry.host_ip, topology.getSwitchById(entry.dpid, controllerId).get)
//     }.toMap)
//   }
// }

// object NetworkAwarenessGlobalAccessTable {
//   def join(
//     localAccessTables: Seq[NetworkAwarenessDomainAccessTable]
//   ) = {

//   }
// }