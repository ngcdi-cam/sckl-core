package org.ngcdi.sckl

import akka.actor._
import org.ngcdi.sckl.msgs._
import com.typesafe.config.ConfigFactory
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.duration._

object FunctionProvisionerLauncher  {


  def main(args: Array[String] ) = {

    import ClusteringConfig._
    import Constants._

    val port = nodePort
    val config = ConfigFactory.parseString(s"""
        akka.remote.netty.tcp.port=$port
        """)
      .withFallback(ConfigFactory.parseString("akka.cluster.roles = ["+functionProvisionerName+"]"))
      .withFallback(ConfigFactory.load())

    implicit val system = ActorSystem(clusterName, config)

    val functionProvisioner = system.actorOf(Props[FunctionProvisioner], name = functionProvisionerName)

    //system.scheduler.scheduleOnce(5 seconds, functionProvisioner, FPReady )
    functionProvisioner ! FPReady

  }
}
