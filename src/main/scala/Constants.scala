package org.ngcdi.sckl
import scala.concurrent.duration._
import org.ngcdi.sckl.ryuclient.NetworkAwarenessService
import org.ngcdi.sckl.ryuclient.NetworkAwarenessRawCrossDomainLink
//import ClusteringConfig._

object Constants {

  val adjReport =
    1000 // For reporting in Kamon multiply for this value to catch decimals. Later in visualisation tool divide over this value.
  val Temperature = "board_temperature"
  val ThroughputIn = "CCInbound Throughput (Kbps)"
  val ThroughputOut = "CCOutbound Throughput"
  val CPU_USG = "cpu_usage"
  val RAM_USG = "ram_usage"
  val OPT_POW = "optical_power"
  val SYS_ERR = "system_error_pkts"
  val conditionKPIs = Seq(Temperature, CPU_USG, RAM_USG, OPT_POW, SYS_ERR)
  val conditionCriteria = "condition-based"
  val shortestPath = "shortestPath"
  val predictedCongestion = "predicted-congestion"

  // Metrics compounding the view
  def qMetrics = 2
  def digitalAssetName = "digiasset"
  def serviceManagerName = "servicemanager"
  def functionProvisionerName = "functionProvisioner"

  def reportFromDAAverage = "average" //of values read
  def reportFromDALast = "last" // last value read
  def reportingValue = reportFromDALast // Change to configure what to report

  def frequencyLocalAggregation = 20 seconds
  def simFrequencySensing = 10 seconds //It should be aligned with file records
  def frequencySensing = 10 //It should be aligned with file records
  def frequencyServiceReport = 30 //seconds
  def simInitialSensingDelay = 0 seconds
  def initialServiceReportDelay = 0 seconds
  def maxLocalAggregation = 4
  def qAssetAgentsService = 3

  def mappingSLAMetric = Seq("bandwidth", "availability")
  def mappingMetricSLA =
    Map(
      ThroughputIn -> mappingSLAMetric(0),
      ThroughputOut -> mappingSLAMetric(0)
    )

  def srcExt = ".csv"
  val readyMsg = "Ready"
  val reportMsg = "Report"

  val notReadyYetMsg = "NotReadyYet"

  //Control chart
  val sampleSize = 3

  val smregistry = "SMRegistry"

  //R Integration Constants

  val rserver = "rserver"
  //val rserver = "localhost"
  val rport = 6311
  val median = 90
  val sd = 5
  val minseglength = 2
  val maxseglength = 100
  val lambda = 5

  // ONOS Integration Constants

  val oServer = "51.15.59.76"
  val oPort = 8181
  val oUser = "onos"
  val oPass = "rocks"
  val oRootUrl = "/onos/v1/imr/imr/"
  val oIntentStats = "intentStats"
  val oAvailableIntents = "monitoredIntents"
  val oRerouteUrl = "reRouteIntents"

  val hosts: Map[String, String] = Map(
    "00:00:00:00:00:01/None" -> "h1(10.0.0.1)",
    "00:00:00:00:00:02/None" -> "h2(10.0.0.2)",
    "00:00:00:00:00:03/None" -> "h3(10.0.0.3)",
    "00:00:00:00:00:04/None" -> "h4(10.0.0.4)"
  )

  val switches: Map[String, String] =
    Map(
      "of:0000000000000001" -> "s1",
      "of:0000000000000002" -> "s2",
      "of:0000000000000003" -> "s3",
      "of:0000000000000004" -> "s4",
      "of:0000000000000005" -> "s5",
      "of:0000000000000006" -> "s6",
      "of:0000000000000007" -> "s7",
      "of:0000000000000008" -> "s8",
      "of:0000000000000009" -> "s9",
      "of:000000000000000a" -> "s10",
      "of:000000000000000b" -> "s11",
      "of:000000000000000c" -> "s12",
      "of:000000000000000d" -> "s13",
      "of:000000000000000e" -> "s14"
    )

  val simplifiedIntents: Map[String, String] = Map(
    "00:00:00:00:00:01/None00:00:00:00:00:03/None" -> "intent1",
    "00:00:00:00:00:04/None00:00:00:00:00:02/None" -> "intent2",
    "00:00:00:00:00:02/None00:00:00:00:00:04/None" -> "intent3",
    "00:00:00:00:00:03/None00:00:00:00:00:01/None" -> "intent4"
  )

  //Slack Integration

  //Webhook

  val notificationMsg =
    "Hi Bob! :smile: \n We have adjusted your work schedule for today :+1:. \n Please have a look: <https://mperhez.github.io/me/assets/images/bob-new-schedule.png>"

  val ConfirmMaintenanceMsg =
    "{\"text\":\"Please confirm if you accept re-planning maintenance for assets failing in less than 7 days. !\"}"

