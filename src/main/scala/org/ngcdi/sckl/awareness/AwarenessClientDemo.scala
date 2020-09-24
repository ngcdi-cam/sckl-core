package org.ngcdi.sckl.awareness

import akka.actor.ActorSystem
import scala.concurrent.Future
import scala.util.{Success, Failure}
import org.ngcdi.sckl.Constants
import scala.concurrent.ExecutionContext

object AwarenessClientDemo {
  def singleControllerTest(implicit
      system: ActorSystem,
      ec: ExecutionContext
  ) = {
    val log = system.log

    val client = new AwarenessClient(
      "http://172.18.0.2:8080"
    )

    (for {
      accessTable <- client.getAccessTable
      _ <- Future { log.info("Access table: " + accessTable) }
      switchFlows <- client.getSwitchFlows(3)
      _ <- Future { log.info("Flows: " + switchFlows) }
      stats <- client.getStats
      _ <- Future { log.info("Stats: " + stats) }
      switchStats <- client.getSwitchStats(1, true)
      _ <- Future { log.info("Switch 1 stats: " + switchStats) }
      links <- client.getLinks
      _ <- Future { log.info("Links: " + links) }
      switchWeightsOld <- client.getSwitchWeights
      _ <- Future { log.info("Switch weights: " + switchWeightsOld) }
      success1 <- client.setSwitchWeights(Map(1 -> 2.5, 3 -> 1.2))
      switchWeightsNew <- client.getSwitchWeights
      _ <- Future { log.info("New switch weights: " + switchWeightsNew) }
      defaultMetricWeightsOld <- client.getDefaultMetricWeights
      _ <- Future {
        log.info("Default metric weights: " + defaultMetricWeightsOld)
      }
      success2 <- client.setDefaultMetricWeights(
        Map("free_bandwidth" -> 0.9, "delay" -> 1.1)
      )
      defaultMetricWeightsNew <- client.getDefaultMetricWeights
      _ <- Future {
        log.info("New default metric weights: " + defaultMetricWeightsNew)
      }
      success3 <- client.setServices(Constants.awarenessServices)
      servicesRaw <- client.getServices
      _ <- Future { log.info("Services: " + servicesRaw) }
    } yield Tuple2(links, accessTable)).onComplete {
      case Success(Tuple2(links, accessTable)) =>
        // val topo = AwarenessTopology.fromStats(stats, 0)
        val topo = AwarenessTopology(links, accessTable, 0)
        // val accessTable = AwarenessAccessTable(0, accessTableRaw, topo)

        val switch1 = topo.switches(1)
        val switch2 = topo.switches(2)

        log.info("Switch 1: " + switch1)
        log.info("Switch 1 Peers: " + switch1.getNeighbours)

        log.info("Switch 2: " + switch2)
        log.info("Switch 2 Peers: " + switch2.getNeighbours)
        log.info("Access table: " + accessTable)
      case Failure(exception) =>
        log.error("Error: " + exception)
    }

  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val executionContext = system.getDispatcher
    singleControllerTest
    
  }
}
