package org.ngcdi.sckl

import org.ngcdi.sckl.ClusteringConfig._
import org.ngcdi.sckl.Config._
import org.ngcdi.sckl.Constants._

import akka.actor._

trait Actuator {
  this: ScklActor =>

  def triggerAction(args:String,ResultAD:Option[Int],details:Seq[String])
}