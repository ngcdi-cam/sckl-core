package org.ngcdi.sckl.awareness

// class AwarenessController(
//     _id: Int,
//     _client: AwarenessClient,
//     _topology: AwarenessTopology
// ) extends Serializable {
//   val id = _id
//   val client = _client
//   val topology = _topology
// }

case class AwarenessController(
    id: Int,
    client: AwarenessClient,
    topology: AwarenessTopology,
    // accessTable: AwarenessAccessTable
)
