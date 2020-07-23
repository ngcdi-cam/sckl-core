package org.ngcdi.sckl

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConverters._


object Config {
  private val config = ConfigFactory.load()
  val datadir = config.getString("sckl.datasource.datadir")
  val restMonitoringUrl = config.getString("sckl.rest.sdn.monitoring.url")
  val restMonitoringUrlNodeName = restMonitoringUrl + ClusteringConfig.nodeName
  val sdncServer = config.getString("sckl.rest.sdn.server")
  val netwServer = config.getString("sckl.rest.netw.server")
  val sdncPort = Integer.parseInt(config.getString("sckl.rest.sdn.port")).intValue()
  val netwPort = Integer.parseInt(config.getString("sckl.rest.netw.port")).intValue()
  val netwApiKey = config.getString("sckl.rest.netw.api_key")
  val netwUrlsStr = config.getStringList("sckl.rest.netw.urls").get(0)
  //val netwUrlsStr:String = config.getList(x$1: String).getStringList("sckl.rest.netw.urls").get(0).substring(1,config.getStringList("sckl.rest.netw.urls").get(0).length -1).toString
  val netwUrls = netwUrlsStr.substring(1,netwUrlsStr.length()-1).split(",")

  val slkChannelURL = config.getString("sckl.rest.ui.url") // slack webhook
  val uiServer = config.getString("sckl.rest.ui.server")
  val uiPort = Integer.parseInt(config.getString("sckl.rest.ui.port"))

  val keyHosts = config.getString("sckl.service.hosts").split(",").toSeq
  val leftNgbName = config.getString("sckl.neighbour.left")
  val rightNgbName = config.getString("sckl.neighbour.right")
  val keyServices = config.getString("sckl.services").split(",").toSeq
  val ngbNames = config.getString("sckl.neighbours").split(",").toSeq
}
