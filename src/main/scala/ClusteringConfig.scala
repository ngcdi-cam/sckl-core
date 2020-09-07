package org.ngcdi.sckl

import com.typesafe.config.ConfigFactory

object ClusteringConfig {
  private val config = ConfigFactory.load()
  val nodeIp = config.getString("clustering.ip")
  val clusterName = config.getString("clustering.cluster.name")
  //val nodeName = config.getString("clustering.ip")
  val nodeName = config.getString("clustering.dev-id")
  val nodePort = config.getString("clustering.port")
  val launcher = config.getString("clustering.launcher")
  val nnodes = config.getString("clustering.nodes")
  val monitorFrom:Int = config.getString("clustering.monitor-from").toInt
}
