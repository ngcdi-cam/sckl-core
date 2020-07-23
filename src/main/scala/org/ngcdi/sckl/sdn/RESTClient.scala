package org.ngcdi.sckl.sdn

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import scala.concurrent.duration._

import scala.util.{ Failure, Success }
import scala.concurrent.{Await, Future, Promise }
import akka.stream.{ OverflowStrategy, QueueOfferResult }
import akka.util.ByteString
import com.typesafe.config.ConfigFactory


import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.Config._
import org.ngcdi.sckl.ClusteringConfig._
import org.ngcdi.sckl.NetworkUtils._
import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.msgs.RESTRequest
import org.ngcdi.sckl.ScklActor
import scala.collection.mutable.ListBuffer

import org.ngcdi.sckl.model._
import org.ngcdi.sckl.RESTController

// REST Msgs
final case class GetRequest(queryUrl:String, secured:Boolean)
final case class SingleGetRequest(queryUrl:String, secured:Boolean)
final case class PostRequest(queryUrl:String, data:String, secured:Boolean, tokenHeader:String)
final case class PostExtraRequest(queryUrl:String, data:String, dataExtra:Seq[String], secured:Boolean, tokenHeader:String)



object RESTClient {
  def props(server:String, port:Int): Props = Props(new RESTClient (server,port))
}

class RESTClient(server:String, port:Int) extends ScklActor {


//  implicit val system = context.system

  //import system.dispatcher

//  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
//  implicit val executionContext = system.dispatcher

  //val oCredentials = BasicHttpCredentials(oUser, oPass)

  val QueueSize = 20

  log.info("SERVER:"+server+":"+port)
  val poolClientFlow =
    if (!port.equals("80")) Http().cachedHostConnectionPool[Promise[HttpResponse]](server,port)
    else Http().cachedHostConnectionPool[Promise[HttpResponse]](server)

  val queue = Source
    .queue (QueueSize, OverflowStrategy.dropNew)
    .via(poolClientFlow)
    .toMat(Sink.foreach({
      case ((Success(resp), p)) =>
        p.success(resp)
      case ((Failure(e), p))    =>
        p.failure(e)
    }))(Keep.left)
    .run()


  def receive = {

    case GetRequest(queryUrl:String, secured:Boolean) =>
      doGetRequest(queryUrl,sender, secured:Boolean)
    case SingleGetRequest(queryUrl:String, secured:Boolean) =>
      doSingleGetRequest(queryUrl,sender, secured:Boolean)
    case PostRequest(queryUrl:String, data:String, secured:Boolean,tokenHeader:String) =>
      doPostRequest(queryUrl:String,data:String, sender, secured,tokenHeader)
    case PostExtraRequest(queryUrl:String, data:String, dataExtra:Seq[String], secured:Boolean,tokenHeader:String) =>
      doPostExtraRequest(queryUrl:String,data:String, dataExtra:Seq[String],sender, secured,tokenHeader)
  }


