package org.ngcdi.sckl

import akka.serialization._
import org.rosuda.REngine._
import org.rosuda.REngine.Rserve._
import org.ngcdi.sckl.Constants._
import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory


object RUtils{
  val log = LoggerFactory.getLogger("org.ngcdi.sckl.RUtils")

  def initialiseConnection():RConnection={
    val c = new RConnection(rserver,rport)
    log.info("R Initialisation OK")
    c
  }

  def initialiseState(labelAD:String,c:RConnection):Unit={

    val initialState:Array[Byte] = c.eval(
      "w_initial_anom("+
        median +","+
        sd +","+
        minseglength +","+
        maxseglength +","+
        lambda +")"
    ).asBytes()
    c.assign("st"+labelAD, new REXPRaw(initialState));
  }

  def update(labelAD:String,newdata:Seq[Double],serialization:Serialization,c:RConnection):AnomLoc = {
    log.debug("===>BEFORE Starting")
     //val sample:Array[Double] = List(94.00, 96.00, 90.00, 90.00).toArray
    //val sample:Array[Double] = Array(94, 96, 90, 90)
    //val ssample = serialization.serialize(sample)
    //val sample = 10

    //val dwrapper:DWrapper = new DWrapper(sample)
    //val ser = serialization.serialize(sample).get

    //c.assign("ser", new REXPRaw(ser));

    val newDataLabel = "nd"+labelAD
    val stateLabel = "st"+labelAD

    c.assign(newDataLabel, new REXPDouble(newdata.toArray))

    val state = c.eval(
      "w_update_anom("+stateLabel+", "+newDataLabel+")"
    ).asBytes()

    c.assign(stateLabel, new REXPRaw(state))

    log.debug("===>COMPLETED 1ST CALL")
    val anomLoc = c.eval(
      "w_print_anom(state)"
    ).asBytes()
    log.debug("===>COMPLETED 2ND CALL")
    val result =  serialization.deserialize(anomLoc, classOf[org.ngcdi.sckl.AnomLoc]).get
    result
  }




}
