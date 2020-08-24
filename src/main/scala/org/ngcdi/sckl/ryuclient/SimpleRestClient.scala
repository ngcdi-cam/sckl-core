package org.ngcdi.sckl.ryuclient

import scala.concurrent.Future
import akka.http.scaladsl.model.{
  HttpRequest,
  HttpResponse,
  HttpMethod,
  HttpHeader,
  HttpMethods,
  RequestEntity,
  HttpEntity
}
import akka.http.scaladsl.Http
import akka.actor.ActorSystem
import scala.collection.immutable.Seq

class SimpleRestClient(baseUrl: String, headers: Seq[HttpHeader] = Seq.empty)
    extends Serializable {

  final protected def httpGet(
      path: String
  )(implicit actorSystem: ActorSystem): Future[HttpResponse] = {
    sendRequest(HttpMethods.GET, path)
  }

  final protected def httpPost(
      path: String,
      entity: RequestEntity = HttpEntity.Empty
  )(implicit actorSystem: ActorSystem): Future[HttpResponse] = {
    sendRequest(HttpMethods.POST, path, entity)
  }

  final protected def httpPatch(
      path: String,
      entity: RequestEntity = HttpEntity.Empty
  )(implicit actorSystem: ActorSystem): Future[HttpResponse] = {
    sendRequest(HttpMethods.PATCH, path, entity)
  }

  final private def sendRequest(
      method: HttpMethod,
      path: String,
      entity: RequestEntity = HttpEntity.Empty
  )(implicit actorSystem: ActorSystem): Future[HttpResponse] = {
    val url = baseUrl + path
    val request = HttpRequest(method, url, headers, entity)
    Http().singleRequest(request)
  }
}