  def doPostRequest(queryUrl:String, data:String, replyTo:ActorRef, secured:Boolean,tokenHeader:String)={

    log.debug("Starting POST request: "+queryUrl)
    val url = "http://"+server +":"+ port + queryUrl
    /*    val requestPost = HttpRequest(
     HttpMethods.POST,
     //uri = "http://mnet:9081/nodes/h5/cmd",
     uri = url,
     entity =  ByteString(data))*/
    //log.info("Directory==>"+datadir)
   // val dataFileName = queryUrl match {
   //   case "/api/get_routes"  => "routesreq"
   //   case "/api/push_intent"  => "reroutereq"
    //}
    //val dataFile = scala.io.Source.fromFile(datadir+dataFileName+".json").mkString

    val requestPost =
      if(secured){
        if(tokenHeader != "")
          HttpRequest(
            HttpMethods.POST,
            uri = url,
            entity = HttpEntity(ContentTypes.`application/json`,data)
          )//.withHeaders(RawHeader("X-Access-Token", "access token"))
            .addCredentials(BasicHttpCredentials(oUser, oPass))
        else
          HttpRequest(
            HttpMethods.POST,
            uri = url,
            entity = HttpEntity(ContentTypes.`application/json`,data)
          ).withHeaders(RawHeader("X-Access-Token", tokenHeader))
            .addCredentials(BasicHttpCredentials(oUser, oPass))

      }else{
        if (tokenHeader != "")
          HttpRequest(
            HttpMethods.POST,
            uri = url,
            entity = HttpEntity(ContentTypes.`application/json`,data)
          )//.withHeaders(RawHeader("X-Access-Token", "access token"))
        else
          HttpRequest(
            HttpMethods.POST,
            uri = url,
            entity = HttpEntity(ContentTypes.`application/json`,data)
          ).withHeaders(RawHeader("X-Access-Token", tokenHeader))
      }

    //log.debug(replyTo.path+": POST "+requestPost.getUri+" ==> "+dataFile+"<==")
    log.debug(replyTo.path+": REQUEST POST ENTITY: "+requestPost.entity)

    val responseFuture: Future[HttpResponse] = Http().singleRequest(requestPost)


    responseFuture.map {
      case response @ HttpResponse(StatusCodes.OK, _, _, _) =>
        response.entity.contentType match {
          case ContentTypes.`application/json` =>

            response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach {
              body =>
              log.debug("REST Got response, BODY1: " + body.utf8String)
              replyTo ! ParseResponseBody(queryUrl,body,data)

            }
          case ContentTypes.`text/html(UTF-8)` =>
            response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach {
              body =>
              log.debug("REST Got response, BODY2: " + body.utf8String)
              replyTo ! ParseResponseBody(queryUrl,body,data) //Temporarily
            }
          case _ =>
            log.debug("RAW RESPONSE==>"+response)
            log.debug("RAW RESPONSE CONTENT TYPE==>"+response.entity.contentType)
            response.discardEntityBytes()
        }

      case response @ HttpResponse(StatusCodes.InternalServerError, _, _, _) =>
        log.debug("INTERNAL SERVER ERROR===>"+response)
        log.debug("Returning false")
        replyTo ! ParseResponseBody(queryUrl,ByteString("false"),data) //Temporarily
      case response:Any =>
        log.error("ERROR===>"+response)
        sys.error("ERROR Something wrong")
    }

    Await.ready(responseFuture, 5 seconds)

    /*
    responseFuture
      .onComplete {
        case Success(res) if (res._1.intValue()!=404) =>
          try{
            replyTo ! ProcessResponse(res.entity)
            //log.info("RESULT POST CODE==>"+res._1.intValue()+"<==")
            //log.info("RESULT POST ENTITy==>"+res.entity+"<==")
            //replyTo ! ProcessResponsePost("OK")
          }catch {
            case e:Exception =>
              e.printStackTrace()
          }


        case Failure(x)   =>
          log.info("OMITTING POST ERROR==>"+x+"<==")
          //sys.error("Something wrong")
      }

     */
  }

