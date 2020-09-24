package org.ngcdi.sckl.behaviour.messagetransport

import org.ngcdi.sckl.TransportMessages.TMessage
import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.behaviour.neighbouring.NameResolutionUtils
import scala.util.Success
import scala.util.Failure

trait DirectTransportBehaviour extends AbstractTransportBehaviour {
  this: ScklActor => 
  override def transportPrestart() = {}
  
  override def transportBehaviour: Receive = {
    case x: TMessage =>
      NameResolutionUtils.resolveNodeName(x.recipient).onComplete {
        case Success(value) => 
          value ! x.message
          log.info(s"Message ${x.message} sent to ${value}")
        case Failure(exception) => 
          log.error(s"Failed to resolve node name ${x.recipient}: $exception")
      }
  }
}