  //Services
  val servicePaths = Seq(
    //Service, Swithch, Port
    //sc1
    Tuple3("sc1", "6", "2"),
    Tuple3("sc1", "1", "3"),
    Tuple3("sc1", "5", "2"),
    Tuple3("sc1", "10", "1"),
    Tuple3("sc1", "1", "1234567"),
    Tuple3("sc1", "2", "1234567"),
    Tuple3("sc1", "3", "1234567"),
    Tuple3("sc1", "4", "1234567"),
    Tuple3("sc1", "5", "1234567"),
    Tuple3("sc1", "6", "1234567"),
    Tuple3("sc1", "7", "1234567"),
    Tuple3("sc1", "8", "1234567"),
    Tuple3("sc1", "9", "1234567"),
    Tuple3("sc1", "10", "1234567"),
    //sc2
//  Tuple3("sc3","7","1"),
//  Tuple3("sc3","2","2"),
//  Tuple3("sc3","3","3"),
//  Tuple3("sc3","8","3"),
    //sc3
    Tuple3("sc2", "7", "1"),
    Tuple3("sc2", "2", "2"),
    Tuple3("sc2", "3", "2"),
    Tuple3("sc2", "4", "2"),
    Tuple3("sc2", "5", "3"),
    Tuple3("sc2", "10", "2")
  )

  //Precalculated RUL values
  val calculatedRUL = Map("Test" -> Seq.empty)

  // ==============================
  // Network Awareness Metric Types
  // ==============================

  val awarenessFreeBandwidth = "free_bandwidth"
  val awarenessThroughput = "throughput"
  val awarenessBandwidth = "bandwidth"
  val awarenessLatency = "delay"
  val awarenessHop = "weight"

  val awarenessMetrics = Set(
    awarenessFreeBandwidth,
    awarenessThroughput,
    awarenessBandwidth,
    awarenessLatency,
    awarenessHop
  )

  val awarenessFlowThroughput = "flow_throughput"

  // ======================================
  // Network Awareness Services and Weights
  // ======================================

  val awarenessServices = Seq(
    NetworkAwarenessService(0, "10.0.0.1", "10.0.0.2", Map(
      awarenessFreeBandwidth -> 0.001,
      awarenessLatency -> 0.999
    ))
  )

  // ========================================
  // Network Awareness Cross Domain Optimizer
  // ========================================

  // Network Awareness Default Metric Weights
  val awarenessDefaultMetricWeights = Map(
    awarenessFreeBandwidth -> 1.0,
    awarenessLatency -> 0.0,
    awarenessHop -> 0.0
  )

  // Network Awareness Enabled Metrics
  val awarenessEnabledMetrics = Seq(
    awarenessFreeBandwidth,
    awarenessLatency,
    awarenessHop
  )

  val awarenessOptimalKPaths = 1
  val awarenessOptimalK2Paths = 5
  
  // ====================================
  // Network Awareness Cross Domain Links
  // ====================================

  val awarenessCrossDomainLinks = Seq(
    // srcDpid, srcPort, srcControllerId, dstDpid, dstPort, dstControllerId
    NetworkAwarenessRawCrossDomainLink(1, 1, 0, 2, 1, 1),
    NetworkAwarenessRawCrossDomainLink(5, 3, 0, 2, 3, 1),
    NetworkAwarenessRawCrossDomainLink(5, 4, 0, 6, 3, 1),
    NetworkAwarenessRawCrossDomainLink(8, 2, 0, 9, 2, 1)
  )

  // ================
  // Ryu Metric Types
  // ================
  
  val ryuRxPackets = "rx_packets"
  val ryuTxPackets = "tx_packets"
  val ryuRxBytes = "rx_bytes"
  val ryuTxBytes = "tx_bytes"
  val ryuRxDropped = "rx_dropped"
  val ryuTxDropped = "tx_dropped"
  val ryuRxErrors = "rx_errors"
  val ryuTxErrors = "tx_errors"

  // ===============================
  // Anomaly Detectors and Actuators
  // ===============================

  // Overheating Detector and Actuator
  val overheatingActuatorTemperatureWeightMap = Map[(Double, Double), Double](
    // (temp_low, temp_high) -> weight
    (Double.NegativeInfinity, 45.0) -> 1.0,
    (45.0, 70.0) -> 0.1,
    (70.0, 80.0) -> 0.05,
    (80.0, Double.PositiveInfinity) -> 0.0
  )

  // Congestion Detector
  val congestionDetectorThreshold = 0.5 // proportion of bandwidth
  val congestionDetectorFlowThreshold = 0.2

  // Congestion Actuator
  val servicesToInstall = awarenessServices

  // ================================
  // Network Awareness Stats Streamer
  // ================================

  val awarenessStatsStreamerSenseInterval = 5 seconds

  // ==================
  // Message Forwarding
  // ==================

  val messageForwardingInitialTtl = 1
  val messageForwardingIdQueueMaxSize = 10
}