  def doGetRequest(queryUrl:String, replyTo:ActorRef, secured:Boolean)={

    val promise = Promise[HttpResponse]

    val request =
      if(secured)
        HttpRequest( uri = queryUrl)
          //val requestStats = HttpRequest( uri = restMonitoringUrl + resourceURL)
          .withHeaders(RawHeader("X-Access-Token", "access token"))
          .addCredentials(BasicHttpCredentials(oUser, oPass))
      else
        HttpRequest( uri = queryUrl)
          .withHeaders(RawHeader("X-Access-Token", "access token"))

      log.debug(replyTo.path+": GET "+request.getUri)



      val responseFuture: Future[HttpResponse] = queue.offer(request->promise).flatMap {
        case QueueOfferResult.Enqueued    => promise.future
        case QueueOfferResult.Dropped     => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
        case QueueOfferResult.Failure(ex) => Future.failed(ex)
        case QueueOfferResult.QueueClosed => Future.failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
      }

    responseFuture.map {
      case response @ HttpResponse(StatusCodes.OK, _, _, _) =>
        response.entity.contentType match {
          case ContentTypes.`application/json` =>

            response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach {
              body =>
              log.debug("Got response, BODY3: " + body.utf8String)
              replyTo ! ParseResponseBody(queryUrl,body,"")

            }
          case _ =>
            log.debug("ERROR RESPONSE RAW==>"+response)
            response.discardEntityBytes()
        }

      case _ => sys.error("ERROR Something wrong")
    }


      /*responseFuture
        .onComplete {
          case Success(res) if (res._1.intValue()==404) =>
            log.info("Server unavailable")
          case Success(res) if (res._1.intValue()!=404 && res.entity.contentType == ContentTypes.`application/json`) =>

            try{
              log.debug("RESULT==>"+res.entity)
//              res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
 //               log.info("Got response, body: " + body.utf8String)
  //            }
              res.discardEntityBytes()
              replyTo ! ParseResponse(queryUrl,res.entity)

            }catch {
              case e:Exception =>
                log.error(e,"Error Getting Response")
            }

          case Failure(x)   => log.error("Something went wrong:"+x)
        }*/

      Await.ready(responseFuture, 5 seconds)

  }


  def doSingleGetRequest(queryUrl:String, replyTo:ActorRef, secured:Boolean)={

    val promise = Promise[HttpResponse]

    val url = "http://"+server +":"+ port + queryUrl

    val request =
      if(secured)
        HttpRequest( uri = url)
          //val requestStats = HttpRequest( uri = restMonitoringUrl + resourceURL)
          .withHeaders(RawHeader("X-Access-Token", "access token"))
          .addCredentials(BasicHttpCredentials(oUser, oPass))
      else
        HttpRequest( uri = url)
          .withHeaders(RawHeader("X-Access-Token", "access token"))

      log.debug(replyTo.path+": GET "+request.getUri)

    val responseFuture: Future[HttpResponse] = Http().singleRequest(request)

    responseFuture.map {
      case response @ HttpResponse(StatusCodes.OK, _, _, _) =>
        response.entity.contentType match {
          case ContentTypes.`application/json` =>

            response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach {
              body =>
              log.debug("Got response, BODY4: " + body.utf8String)
              replyTo ! ParseResponseBody(queryUrl,body,"")

            }
          case _ =>
            log.debug("RESPONSE RAW==>"+response)
            response.discardEntityBytes()
        }

      case _ => sys.error("ERROR Something wrong")
    }


      Await.ready(responseFuture, 5 seconds)

  }



  /*
   * It pushes an action on SDN Controller
   */
  def pushAction(url:String, newRouteJson:String, respondTo:ActorRef):Unit ={

    val jsonReroute = ""

    log.debug("file is===>"+jsonReroute+"<===")

    val url = "http://"+oServer+":"+ oPort + oRootUrl + oRerouteUrl

    log.debug("Posting to: "+url+" ==> "+jsonReroute)

    val requestNotification = HttpRequest(
      HttpMethods.POST,
      uri = url,
      entity = HttpEntity(ContentTypes.`application/json`,jsonReroute.getBytes())
    ).withHeaders(RawHeader("X-Access-Token", "access token"))
      //.addCredentials(oCredentials)


    val responseFuture: Future[HttpResponse] = Http().singleRequest(requestNotification)

    responseFuture
      .onComplete {
        case Success(res) if (res._1.intValue()!=404) =>
          try{
            respondTo ! ParseResponse(url,res.entity)
          }catch {
            case e:Exception =>
              e.printStackTrace()
          }


        case Failure(_)   => sys.error("something wrong")
      }

  }

  /*
   * Only for Slack
   * Data extra contains list to be included in reply
   */

