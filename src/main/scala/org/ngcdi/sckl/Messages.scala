package org.ngcdi.sckl

import org.ngcdi.sckl.msgs.Measurement
import org.ngcdi.sckl.anomalydetector.AnomalyDetectionResult
import akka.actor.ActorPath

object AnomalyMessages {
  // @name: NewProcessedMeasurements
  // @description: NewProcessedMeasurements should be sent by ServiceView when a set of processed measurements are available
  // @sender: ServiceView
  // @receiver: AnomalyDetector

  case class NewProcessedMeasurements(tick: Int, sample: Seq[Measurement])

  // @name: AnomalyDetected
  // @description: AnomalyDetected messages should be sent by subclasses of AnomalyDetector whenever a anomaly detected
  // @sender: subclass of this AnomalyDetector trait
  // @receiver: interested Actuators which react to the anomaly

  case class AnomalyDetected(anomaly: AnomalyDetectionResult)
}

object AwarenessMessages {

  // @name: DoGetAwarenessStats
  // @description: DoGetAwarenessStats messages are sent by AwarenessStatsStreamerBehaviour periodically to get the latest stats from the Network Awareness app
  // @sender: AwarenessStatsStreamerBehaviour periodically
  // @receiver: AwarenessStatsStreamerBehaviour

  object DoGetAwarenessStats
}

object ForwardingMessages {
  case class ForwardedMessage(id: Long, ttl: Int, hops: Int, message: Any)
}

object TransportMessages {
  case class TMessage(
      id: Long,
      ttl: Int,
      hops: Int,
      message: Any,
      sender: String,
      recipient: String
  )

  def nextTMessage(orig: TMessage) = {
    orig match {
      case TMessage(id, ttl, hops, message, original_sender, recipient) =>
        TMessage(
          id,
          ttl - 1,
          hops + 1,
          message,
          original_sender,
          recipient
        )
    }
  }
}
