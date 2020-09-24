package org.ngcdi.sckl

import akka.actor._
import com.typesafe.config.ConfigFactory
import org.ngcdi.sckl.sim.SimAsset
import org.slf4j.LoggerFactory

object DigiAssetLauncher {

  val log = LoggerFactory.getLogger("org.ngcdi.sckl.Digiassetlauncher")

  def main(args: Array[String]) = {

    import ClusteringConfig._
    import Constants._

    val port = nodePort
    val config = ConfigFactory
      .parseString(s"""
        akka.remote.netty.tcp.port=$port
        """)
      .withFallback(ConfigFactory.load())

    val system = ActorSystem(clusterName, config)

    // Creates actor simulating the asset
    val simAsset = system.actorOf(
      Props(
        classOf[SimAsset],
        "/data/" + nodeIp + "-in.csv",
        Seq(Temperature, CPU_USG, RAM_USG, SYS_ERR, OPT_POW),
        10,
        "logs/" + nodeIp + ".csv"
      ),
      name = "sim-" + nodeIp
    )

    val lProcessor =
      system.actorOf(Props(classOf[LocalProcessor]), name = "localProcessor")

    println("args:" + args(0))

    if (args(0) != null) {
      log.info(s"Launching Digital Asset with args ${args(0)}")

      args(0) match {
        case _ =>
          val digiAsset =
            system.actorOf(
              DigitalAsset.props(
                args(0),
                "DA-" + nodeName,
                lProcessor
              ),
              digitalAssetName
            )

          log.info(digiAsset.toString())
      }
    }
  }

}
