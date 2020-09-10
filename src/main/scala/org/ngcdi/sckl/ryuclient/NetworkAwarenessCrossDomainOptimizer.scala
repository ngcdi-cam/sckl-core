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
    kPaths: Int,
    k2Paths: Int,
    enabledMetrics: Seq[String],
    defaultMetricWeights: Map[String, Double]
)(implicit
    manager: NetworkAwarenessManager,
    ec: ExecutionContext
) {

  private lazy val rawDomainConnectivityGraph =
    manager.topology.getDomainConnectivityGraph()

  private lazy val domainConnectivityGraph: Graph[Int, DefaultEdge] = {
    val graph =
      new DefaultUndirectedGraph[Int, DefaultEdge](new DefaultEdge().getClass())
    (0 until manager.controllers.size).foreach(graph.addVertex(_))
    rawDomainConnectivityGraph.keySet.foreach { edge =>
      graph.addEdge(edge._1, edge._2)
    }
    graph
  }

  // private lazy val graph: Graph[NetworkAwarenessSwitch, DefaultEdge] = {

  // val intraDomainLinks = manager.topology.switches
  //   .groupBy(_.controllerId)
  //   .map {
  //     case (controllerId, switches) =>
  //       Tuple2(
  //         controllerId,
  //         switches.flatMap { x =>
  //           switches.map(Tuple2(x, _))
  //         }
  //       )
  //   }
  //   .toMap

  // val interDomainLinks = manager.topology.getEdgeLinks
  // val allLinks = intraDomainLinks.values.flatten.toSeq ++ interDomainLinks
  // val pregraph =
  //   new DefaultUndirectedGraph[NetworkAwarenessSwitch, DefaultEdge](
  //     new DefaultEdge().getClass()
  //   )

  // manager.topology.switches.foreach(pregraph.addVertex(_))
  // allLinks.foreach { x => pregraph.addEdge(x._1, x._2) }
  // pregraph
  // }

  private def getCandidatePaths(
      src: NetworkAwarenessSwitch,
      dst: NetworkAwarenessSwitch,
      k: Int
  ): Seq[GraphPath[Int, DefaultEdge]] = {
    val algo = new YenKShortestPath(domainConnectivityGraph)
    val pathsList = algo.getPaths(src.controllerId, dst.controllerId, k)
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
    path.map(_.metrics.getOrElse(metric, Double.PositiveInfinity)).min
  }

  private def getPathTotalMetricValue(
      path: Seq[PathStats],
      metric: String
  ): Double = {
    path.map(_.metrics.getOrElse(metric, 0.0)).sum
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
      val metricScore =
        metricScoreFunctions.get(metricName).get.apply(metricValue)
      metricScore * weight
    }.sum

    val switchTotalScore = path.map { link =>
      edgeSwitchWeights.getOrElse(link.src, 1.0) * edgeSwitchWeights.getOrElse(link.dst, 1.0)
    }.product

    metricTotalScore * switchTotalScore
  }

  // Get the optimal path among candidate paths for service
  private def getOptimalPathInternal(
      pathStatsMap: Map[
        Tuple2[NetworkAwarenessSwitch, NetworkAwarenessSwitch],
        PathStats
      ],
      paths: Seq[Seq[Tuple2[
        Tuple2[NetworkAwarenessSwitch, Int],
        Tuple2[NetworkAwarenessSwitch, Int]
      ]]],
      service: NetworkAwarenessService,
      edgeSwitchWeights: Map[NetworkAwarenessSwitch, Double]
  ): Seq[Tuple2[
    Tuple2[NetworkAwarenessSwitch, Int],
    Tuple2[NetworkAwarenessSwitch, Int]
  ]] = {
    val weights = defaultMetricWeights ++ service.weights
    paths.maxBy({ path =>
      val pathWithStats = path.map { link =>
        pathStatsMap.get(Tuple2(link._1._1, link._2._1)).get
      }
      getPathScore(pathWithStats, weights, edgeSwitchWeights)
    })
  }

  // Get the optimal path from src to dst for service
  def getOptimalPath(
      src: NetworkAwarenessSwitch,
      dst: NetworkAwarenessSwitch,
      service: NetworkAwarenessService,
      k: Int,
      k2: Int
  )(implicit
      actorSystem: ActorSystem
  ): Future[Seq[NetworkAwarenessSwitchLink]] = {
    // actorSystem.log.info(s"src is $src, dst is $dst")

    // inter-domain links
    val paths = getCandidatePaths(src, dst, k).flatMap { path =>
      val controllers =
        JavaConverters.asScalaIterator(path.getVertexList().iterator()).toSeq
      // actorSystem.log.info(s"[PATH] ${controllers.toBuffer}")

      var switchLinks = Seq(Seq.empty[NetworkAwarenessSwitchLink])
      controllers
        .dropRight(1)
        .zip(controllers.drop(1))
        .foreach { // TODO: use reduce
          controllerLink =>
            // actorSystem.log.info(s"controllerLink: $controllerLink")
            val localSwitchLinks =
              rawDomainConnectivityGraph.get(controllerLink).get.take(k2)
            // actorSystem.log.info(s"localSwitchLinks: ${localSwitchLinks.toBuffer}")
            switchLinks = switchLinks
              .flatMap { path => localSwitchLinks.map(path :+ (_)) }
              .take(k2)
        }

      switchLinks

    // switches.dropRight(1).zip(switches.drop(1)).filter {
    //   // get intra-domain links
    //   case (pathSrc, pathDst) =>
    //     pathSrc.controllerId == pathDst.controllerId
    // }.toBuffer
    }

    actorSystem.log.info(s"Candidate paths: ${paths.toBuffer}")

    val intraDomainPaths = // : Seq[Seq[Tuple2[NetworkAwarenessSwitch, Int]]]
      paths.map { origPath =>
        val path = (origPath.flatMap { link =>
          Seq(Tuple2(link.src, link.srcPort), Tuple2(link.dst, link.dstPort))
        } :+ Tuple2(dst, -1)).+:(Tuple2(src, -1))
        assert(path.length % 2 == 0)
        path.grouped(2).map { x => Tuple2(x(0), x(1)) }.toBuffer // TODO: toSeq
      }

    actorSystem.log.info(s"IntraDomainPaths: ${intraDomainPaths.toBuffer}")

    val intraDomainLinks = intraDomainPaths.flatten
      .map { x =>
        Tuple2(x._1._1, x._2._1)
      }
      .distinct
      .groupBy(_._1.controllerId)

    Future
      .sequence(intraDomainLinks.map {
        case (controllerId, links) =>
          manager
            .getPathInfo(links, service.weights, service.src, service.dst)
            .map { x => Tuple3(controllerId, links, x) }
      })
      .map { responses =>
        val edgeSwitchWeights = responses.flatMap { x =>
          x._3.switch_weights.map { y =>
            Tuple2(manager.topology.getSwitchById(y._1, x._1).get, y._2)
          }.toMap
        }.toMap

        val pathStatsMap = responses.flatMap {
          case (controllerId, links, stats) =>
            links.zip(stats.stats).map { x =>
              Tuple2(x._1, PathStats(x._1._1, x._1._2, x._2.metrics))
            }
        }.toMap

        val rawPath = getOptimalPathInternal(
          pathStatsMap,
          intraDomainPaths,
          service,
          edgeSwitchWeights
        )
        rawPath
          .flatMap { link => Seq(link._1, link._2) }
          .drop(1)
          .dropRight(1)
          .grouped(2)
          .map { link =>
            assert(link.length == 2)
            val src = link(0)
            val dst = link(1)
            NetworkAwarenessSwitchLink(src._1, dst._1, src._2, dst._2)
          }
          .toSeq
      }

    // manager
    //   .getPathInfo(Seq.empty, service.weights, service.src, service.dst)
    //   .map { pathInfos =>
    //     val pathInfosZipped = intraDomainLinks.zip(pathInfos.stats)
    //     // val edgeSwitchWeights = pathInfos..flatMap { x =>
    //     //   Map(
    //     //     x._1._1 -> x._2.switch_weights.get(x._1._1.dpid).get,
    //     //     x._1._2 -> x._2.switch_weights.get(x._1._2.dpid).get
    //     //   )
    //     // }.toMap
    //     val edgeSwitchWeights = Map.empty

    //     val pathStatsMap = pathInfosZipped.map { path =>
    //       Tuple2(
    //         path._1,
    //         PathStats(path._1._1, path._1._2, path._2.stats.metrics)
    //       )
    //     }.toMap

    //     // val pathsStats = intraDomainPaths.map(_.map(pathStatsMap.get(_).get))

    //     val rawPath = getOptimalPathInternal(
    //       pathStatsMap,
    //       intraDomainPaths,
    //       service,
    //       edgeSwitchWeights
    //     )
    //     rawPath
    //       .flatMap { link => Seq(link._1, link._2) }
    //       .drop(1)
    //       .dropRight(1)
    //       .grouped(2)
    //       .map { link =>
    //         assert(link.length == 2)
    //         val src = link(0)
    //         val dst = link(1)
    //         NetworkAwarenessSwitchLink(src._1, dst._1, src._2, dst._2)
    //       }
    //       .toSeq
    //   }
  }

  def optimize(service: NetworkAwarenessService)(implicit
      actorSystem: ActorSystem
  ): Future[Unit] = {
    val srcHost = manager.topology.getHostByIp(service.src).get
    val dstHost = manager.topology.getHostByIp(service.dst).get
    val srcSwitch = srcHost.switch
    val dstSwitch = dstHost.switch
    getOptimalPath(srcSwitch, dstSwitch, service, kPaths, k2Paths).map {
      interDomainLinks =>
        val pinnings = interDomainLinks
          .flatMap {
            case NetworkAwarenessSwitchLink(
                  srcSwitch,
                  dstSwitch,
                  srcPort,
                  dstPort
                ) =>
              Seq(
                Tuple2(
                  dstSwitch.controllerId,
                  NetworkAwarenessRawAccessTableEntryPinning(
                    service.src,
                    dstSwitch.dpid,
                    dstPort
                  )
                ),
                Tuple2(
                  srcSwitch.controllerId,
                  NetworkAwarenessRawAccessTableEntryPinning(
                    service.dst,
                    srcSwitch.dpid,
                    srcPort
                  )
                )
              )
          }
          .groupBy(_._1)
          .map {
            case (controllerId, pinnings) =>
              actorSystem.log.info(
                s"Updating pinnings of ctrl $controllerId: ${pinnings.toBuffer}"
              )
              manager
                .controllers(controllerId)
                .client
                .setAccessTableEntryPinning(pinnings.map(_._2))
          }

        Future.sequence(pinnings).map { _ => Unit }
    }

  }

}
