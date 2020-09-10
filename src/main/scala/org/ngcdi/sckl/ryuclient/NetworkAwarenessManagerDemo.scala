package org.ngcdi.sckl.ryuclient

import akka.actor.ActorSystem
import scala.util.Success
import scala.util.Failure

object NetworkAwarenessManagerDemo {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val executionContext = system.getDispatcher
    val log = system.log

    val baseUrls = Seq(
      "http://127.0.0.1:8080",
      "http://127.0.0.1:8081",
    )

    val manager = new NetworkAwarenessManager(baseUrls)
    manager.init.onComplete {
      case Success(_) =>
        log.info("Topology switches are " + manager.topology.switches)
        log.info("Hosts are " + manager.topology.hosts)
        log.info("Switch 1 Peers = " + manager.getSwitchById(1, 0).get.getPeers.toBuffer)
      case Failure(exception) => 
        exception.printStackTrace()
    }
  }
}
