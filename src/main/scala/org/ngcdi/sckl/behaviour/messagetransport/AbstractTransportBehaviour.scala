package org.ngcdi.sckl.behaviour.messagetransport

import scala.util.Random

import akka.actor.ActorPath
import akka.actor.ActorRef

import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.Constants
import org.ngcdi.sckl.TransportMessages.TMessage

trait AbstractTransportBehaviour extends ScklActor {
  def transportBehaviour: Receive = PartialFunction.empty

  def transportPrestart(): Unit = {}

  def sendTransportMessage(
      message: Any,
      targetName: String,
      sender: ActorRef = self,
      senderName: String
  ): Unit = {
    sender.tell(getTMessage(message, targetName, senderName), sender)
  }

  private def getTMessage(message: Any, recipient: String, sender: String): TMessage = {
    TMessage(
      Random.nextLong(),
      Constants.messageForwardingInitialTtl,
      0,
      message,
      sender,
      recipient
    )
  }
}
