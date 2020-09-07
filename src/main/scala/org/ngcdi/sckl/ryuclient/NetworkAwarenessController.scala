package org.ngcdi.sckl.ryuclient

// class NetworkAwarenessController(
//     _id: Int,
//     _client: NetworkAwarenessClient,
//     _topology: NetworkAwarenessTopology
// ) extends Serializable {
//   val id = _id
//   val client = _client
//   val topology = _topology
// }

case class NetworkAwarenessController(
    id: Int,
    client: NetworkAwarenessClient,
    topology: NetworkAwarenessTopology,
    accessTable: NetworkAwarenessAccessTable
)
