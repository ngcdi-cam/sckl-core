package org.ngcdi.sckl.ryuclient

import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.model.HttpResponse
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem

object NetworkAwarenessClientUtils {
  def isSuccessful(httpResponse: HttpResponse)(implicit
      actorSystem: ActorSystem,
      executionContext: ExecutionContext
  ): Future[Boolean] =
    Unmarshal(httpResponse).to[Map[String, Boolean]].map { x =>
      x.get("success").get
    }
}
