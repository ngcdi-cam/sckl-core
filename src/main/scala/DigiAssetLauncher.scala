package org.ngcdi.sckl

import akka.actor._
import com.typesafe.config.ConfigFactory
import org.ngcdi.sckl.sim.SimAsset
import org.slf4j.LoggerFactory


object DigiAssetLauncher {

  val log = LoggerFactory.getLogger("org.ngcdi.sckl.Digiassetlauncher")

def main(args:Array[String]) = {

  import ClusteringConfig._
  import Constants._



  val port = nodePort
  val config = ConfigFactory.parseString(s"""
        akka.remote.netty.tcp.port=$port
        """)
//    .withFallback(ConfigFactory.parseString("akka.cluster.roles = ["+digitalAssetName+"]"))
    .withFallback(ConfigFactory.load())


  val system = ActorSystem(clusterName, config)

  // Creates actor simulating the asset
  val simAsset = system.actorOf(
    Props(classOf[SimAsset],
      "/data/"+nodeIp+"-in.csv",
      //Seq(Temperature, ThroughputIn, ThroughputOut),
      Seq(Temperature, CPU_USG, RAM_USG, SYS_ERR, OPT_POW),
      10,
      "logs/"+nodeIp+".csv"),name = "sim-"+nodeIp)

  //println(nodeName+"--->"+simAsset.path)
  // Initially, the container with the digitalAsset agent is inactive and later on activated by FunctionProvisioner when required.
  //TODO Ideally the container should be created on demand, with the digitalAsset agent active.

 // val lView = system.actorOf(
 //   Props(classOf[LocalView], 20  // 20 seconds to report
 // ), name = "localView")
  //val lFilter = system.actorOf(Props[LocalFilter], name = "localFilter")
  val lProcessor = system.actorOf(
    Props(classOf[LocalProcessor]
      ), name = "localProcessor")

  println("args:"+args(0))

  if(args(0)!= null){


    args(0) match {

      case _ =>

        val digiAsset =
          system.actorOf(DigitalAsset.props(
            args(0),
            "DA-"+nodeName,
            lProcessor
          ), digitalAssetName
          )

        log.info(digiAsset.toString())

    }


  }
  //system.scheduler.scheduleOnce(10 seconds, digiAsset, Sense)
  //sys.addShutdownHook(system.terminate())

  }





}
