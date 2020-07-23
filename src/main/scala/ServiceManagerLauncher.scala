package org.ngcdi.sckl

import akka.actor._
import org.ngcdi.sckl.msgs._
import com.typesafe.config.ConfigFactory
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.duration._

object ServiceManagerLauncher {


  def main(args: Array[String] ) = {

    import ClusteringConfig._
    import Constants._
    import Config._

    val port = nodePort
    val config = ConfigFactory.parseString(s"""
        akka.remote.netty.tcp.port=$port
        """)
      //.withFallback(ConfigFactory.parseString("akka.cluster.roles = ["+serviceManagerName+"]"))
      .withFallback(ConfigFactory.load())

    implicit val system = ActorSystem(clusterName, config)

    val serviceManager  = system.actorOf(
      ServiceManagerSimple.props(args(0)), name = serviceManagerName
    )

    if(nnodes != ""){
      serviceManager ! SMReady(nnodes.toInt)
    }

  }
}
