package org.ngcdi.sckl.awareness

import akka.actor.ActorSystem
import scala.util.Success
import scala.util.Failure
import org.ngcdi.sckl.Constants

object AwarenessCrossDomainOptimizerDemo {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val executionContext = system.getDispatcher
    // val log = system.log

    val baseUrls = Seq(
      "http://172.18.0.2:8080",
      "http://172.18.0.3:8080"
    )

    implicit val manager = new AwarenessManager(baseUrls)

    val optimizer = new AwarenessCrossDomainOptimizer(
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
