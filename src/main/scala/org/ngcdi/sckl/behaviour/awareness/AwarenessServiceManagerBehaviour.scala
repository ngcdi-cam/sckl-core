package org.ngcdi.sckl.behaviour.awareness

import org.ngcdi.sckl.ScklActor
import org.ngcdi.sckl.behaviour.NetworkAwarenessManagerReceiverBehaviour
import scala.concurrent.Future
import org.ngcdi.sckl.Constants
import org.ngcdi.sckl.ryuclient.NetworkAwarenessUtils
import org.ngcdi.sckl.behaviour.FlowCongestionDetected
import org.ngcdi.sckl.ryuclient.NetworkAwarenessCrossDomainOptimizer

trait AwarenessServiceManagerBehaviour
    extends NetworkAwarenessSwitchProvider
    with NetworkAwarenessManagerReceiverBehaviour
    with AwarenessManagedServicesProvider
    with AwarenessManagedGlobalServicesProvider
    with AwarenessCrossDomainOptimizerProvider {

  this: ScklActor =>

  def awarenessServiceManagerPrestart(): Future[Unit] = {
    for {
      manager <- awarenessManager
      switch <- awarenessSwitch
      _ <- Future {
        val optimizer = new NetworkAwarenessCrossDomainOptimizer(
          Constants.awarenessOptimalKPaths,
          Constants.awarenessOptimalK2Paths,
          Constants.awarenessEnabledMetrics,
          Constants.awarenessDefaultMetricWeights
        )(manager, executionContext)
        setAwarenessCrossDomainOptimizer(optimizer)

        val controllerId = switch.controllerId
        val hosts = manager.controllers(controllerId).topology.hosts
        log.info("Hosts are " + hosts)
        val hostIps = hosts.filter(_.switch == switch).map(_.ip).toSeq
        val managedServices = hostIps.flatMap(
          NetworkAwarenessUtils.lookUpServiceByIp(
            Constants.awarenessServices,
            _
          )
        )
        setAwarenessManagedServices(managedServices)

        val globalHosts = manager.topology.hosts
        log.info("Global hosts are " + globalHosts)
        val globalHostIps = globalHosts.filter(_.switch == switch).map(_.ip).toSeq
        val managedGlobalServices = globalHostIps.flatMap(
          NetworkAwarenessUtils.lookUpServiceByIp(
            Constants.awarenessServices,
            _,
            true // only the head of the path takes the role of optimizing the cross domain paths
          )
        )

        if (managedServices.size > 0) {
          log.info(
            s"I'm an awareness service manager, host IPs: $hostIps, managed services: $managedServices"
          )
        } else {
          log.info("I'm not an awareness service manager")
        }

        if (managedGlobalServices.size > 0) {
          log.info(
            s"i'm an awareness global service manager, host IPs: $globalHostIps, managed services: $managedGlobalServices"
          )
        } else {
          log.info("i'm not an awareness global service manager")
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
          globalServices <- awarenessManagedGlobalServices
          optimizer <- awarenessCrossDomainOptimizer
          _ <- Future {
            val controllerId = switch.controllerId
            flows.foreach { flow =>
              val targetServices = NetworkAwarenessUtils
                .lookUpServiceBySrcDstIp(services, flow.src_ip, flow.dst_ip)
              log.info("Services to install: " + targetServices)
              manager.installServices(targetServices, controllerId)
            }
          }
          _ <- Future.sequence(globalServices.map(optimizer.optimize(_)))
        } yield Unit
      }
  }

}
