package org.ngcdi.sckl.ryuclient

import akka.actor.ActorSystem

object NetworkAwarenessManagerDemo {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val executionContext = system.getDispatcher
    val log = system.log

    val baseUrl = "http://127.0.0.1:8080"
    val manager = new NetworkAwarenessManager(Seq(baseUrl))
    manager.init.foreach { _ =>
      log.info("Manager is " + manager.controllers)
    }
  }
}
