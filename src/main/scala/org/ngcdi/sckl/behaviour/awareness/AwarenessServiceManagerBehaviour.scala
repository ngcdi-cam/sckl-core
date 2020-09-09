package org.ngcdi.sckl.behaviour.awareness

import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.behaviour.NetworkAwarenessManagerReceiverBehaviour
import scala.concurrent.Future
import org.ngcdi.sckl.Constants
import org.ngcdi.sckl.ryuclient.NetworkAwarenessUtils
import org.ngcdi.sckl.behaviour.FlowCongestionDetected

trait AwarenessServiceManagerBehaviour
    extends NetworkAwarenessSwitchProvider
    with NetworkAwarenessManagerReceiverBehaviour
    with AwarenessManagedServicesProvider {

  this: ScklActor =>

  def awarenessServiceManagerPrestart(): Future[Unit] = {
    for {
      manager <- awarenessManager
      switch <- awarenessSwitch
      _ <- Future {
        val controllerId = switch.controllerId
        val hosts = manager.controllers(controllerId).topology.hosts
        // val accessTable = manager.controllers(controllerId).accessTable
        log.info("Hosts are " + hosts)
        val hostIps = hosts.filter(_.link == switch).map(_.ip).toSeq
        val managedServices = hostIps.flatMap(
          NetworkAwarenessUtils.lookUpServiceByIp(
            Constants.awarenessServices,
            _
          )
        )
        setAwarenessManagedServices(managedServices)

        if (hostIps.size > 0) {
          log.info(
            s"I'm an awareness service manager, host ips: $hostIps, managed services: $managedServices"
          )
        } else {
          log.info("I'm not an awareness service manager")
        }
      }
    } yield Unit
  }

  def awarenessServiceManagerBehaviour(): Receive = {
    case FlowCongestionDetected(srcIpDpid, flows) =>
      if (!awarenessManagedServices.isCompleted) {
        log.warning(
          "Received FlowCongestionDetected, but no service is managed by this instance now"
        )
      } else {
        log.info(s"Received FlowCongestionDetected from $srcIpDpid")
        for {
          manager <- awarenessManager
          switch <- awarenessSwitch
          services <- awarenessManagedServices
          _ <- Future {
            val controllerId = switch.controllerId
            flows.foreach { flow =>
              val targetServices = NetworkAwarenessUtils
                .lookUpServiceBySrcDstIp(services, flow.src_ip, flow.dst_ip)
              log.info("Services to install: " + targetServices)
              manager.installServices(targetServices, controllerId)
            }
          }
        } yield Unit
      }
  }

}
