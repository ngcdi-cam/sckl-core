package org.ngcdi.sckl.ryuclient

import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters;

import org.jgrapht._
import org.jgrapht.graph._
import org.jgrapht.alg.shortestpath.YenKShortestPath
import org.ngcdi.sckl.Constants._
import akka.actor.ActorSystem
import scala.concurrent.Future

class NetworkAwarenessCrossDomainOptimizer(
    enabledMetrics: Seq[String],
    defaultMetricWeights: Map[String, Double]
)(implicit
    manager: NetworkAwarenessManager,
    ec: ExecutionContext
) {

  private lazy val graph: Graph[NetworkAwarenessSwitch, DefaultEdge] = {
    val intraDomainLinks = manager.topology.switches
      .groupBy(_.controllerId)
      .map {
        case (controllerId, switches) =>
          Tuple2(
            controllerId,
            switches.flatMap { x =>
              switches.map(Tuple2(x, _))
            }
          )
      }
      .toMap

    val interDomainLinks = manager.topology.getEdgeLinks
    val allLinks = intraDomainLinks.values.flatten.toSeq ++ interDomainLinks
    val pregraph = new DefaultUndirectedGraph[NetworkAwarenessSwitch, DefaultEdge](
      new DefaultEdge().getClass()
    )

    manager.topology.switches.foreach(pregraph.addVertex(_))
    allLinks.foreach { x => pregraph.addEdge(x._1, x._2) }
    pregraph
  }

  private def getCandidatePaths(
      src: NetworkAwarenessSwitch,
      dst: NetworkAwarenessSwitch,
      k: Int
  ): Seq[GraphPath[NetworkAwarenessSwitch, DefaultEdge]] = {

    val algo = new YenKShortestPath(graph)
    val pathsList = algo.getPaths(src, dst, k)
    JavaConverters.asScalaIterator(pathsList.iterator()).toSeq
  }

  private case class PathStats(
      src: NetworkAwarenessSwitch,
      dst: NetworkAwarenessSwitch,
      metrics: Map[String, Double]
  )

  private def getPathBottleneckMetricValue(
      path: Seq[PathStats],
      metric: String
  ): Double = {
    path.map(_.metrics.get(metric).get).min
  }

  private def getPathTotalMetricValue(
      path: Seq[PathStats],
      metric: String
  ): Double = {
    path.map(_.metrics.get(metric).get).sum
  }

  private def getPathScore(
      path: Seq[PathStats],
      weights: Map[String, Double],
      edgeSwitchWeights: Map[NetworkAwarenessSwitch, Double]
  ): Double = {
    val metricRawFunctions = Map[String, Seq[PathStats] => Double](
      awarenessFreeBandwidth -> { x =>
        getPathBottleneckMetricValue(x, awarenessFreeBandwidth)
      },
      awarenessLatency -> { x =>
        getPathTotalMetricValue(x, awarenessLatency)
      },
      awarenessHop -> { x =>
        getPathTotalMetricValue(x, awarenessHop)
      }
    )

    val metricScoreFunctions = Map[String, Double => Double](
      awarenessFreeBandwidth -> (_ / 1000.0),
      awarenessLatency -> { x => math.exp(-1.5 * x) },
      awarenessHop -> { x => math.exp(-0.2 * x) }
    )

    val metricTotalScore = enabledMetrics.map { metricName =>
      val weight = weights.get(metricName).get
      val metricValue = metricRawFunctions.get(metricName).get.apply(path)
      val metricScore = metricScoreFunctions.get(metricName).get.apply(metricValue)
      metricScore * weight
    }.sum

    val switchTotalScore = path.map { link =>
      edgeSwitchWeights.get(link.src).get * edgeSwitchWeights.get(link.dst).get
    }.product

    metricTotalScore * switchTotalScore
  }

  private def getOptimalPathInternal(
      paths: Seq[Seq[PathStats]],
      service: NetworkAwarenessService,
      edgeSwitchWeights: Map[NetworkAwarenessSwitch, Double]
  ): Seq[PathStats] = {
    val weights = defaultMetricWeights ++ service.weights
    paths.maxBy(getPathScore(_, weights, edgeSwitchWeights))
  }

  private def getOptimalPath(
      src: NetworkAwarenessSwitch,
      dst: NetworkAwarenessSwitch,
      service: NetworkAwarenessService,
      k: Int
  )(implicit actorSystem: ActorSystem): Future[Seq[Tuple2[NetworkAwarenessSwitch, NetworkAwarenessSwitch]]] = {
    val paths = getCandidatePaths(src, dst, k).map { path =>
      val switches =
        JavaConverters.asScalaIterator(path.getVertexList().iterator()).toSeq
      switches.dropRight(1).zip(switches.drop(1)).filter {
        // get intra-domain links
        case (pathSrc, pathDst) =>
          pathSrc.controllerId == pathDst.controllerId
      }
    }

    val links = paths.flatten

    Future.sequence(links.map { link => 
      manager.getPathInfo(link._1, link._2)
    }).map { pathInfos => 
      val pathInfosZipped = links.zip(pathInfos)
      val edgeSwitchWeights = pathInfosZipped.flatMap { x => 
        Map(
          x._1._1 -> x._2.switch_weights.get(x._1._1.dpid).get,
          x._1._2 -> x._2.switch_weights.get(x._1._2.dpid).get
        )
      }.toMap

      val pathStatsMap = pathInfosZipped.map { path => 
        Tuple2(path._1, PathStats(path._1._1, path._1._2, path._2.metrics))
      }.toMap

      val pathsStats = paths.map(_.map(pathStatsMap.get(_).get))

      getOptimalPathInternal(pathsStats, service, edgeSwitchWeights).map { x => 
        Tuple2(x.src, x.dst)
      }
    }
  }

  def optimize(service: NetworkAwarenessService) = {
    
  }

}
