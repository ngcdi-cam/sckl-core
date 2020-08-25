package org.vca

import org.ngcdi.sckl.ClusteringConfig._
import org.ngcdi.sckl.Config._
import org.ngcdi.sckl.Constants._

import akka.stream.ActorMaterializer
import akka.actor.Actor.Receive
import akka.http.scaladsl.model.ResponseEntity

import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer


/*
* Abstraction for all connection behaviours e.g. simple remoting or clustering.
*/
trait ConnectionBehaviour {
  def connPreStart(): Unit = {}
  val connBehaviour: Receive = Map.empty
}
