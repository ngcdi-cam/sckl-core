package org.ngcdi.sckl.awareness

import java.io._
import akka.actor._
import scala.util.{Success, Failure}

object SerializeDemo {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val executionContext = system.getDispatcher
    val log = system.log

    val manager = new AwarenessManager(Seq("http://172.18.0.2:8080", "http://172.18.0.3:8080"))
    manager.init.onComplete {
      case Success(value) =>
        log.info("" + manager)
        log.info("" + manager.controllers)
        log.info("" + manager.controllers(0).topology)
        val oos =
          new ObjectOutputStream(new FileOutputStream("/tmp/manager.bin"))
        log.info("type " + manager.controllers(0).topology.switches(0).ports.getClass())
        log.info("type " + manager.topology.switches(0).ports.getClass())
        log.info("switches: " + manager.topology.switches.toBuffer)
        oos.writeObject(manager)
        oos.close

        // manager.getStats(0).onComplete {
        //   case Success(value) => 
        //     log.info("" + value)
        //   case Failure(exception) => 
        //     log.error("" + exception)
        // }

        val ois = new ObjectInputStream(new FileInputStream("/tmp/manager.bin"))
        val newManager = ois.readObject.asInstanceOf[AwarenessManager]
        ois.close

        log.info("" + newManager)
        log.info("" + newManager.controllers)
        log.info("" + newManager.controllers(0).topology)
        newManager.controllers(0).client.getStats.onComplete {
          case Success(value) => 
            log.info("" + value)
        }
        // newManager.getStats(0).onComplete {
        //   case Success(value) => 
        //     log.info("" + value)
        // }
      case Failure(exception) =>

    }
  }
}