  def doPostExtraRequest(queryUrl:String, data:String, dataExtra:Seq[String], replyTo:ActorRef, secured:Boolean,tokenHeader:String)={

    //Slack config
    val url = "https://"+server + queryUrl
    log.debug("Starting POST request: "+url)
    /*    val requestPost = HttpRequest(
     HttpMethods.POST,
     //uri = "http://mnet:9081/nodes/h5/cmd",
     uri = url,
     entity =  ByteString(data))*/
    //log.info("Directory==>"+datadir)
   // val dataFileName = queryUrl match {
   //   case "/api/get_routes"  => "routesreq"
   //   case "/api/push_intent"  => "reroutereq"
    //}
    //val dataFile = scala.io.Source.fromFile(datadir+dataFileName+".json").mkString

    val requestPost =
      if(secured){
        if(tokenHeader != "")
          HttpRequest(
            HttpMethods.POST,
            uri = url,
            entity = HttpEntity(ContentTypes.`application/json`,data)
          )//.withHeaders(RawHeader("X-Access-Token", "access token"))
            .addCredentials(BasicHttpCredentials(oUser, oPass))
        else
          HttpRequest(
            HttpMethods.POST,
            uri = url,
            entity = HttpEntity(ContentTypes.`application/json`,data)
          ).withHeaders(RawHeader("X-Access-Token", tokenHeader))
            .addCredentials(BasicHttpCredentials(oUser, oPass))

      }else{
        if (tokenHeader != "")
          HttpRequest(
            HttpMethods.POST,
            uri = url,
            entity = HttpEntity(ContentTypes.`application/json`,data.getBytes)
          )//.withHeaders(RawHeader("X-Access-Token", "access token"))
        else
          HttpRequest(
            HttpMethods.POST,
            uri = url,
            entity = HttpEntity(ContentTypes.`application/json`,data)
          ).withHeaders(RawHeader("X-Access-Token", tokenHeader))
      }

    //log.debug(replyTo.path+": POST "+requestPost.getUri+" ==> "+dataFile+"<==")
    log.debug(replyTo.path+": REQUEST POST ENTITY: "+requestPost.entity)

    val responseFuture: Future[HttpResponse] = Http().singleRequest(requestPost)


    responseFuture.map {
      case response @ HttpResponse(StatusCodes.OK, _, _, _) =>
        response.entity.contentType match {
          case ContentTypes.`application/json` =>

            response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach {
              body =>
              log.debug("EXTRA REST Got response, BODY5: " + body.utf8String)
              replyTo ! ParseResponseBodyExtra(queryUrl,body,data,dataExtra)

            }
          case ContentTypes.`text/html(UTF-8)` =>
            response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach {
              body =>
              log.debug("EXTRA REST Got response, BODY6: " + body.utf8String)
              replyTo ! ParseResponseBodyExtra(queryUrl,body,data,dataExtra) //Temporarily
            }
          case _ =>
            log.debug("EXTRA RAW RESPONSE==>"+response)
            log.debug("RAW RESPONSE CONTENT TYPE==>"+response.entity.contentType)
            response.discardEntityBytes()
        }

      case response @ HttpResponse(StatusCodes.InternalServerError, _, _, _) =>
        log.debug("INTERNAL SERVER ERROR===>"+response)
        log.debug("Returning false")
        replyTo ! ParseResponseBodyExtra(queryUrl,ByteString("false"),data,dataExtra) //Temporarily
      case response:Any =>
        log.error("ERROR===>"+response)
        sys.error("ERROR Something wrong")
    }

    Await.ready(responseFuture, 5 seconds)

    /*
    responseFuture
      .onComplete {
        case Success(res) if (res._1.intValue()!=404) =>
          try{
            replyTo ! ProcessResponse(res.entity)
            //log.info("RESULT POST CODE==>"+res._1.intValue()+"<==")
            //log.info("RESULT POST ENTITy==>"+res.entity+"<==")
            //replyTo ! ProcessResponsePost("OK")
          }catch {
            case e:Exception =>
              e.printStackTrace()
          }


        case Failure(x)   =>
          log.info("OMITTING POST ERROR==>"+x+"<==")
          //sys.error("Something wrong")
      }

     */
  }




}
