// package org.ngcdi.sckl.awareness

// case class AwarenessDomainAccessTable(
//     hosts: Map[String, AwarenessSwitch] // host_ip -> switch
// )

// case class AwarenessGlobalAccessTable(
//   hosts: Map[String, AwarenessSwitch] // host_ip -> switch
// )

// object AwarenessDomainAccessTable {
//   def apply(
//       controllerId: Int,
//       table: Seq[AwarenessAccessTableEntry],
//       topology: AwarenessTopology
//   ): AwarenessDomainAccessTable = {
//     AwarenessDomainAccessTable(table.map { entry =>
//       Tuple2(entry.host_ip, topology.getSwitchById(entry.dpid, controllerId).get)
//     }.toMap)
//   }
// }

// object AwarenessGlobalAccessTable {
//   def join(
//     localAccessTables: Seq[AwarenessDomainAccessTable]
//   ) = {

//   }
// }