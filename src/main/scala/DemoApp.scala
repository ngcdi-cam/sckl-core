package org.ngcdi.sckl

import kamon.prometheus._
import kamon._
//import kamon.system.SystemMetrics
import akka.actor.ActorSystem
import kamon.Kamon
//import kamon
import org.ngcdi._

object DemoApp {

  def main(args: Array[String]): Unit = {

    import ClusteringConfig._

    Kamon.registerModule("prom-reporter", new PrometheusReporter());

    launcher match {
      case "serviceManagerIntent" =>
        ServiceManagerLauncher.main(Array("intent+file"))
      case "serviceManager" =>
        ServiceManagerLauncher.main(Array("port+file"))
      case "digiasset" =>
        DigiAssetLauncher.main(Array("port+file"))
      case "digiasset-intent" =>
        DigiAssetLauncher.main(Array("intent+file"))
      case "digiAssetStandalone" =>
        DigiAssetLauncher.main(Array("standalone"))
      case "digiAssetCustomArg" =>
        DigiAssetLauncher.main(Array(ClusteringConfig.digiAssetLauncher))
      case "functionProvisioner" =>
        FunctionProvisionerLauncher.main(Array.empty)
      case _ =>
        println(s"Invalid launcher specified: ${launcher}")
    }
  }

}
