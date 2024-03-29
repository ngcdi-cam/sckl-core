package org.ngcdi.sckl
import akka.actor._

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId, ZonedDateTime}
import java.util.{Locale}

import kamon.metric.Counter
import kamon.Kamon

import ClusteringConfig._
import msgs._
import Constants._
import org.ngcdi.sckl.behaviour.neighbouring.TargetPathsProvider
import org.ngcdi.sckl.behaviour.forwarder.MessageForwardingBehaviour
import scala.util.Random
import org.ngcdi.sckl.behaviour.messagetransport.AbstractTransportBehaviour
import org.ngcdi.sckl.behaviour.messagetransport.DirectTransportBehaviour

final case class UpdateTargets(newTargets: Seq[ActorPath])

object LocalView {
  def props(
      digitalAssetRef: ActorRef,
      consolidateAtSeconds: Int,
      targetPaths: Seq[ActorPath],
      lpPath: ActorPath
  ): Props = Props(new LocalView(digitalAssetRef, consolidateAtSeconds, targetPaths, lpPath))
}

class LocalView(
    digitalAssetRef: ActorRef,
    consolidateAtSeconds: Int,
    initialTargetPaths: Seq[ActorPath],
    localProcessorPath: ActorPath
) extends AbstractTransportBehaviour
    with TargetPathsProvider
    // with MessageForwardingBehaviour 
    // with DirectTransportBehaviour
      {

  var msgTypeCount: Map[String, Counter] = _
  var view: ListBuffer[Measurement] = _

  override def preStart() = {
    view = new ListBuffer[Measurement]
    system.scheduler.scheduleOnce(
      consolidateAtSeconds.seconds,
      self,
      Consolidate
    )
    neighbours = initialTargetPaths
    log.debug("Finished pre-start of actor" + getTargetPaths())
  }

  def receive: Receive = {
    localViewBehaviour().orElse(transportBehaviour)
  }

  def localViewBehaviour(): Receive = {
    // Sense Functions

    case s: String =>
      val m = parse(s)
      log.debug("My path:" + self)
      log.debug("sender: " + sender)
      log.debug("Existing Keys: {}", msgTypeCount)
      log.debug("value of smp:" + getTargetPaths())
      record(m)
      publish("ngcdi.da.sensed", m.metricName, "flow", m.resourceId, m.value)

    case Report(cView: Seq[Measurement]) =>
      report(cView)

    case Consolidate =>
      consolidate()

    case UpdateTargets(newTargets: Seq[ActorPath]) =>
      addTargetPaths(newTargets)
      log.debug("New target paths:" + getTargetPaths())
  }

  /*
   * It parses the reading from String to Measurement
   */
  def parse(line: String): Measurement = {
    log.debug("To create Measurement for==>" + line)
    val ms = line
      .split(",")
      .map(_.trim())

    val time =
      LocalDateTime.parse(ms(3), formatterTime)

    val zonedTime = ZonedDateTime.of(time, ZoneId.systemDefault());
    val instant: Instant = Instant.from(zonedTime)

    log.debug("The ms(1) " + ms(1))
    log.debug("The ms(5) " + ms(5))
    log.debug("The ms(6) " + ms(6))
    log.debug("The ms(0) " + ms(0))
    log.debug("The ms(2) " + ms(2))
    log.debug("The ms(4) " + ms(4).toDouble)
    log.debug(
      "The ms(4) to Long OK" + BigDecimal(ms(4))
        .setScale(0, BigDecimal.RoundingMode.HALF_UP)
        .toLong
    )
    log.debug("The time " + time.format(formatterTime))

    val msmnt = Measurement(
      nodeName,
      ms(1),
      ms(2),
      instant.getEpochSecond,
      instant.getNano,
      ms(4).toDouble,
      ms(5),
      ms(6),
      nodeIp
    )
    log.info("Measurement created:==>" + msmnt)
    msmnt
  }

  /*
   * Record the measurement
   */
  def record(m: Measurement) {
    //if (view.find{_.metricName == measurement.metricName} == Option.empty)
    view += m
    //TODO store local view persistently
    
  }

  /*
   * Consolidate measurements
   */

  def consolidate() = {
    log.debug("Starting consolidation")
    val currentView = view.toSeq
    context.actorSelection(localProcessorPath) ! currentView
    view.clear()
    // log.info(
    //   "Message sent for consolidation " + localProcessorPath + " --> " + currentView
    // )
  }

  /*
   * Report measurements
   */

  def report(cView: Seq[Measurement]) = {
    if (!cView.isEmpty) {
      log.info("targetPaths:" + getTargetPaths())

      // targetPaths.foreach { tp =>
      //   context.actorSelection(tp) ! AggregateLocalView(cView)
      //   log.info("Message sent to aggregator:" + tp + " --> " + cView)
      // }
      val message = AggregateLocalView(cView)
      // self ! ForwardingMessages.ForwardedMessage(
      //   Random.nextLong(),
      //   Constants.messageForwardingInitialTtl,
      //   0,
      //   message
      // )

      getTargetPaths().foreach( tp => 
        // log.info(s"Target path: $tp")
        sendTransportMessage(message, tp.address.host.getOrElse(ClusteringConfig.nodeIp), digitalAssetRef, ClusteringConfig.nodeIp)
      )

    } else {
      log.info("Nothing to report to SM...")
    }
    system.scheduler.scheduleOnce(
      consolidateAtSeconds.seconds,
      self,
      Consolidate
    )

  }

  override def transportBehaviour: PartialFunction[Any,Unit] = PartialFunction.empty
  override def transportPrestart() = {}
}
