package org.vca
import java.time.{ZoneOffset, LocalDateTime, Instant}
import org.ngcdi.sckl.DigitalAssetBase
import org.ngcdi.sckl.model.Model
import akka.http.scaladsl.model.ResponseEntity
import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.Config
import org.ngcdi.sckl.ScklActor
import akka.util.ByteString

trait MessageParser {
  this: ScklActor =>

  def parseResponse(request: String, body: ByteString, data: String): Unit = {
    log.debug("DA DOES NOT Parse response...")
  }

  def parsingBehaviour: Receive = {

    case ParseResponseBody(request: String, body: ByteString, data: String) =>
      parseResponse(request, body, data)
    case ParseResponseBodyExtra(
          request: String,
          body: ByteString,
          data: String,
          dataExtra: Seq[String]
        ) =>
      parseResponseExtra(request, body, data, dataExtra)
  }

  def parseResponseExtra(
      request: String,
      body: ByteString,
      data: String,
      dataExtra: Seq[String]
  ) = {
    log.error("Parse Response Extra NOT IMPLEMENTED")
  }
}
