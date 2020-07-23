package org.ngcdi.sckl

import kamon.prometheus._
import kamon._
//import kamon.system.SystemMetrics

import kamon.Kamon
//import kamon
import org.ngcdi._

object DemoApp {

  def main(args: Array[String]): Unit = {

    import ClusteringConfig._

//    SystemMetrics.startCollecting()
//Kamon.addReporter(new PrometheusReporter())
    Kamon.registerModule("prom-reporter", new PrometheusReporter());
  //  val customConfig = ConfigFactory.load("custom-config")
  //  val codeConfig = ConfigFactory.parseString("kamon.metric.tick-interval = 15 seconds")

    // Kamon gets reconfigured with the provided configuration.
   // Kamon.reconfigure(codeConfig.withFallback(customConfig))
 //   Kamon.init()

    launcher match{
      case "serviceManagerIntent" =>
        ServiceManagerLauncher.main(Array("intent+file"))
      case "serviceManager" =>
        ServiceManagerLauncher.main(Array("port+file"))
      case "digiasset" =>
          DigiAssetLauncher.main(Array("port+file"))
      //case "digiassetd" =>
      //  DigiAssetLauncher.main(Array("DigitalAssetD"))
      //case "digiasseti" =>
      //  DigiAssetLauncher.main(Array("DigitalAssetI"))
      case "digiasset-intent" =>
        DigiAssetLauncher.main(Array("intent+file"))
      case "functionProvisioner" =>
        FunctionProvisionerLauncher.main(Array.empty)
//      case "TestServer" =>
//        QuickstartServer.main(Array.empty)
//      case "TestClient" =>
//       Thread.sleep(10000)
//       Client.main(Array.empty)

    }



  }


}
