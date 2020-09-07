package org.ngcdi.sckl.behaviour.forwarder

import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.behaviour.neighbouring.TargetPathsProvider
import org.ngcdi.sckl.ForwardingMessages._
import scala.collection.mutable
import org.ngcdi.sckl.Constants

trait MessageForwardingBehaviour extends ScklActor with TargetPathsProvider {

  private val messagesIdsReceived = mutable.Queue.empty[Long]
  private val queueMaxSize = Constants.messageForwardingIdQueueMaxSize
  final def messageForwardingBehaviour: Receive = {
    case ForwardedMessage(id, ttl, hops, message) =>
      log.info(s"Received ForwardedMessage($id, $ttl, $hops, $message) ")
      if (messagesIdsReceived.contains(id))
        log.info("Dropping message because it is received earlier")
      else {
        messagesIdsReceived.enqueue(id)
        if (messagesIdsReceived.size > queueMaxSize)
          messagesIdsReceived.dequeue()

        if (ttl > 0) {
          val targetPaths = getTargetPaths().filter { x =>
            x != sender().path && x != self.path
          }
          log.info(s"Recipients: " + targetPaths)
          targetPaths.foreach { tp =>
            context.actorSelection(tp) ! ForwardedMessage(
              id,
              ttl - 1,
              hops + 1,
              message
            )
          }
        }
        else {
          log.info("Dropping message because of 0 TTL")
        }
        self ! message
      }
  }
}
