package org.ngcdi.sckl

import akka.actor._
import java.time.{ZoneOffset,LocalDateTime, ZoneId, Instant}
import java.time.format.DateTimeFormatter
import java.util.Locale
import akka.util.Timeout
import scala.concurrent._
import scala.concurrent.duration._


abstract class ScklActor extends Actor with ScklLogging {
  this:Actor =>

  val timeoutd = 10 seconds
  implicit val timeout = Timeout(timeoutd)
  //implicit val mat = akka.stream.Materializer(context)
  implicit val mat = akka.stream.Materializer
  implicit val system = context.system
  implicit val executionContext = system.dispatcher



  val formatterTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm ss.SSSSSS")
    .withLocale( Locale.UK )
    .withZone( ZoneId.systemDefault() )

  val formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    .withLocale( Locale.UK )
    .withZone( ZoneId.systemDefault() )

  def getReadingTime():String={
    val nowInstant = Instant.now()
    val now = LocalDateTime.ofInstant(nowInstant, ZoneOffset.UTC).format(formatterTime)
    startTimer(nowInstant.getNano.toString(),"anomd")
    now
  }

  val scklBehaviour:Receive ={
    case x:Any => log.info("received unknown message {}, sender: {}",x,sender)
  }


}
