package org.ngcdi.sckl

import akka.actor._
import akka.serialization._
import org.rosuda.REngine._
import org.rosuda.REngine.Rserve._
import org.ngcdi.sckl.Constants._
import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory
import org.ngcdi.sckl.msgs._

object RUtilsActor {
  def props():Props = Props(new RUtilsActor())
}

class RUtilsActor() extends Actor with ActorLogging{

  var rConnection:RConnection = _
  var serialization:Serialization = _

  override def preStart(): Unit = {
    rConnection = initialiseConnection()
    serialization = SerializationExtension(context.system)
    log.debug("FINISHED PRE-START!!!!")
  }

  def receive = {
    case InitStateAnomD(labelAD:String) =>
      try {
        initialiseState(labelAD)
        sender ! true
      } catch {
        case e: Exception =>
          sender() ! akka.actor.Status.Failure(e)
          throw e
      }

    case UpdateAnomD(tick:Int,labelAD:String,deviceId:String,intent:String,newData:Seq[Double]) =>

      val resultAD:Option[Int] =
        try{
          log.debug("===>Before R call: ---"+tick+"--"+labelAD+"--->"+newData)
          val anomLoc = update(labelAD,newData)
          log.debug("=="+tick+"==="+labelAD+"====> anomState: "+anomLoc.anomstate+" -- pointAnom: "+anomLoc.pointanom+" -- CollAnom: "+anomLoc.collectiveanom)
          Option(anomLoc.anomstate)
        }catch{
          case e:Exception =>
            e.printStackTrace()
            None
        }

      sender ! ResultAnomD(tick,labelAD,deviceId,intent,resultAD)
  }

  def initialiseConnection():RConnection={
    val c = new RConnection(rserver,rport)
    log.info("R Initialisation OK")
    c
  }

  def initialiseState(labelAD:String):Unit={

    val initialState:Array[Byte] = rConnection.eval(
      "w_initial_anom("+
        median +","+
        sd +","+
        minseglength +","+
        maxseglength +","+
        lambda +")"
    ).asBytes()
    rConnection.assign("st"+labelAD, new REXPRaw(initialState));
    log.debug("===>Initialised Correctly: "+"st"+labelAD)
  }

  def update(labelAD:String,newdata:Seq[Double]):AnomLoc = {
    log.debug("=="+labelAD+"===>BEFORE Starting")

    val newDataLabel = "nd"+labelAD
    val stateLabel = "st"+labelAD

    rConnection.assign(newDataLabel, new REXPDouble(newdata.toArray))

    val state = rConnection.eval(
      "w_update_anom("+stateLabel+", "+newDataLabel+")"
    ).asBytes()

    rConnection.assign(stateLabel, new REXPRaw(state))

    log.debug("===>COMPLETED 1ST CALL")
    val anomLoc = rConnection.eval(
      "w_print_anom("+stateLabel+")"
    ).asBytes()
    log.debug("===>COMPLETED 2ND CALL")
    val result =  serialization.deserialize(anomLoc, classOf[org.ngcdi.sckl.AnomLoc]).get
    result
  }


}
