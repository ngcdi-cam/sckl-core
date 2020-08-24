package org.ngcdi.sckl.ryuclient

import akka.actor.ActorSystem
import scala.concurrent.Future

object NetworkAwarenessClientDemo {
    def main(args: Array[String]): Unit = {
        implicit val system = ActorSystem()
        implicit val executionContext = system.getDispatcher
        val log = system.log
        
        val client: NetworkAwarenessClient = new NetworkAwarenessClient("http://localhost:8080")
        
        (for {
            stats <- client.getStats
            _ <- Future { log.info("Stats: " + stats) }
            links <- client.getLinks
            _ <- Future { log.info("Links: " + links) }
            switchWeightsOld <- client.getSwitchWeights
            _ <- Future { log.info("Switch weights: " + switchWeightsOld) }
            success1 <- client.setSwitchWeights(Map(1 -> 2.5, 3 -> 1.2))
            switchWeightsNew <- client.getSwitchWeights
            _ <- Future { log.info("New switch weights: " + switchWeightsNew)}
            defaultMetricWeightsOld <- client.getDefaultMetricWeights
            _ <- Future { log.info("Default metric weights: " + defaultMetricWeightsOld) }
            success2 <- client.setDefaultMetricWeights(Map("free_bandwidth" -> 0.9, "delay" -> 1.1))
            defaultMetricWeightsNew <- client.getDefaultMetricWeights
            _ <- Future { log.info("New default metric weights: " + defaultMetricWeightsNew) }
        } yield links).foreach { links => 
            // val topo = NetworkAwarenessTopology.fromStats(stats, 0)
            val topo = NetworkAwarenessTopology.fromLinks(links, 0)
            val switch1 = topo.switches.get(1).get
            val switch2 = topo.switches.get(2).get

            log.info("Switch 1: " + switch1)
            log.info("Switch 1 Peers: " + switch1.getPeers)

            log.info("Switch 2: " + switch2)
            log.info("Switch 2 Peers: " + switch2.getPeers)
        }
    }
}
