// WiP

// package org.ngcdi.sckl.behaviour.neighbouring

// import org.ngcdi.sckl.behaviour.TargetPathsProvider
// import akka.actor.ActorPath
// import org.ngcdi.sckl.ScklActor
// import org.ngcdi.sckl.behaviour.NetworkAwarenessManagerReceiverBehaviour

// trait AwarenessNeighbouringBehaviour 
//   extends ScklActor
//   with TargetPathsProvider
//   with NetworkAwarenessManagerReceiverBehaviour {

//   override def getTargetPaths(): Seq[ActorPath] = {
//     awarenessManager.getSwitchById()
//   }
// }