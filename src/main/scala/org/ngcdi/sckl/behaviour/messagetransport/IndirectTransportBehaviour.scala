package org.ngcdi.sckl.behaviour.messagetransport

import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.TransportMessages.TMessage
import scala.collection.mutable
import akka.actor.ActorPath
import org.ngcdi.sckl.Constants
import org.ngcdi.sckl.TransportMessages
import org.ngcdi.sckl.behaviour.neighbouring.NameResolutionUtils
import scala.util.Success
import scala.util.Failure
import akka.actor.ActorRef
import org.ngcdi.sckl.ClusteringConfig

trait IndirectTransportBehaviour
    extends AbstractTransportBehaviour
    with TransportTopologyProvider {

  this: ScklActor =>

  override def transportPrestart() = {
    transportTopologyProviderPrestart()
  }

  private val table =
    mutable.Map.empty[String, Tuple2[
      Int,
      String
    ]] // destination -> (hops, nexthop)

  private val messagesIdsReceived = mutable.Queue.empty[Long]
  private val queueMaxSize = Constants.messageForwardingIdQueueMaxSize

  final override def transportBehaviour: Receive = {
    case msg: TMessage =>
      if (
        !table.contains(msg.sender) || (table
          .contains(msg.sender) && msg.hops < table.get(msg.sender).get._1)
      ) {
        log.info(s"Sender: ${sender()}")
        table.update(msg.sender, Tuple2(msg.hops, sender().path.address.host.getOrElse(ClusteringConfig.nodeIp)))
        log.info(s"Shortest forwarding table updated: $table")
      }

      if (messagesIdsReceived.contains(msg.id)) {
        log.info("Dropping message because it is received earlier")
      } else {
        messagesIdsReceived.enqueue(msg.id)
        if (messagesIdsReceived.size > queueMaxSize)
          messagesIdsReceived.dequeue()

        if (ClusteringConfig.nodeIp == msg.recipient) {
          log.info("TMessage unpacked and sent to myself")
          self ! msg.message
        } else if (msg.ttl == 0) {
          log.info("Dropping message because of zero TTL")
        } else {

          if (table.contains(msg.recipient)) {
            // forward the message
            val recipient = msg.recipient
            val nexthop = table.get(recipient).get._2
            log.info(s"FORWARD: nexthop = $nexthop, dest = $recipient")

            NameResolutionUtils.resolveNodeName(nexthop).onComplete {
              case Success(value) => 
                value ! TransportMessages.nextTMessage(msg)
              case Failure(exception) => 
                log.error("Error resolving node name: " + exception)
            }
          } else {
            // flood the message
            log.info("FLOOD")
            transportNeighbourRefs.foreach { x =>
              x.foreach { nexthop =>
                nexthop ! TransportMessages.nextTMessage(msg)
              }
            }
          }
        }
      }
  }

  final def transportFlushTable(): Unit = table.clear()
}
