package org.ngcdi.sckl.ryu

import akka.actor.ActorSystem
import scala.concurrent.Future
import scala.util.{ Success, Failure }

object RyuStatsClientDemo {
    def main(args: Array[String]): Unit = {
        implicit val system = ActorSystem()
        implicit val executionContext = system.getDispatcher
        val log = system.log
        
        val client = new RyuStatsClient("http://172.18.0.2:8080")
        
        (for {
            stats <- client.getSwitchStatsDelta(1)
            _ <- Future { 
              log.info("Stats: " + stats)
            }
        } yield stats).onComplete { 
          case Success(links) => 
          case Failure(exception) =>
            log.error("Error: " + exception)
        }
    }
}
