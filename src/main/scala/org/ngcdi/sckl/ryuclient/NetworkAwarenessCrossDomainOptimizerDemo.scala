package org.ngcdi.sckl.ryuclient

import akka.actor.ActorSystem
import scala.util.Success
import scala.util.Failure
import org.ngcdi.sckl.Constants

object NetworkAwarenessCrossDomainOptimizerDemo {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val executionContext = system.getDispatcher
    val log = system.log

    val baseUrls = Seq(
      "http://127.0.0.1:8080",
      "http://127.0.0.1:8081"
    )

    implicit val manager = new NetworkAwarenessManager(baseUrls)

    val optimizer = new NetworkAwarenessCrossDomainOptimizer(
      Constants.awarenessOptimalKPaths,
      Constants.awarenessOptimalK2Paths,
      Constants.awarenessEnabledMetrics,
      Constants.awarenessDefaultMetricWeights
    )

    val service = Constants.awarenessServices(0)

    manager.init.onComplete {
      case Success(_) =>
        optimizer.optimize(service).onComplete {
          case Failure(exception) => 
            exception.printStackTrace()
          case Success(value) => 

        }
      
      case Failure(exception) =>
        exception.printStackTrace()
    }
  }
}
