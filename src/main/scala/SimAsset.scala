package org.ngcdi.sckl.sim

import akka.actor._
import scala.concurrent.duration._
import akka.stream.scaladsl._
//import akka.NotUsed
import _root_.akka.stream.{ActorMaterializer, IOResult} //Materializer
import _root_.akka.stream.scaladsl.{Sink, Source, FileIO, Keep} //Flow,
import akka.util.ByteString
import akka.stream.OverflowStrategy
import java.nio.file.{FileSystems, Files, Paths} //StandardOpenOption, OpenOption
import java.nio.charset.StandardCharsets
import scala.concurrent.Future
import akka.stream._
import org.ngcdi.sckl.msgs._
import java.time.{Instant, LocalDateTime, ZoneOffset, ZoneId}
import java.util.Locale
import java.time.format.DateTimeFormatter
/*
 *  It simulates on Real Time (RT) an asset which sensors output to a file passed as the last argument (fileOutStr)).
 * The other arguments provide the simulation parameter for the asset as follows:
 *  - fileInStr: Source file where the status of this asset is described through a time series of comma separated values with every line being a reading from a sensor in a time t.
 *  - metrics: Seq with name of the metrics corresponding to the readings representing the asset status.
 *  - frequency: (In seconds), how often the sensors read and pushes out to fileOutStr file. It assumes all sensor are synchronised and read and push at the same time (slight ms variations)
 */


class SimAsset(fileInStr:String, metrics:Seq[String], frequency:Int, fileOutStr:String) extends Actor with ActorLogging {

  implicit val materializer = ActorMaterializer()
  val formatterTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm ss.SSSSSS")
    .withLocale( Locale.UK )
    .withZone( ZoneId.systemDefault() )

  override def postStop(): Unit = {
    context.system.terminate()
  }

  def receive = {
    case Start  =>
      log.debug("Receives message!!!")
      simulate()
    case Stop =>
      context.stop(self)
  }

  /*
   * It simulates a series of sensor readings that represent the status of the asset.
   * The readings are sourced from a file and pushed out, according to a given time frequency, e.g. one reading every 5 seconds.
   */
  def simulate():Unit = {

    try{
    val fs = FileSystems.getDefault
    val inPath =fs.getPath(fileInStr)
    val outPath =fs.getPath(fileOutStr)

    Files.createFile(outPath)
      log.debug("File Created-->"+outPath)
    implicit val system = context.system
    import system.dispatcher


    //Setting input data source
    val fileIn:Source[ByteString,Future[IOResult]] =
      FileIO.fromPath(inPath)

    val bufferSize = metrics.size*5

    val queue = Source
      .queue[Seq[String]](bufferSize, OverflowStrategy.backpressure) // It ensures every line of the CSV is processed by waiting until consumer is ready to write readings in file
      //.queue[Seq[String]](bufferSize, OverflowStrategy.dropBuffer) // Uncomment if we want to start feeding the input file data only when Digital Asset is ready, discarding whatever it has been processed earlier.
      .throttle(1, frequency.seconds) // It processes one element within frequency seconds. 1 Element corresponds to 1 reading of each metric, so, as many as metrics are.
      .map(
        s =>
        ByteString(
          bringToRT(s) // Need to bring to RT at this point as it is when the queue/throttle function has been applied.
            .mkString("","\n","\n") // Converts the Vector of X readings in one String to perform just one file write.
        )
      )
      .toMat(
        FileIO.toPath(outPath)
      )(Keep.left) // As streams are processed left (from source) to right (to sink)
      .run()

      log.debug("Queue created for simulation")
    //implicit val ec = system.dispatcher

    val processReading = fileIn
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024))
      .map(decode)
      //.map(bringToRT)
      .grouped(metrics.size) // Groups lines from source file according to q of metrics

    processReading
      .mapAsync(1)(x => {
        log.debug("Processing reading--->"+x )
        queue.offer(x).map {
          case QueueOfferResult.Enqueued    => log.debug(s"enqueued $x")
          case QueueOfferResult.Dropped     => log.debug(s"dropped $x")
          case QueueOfferResult.Failure(ex) => log.debug(s"Offer failed ${ex.getMessage}")
          case QueueOfferResult.QueueClosed => log.debug("Source Queue closed")
        }
      })
      .runWith(Sink.ignore)

      log.info("Simulation started.")
    }catch{
      case e:Exception =>
        e.printStackTrace()
    }
  }

  /*
   * It is a wrapper function that decodes the ByteString into a string according to the preferred charset
   */

  def decode(l:ByteString):String = {
    l.decodeString(StandardCharsets.UTF_8)
  }

  /*
   * Convert timestamps from files in real time according to sensing frequency
   */
  def bringToRT (l:Seq[String]):Seq[String]={
    val n = l
      .foldLeft(Seq[String]())(
        {
          (z,l) =>
          val newl = getRTReading(l)
          z:+ newl
        }

      )
    n
    }


  /*
   * It replaces time-related information in each line of the CSV with current time info
   *  This function is specific to the CSV layout and must be adjusted accordingly.
   */
  def getRTReading(l:String):String = {
     val now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).format(formatterTime)

    //TODO This can be improved to avoid using mutable String. (Left for later... as this is just the Asset simulator).
    var newString = ""

    for((x,i)<-l.split(",").map(_.trim).zipWithIndex){
      i match {
        case 3 => // 3rd position of the CSV is the timestamp
          newString = newString + now + ","
        case 5 => // 5 position of the CSV is the date
          newString = newString + now.substring(0,10) + ","
        case _ =>
          newString = newString + x + ","
      }
    }
    newString.substring(0, newString.lastIndexOf(","))
  }

}
