package org.ngcdi.sckl.sdn

import org.ngcdi.sckl.model._

//#json-support
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
//import org.slf4j.LoggerFactory
import org.ngcdi.sckl._

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol{
  this:ScklActor =>
  //val log1 = LoggerFactory.getLogger("org.ngcdi.sckl.sdn.JsonProtocol")
  // import the default encoders for primitive types (Int, String, Lists etc)
  //import DefaultJsonProtocol._

  // ****Watch out the order of the format mappings as implicit used types should have been defined before***

  implicit object intentMetricKeyFormat extends RootJsonFormat[IntentMetricKey] {
    def read(json: JsValue): IntentMetricKey          = IntentMetricKey(json.asInstanceOf[JsString].value)
    override def write(obj: IntentMetricKey): JsValue = JsString(obj.value)
  }

  implicit val intentMetricJsonFormat = jsonFormat4(IntentMetric)

  implicit object intentMetricValueFormat extends RootJsonFormat[IntentMetricValue] {
    def read(json: JsValue): IntentMetricValue          = {
      //print("VALUE READ==>"+json+"<==")
      try{
        IntentMetricValue(json.convertTo[IntentMetric])
      }catch{
        case e:Exception =>
          e.printStackTrace()
          IntentMetricValue(IntentMetric("+++++++Error!!!++++++",0l,0l,""))
      }
    }
    override def write(obj: IntentMetricValue):JsValue = obj.value.toJson
  }



  implicit val intentMetricValueSetFormat = new RootJsonFormat[Set[IntentMetricValue]] {
    def write(items: Set[IntentMetricValue]) = JsArray(items.map(_.toJson).toVector)
    def read(value: JsValue) = value match {
      case JsArray(elements) => elements.map(_.convertTo[IntentMetricValue]).toSet[IntentMetricValue]
      case x                 => deserializationError("Expected Array as JsArray, but got " + x)
    }
  }

  implicit val intentMetricEntryFormat = DefaultJsonProtocol.mapFormat[IntentMetricKey, Set[IntentMetricValue]]
  //implicit val dataMapRootReader  = DefaultJsonProtocol.mapFormat[CampKey, Map[DataPointKey, Set[DataPointValue]]]

  implicit val intentMetricsJsonFormat = jsonFormat1(IntentMetrics)

  //implicit val istatisticsJsonFormat = jsonFormat1(IStatistics)
  implicit val responseStatisticJsonFormat = jsonFormat3(ResponseStatistic)
  implicit val responseStatisticsJsonFormat = jsonFormat1(ResponseStatistics)



  implicit val onosAppJsonFormat = jsonFormat2(OnosApp)
  implicit val pathJsonFormat = jsonFormat2(Path)

  implicit val intentPathJsonFormat = jsonFormat3(IntentPath)
  implicit val intentReroutingJsonFormat = jsonFormat1(IntentRoutes)
  implicit val simpleResult = jsonFormat1(SimpleResult)

  implicit val notificationJsonFormat = jsonFormat1(Notification)

  //implicit val requestIntentRoutes = jsonFormat1(RequestIntentRoutes)

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any) = x match {
      case n: Int => JsNumber(n)
      case s: String => JsString(s)
      case b: Boolean if b == true => JsTrue
      case b: Boolean if b == false => JsFalse
      //case t: Tuple2[String,Double] =>
      case sq:Seq[String] =>
        val elements = sq.collect{
          case s:String =>
            JsString(s)
        }.toList
        JsArray(elements)
      /*case sqe:Seq[RouteEntry] =>
        val elements = sq.collect{
          case re:RouteEntry =>
            re.toJson
        }.toList
        JsArray(elements)*/
      /*case mp:Map[String,Any]=>
        mp.toJson
        mp.map{
          case (k,v)=>
            JsonObject(
              k.toJson->v.toJson
            )
        }*/
    }
    def read(value: JsValue) = value match {
      case JsNumber(n) => n.intValue()
      case JsString(s) => s
      case JsTrue => true
      case JsFalse => false
      case JsArray(e) =>
        //log.info("PARsING ===>"+e)
        val x:Vector[String] = e  match {
          case v:Vector[JsValue] =>
            val a = v.collect{
              case el:JsString =>
                el.toString
            }
            a
        }
        x.toSeq
    }
  }


  implicit object routeKeyFormat extends RootJsonFormat[RouteKey] {
    def read(json: JsValue): RouteKey          = RouteKey(json.asInstanceOf[JsString].value)
    override def write(obj: RouteKey): JsValue = JsString(obj.value)
  }

  implicit object routeValueFormat extends RootJsonFormat[RouteValue] {
    def read(json: JsValue): RouteValue          = {
      //log.debug("VALUE READ==>"+json+"<==")
      try{
        RouteValue(json.convertTo[Seq[String]])
      }catch{
        case e:Exception =>
          log.error("ERROR!!!:"+e.getMessage())
          e.printStackTrace()
          RouteValue(Seq.empty)
      }
    }
    override def write(obj: RouteValue):JsValue = obj.value.toJson
  }


  //implicit val routeMap = jsonFormat2(RouteMap)
  implicit val routeMapEntryFormat = DefaultJsonProtocol.mapFormat[RouteKey, Seq[RouteValue]]
  implicit val routeEntryFormat = jsonFormat2(RouteEntry)
  implicit val alternativeRoute = jsonFormat3(AlternativeRoute)
  implicit val getRoutesRequest = jsonFormat2(GetRoutesRequest)
  implicit val pushIntentRequest = jsonFormat2(PushIntentRequest)

/*
  implicit object alternativeRouteJsonFormat extends JsonFormat[AlternativeRoute] {

    override def read(json: JsValue): MatchRule =
      json.asJsObject.getFields("key", "routes", "num_routes") match {

        case Seq( JsString(key), JsObject(routes), JsString(num_routes)) =>



        case Seq( JsString(key)) =>


      case _ =>
        throw new DeserializationException("Alternative Route expected")
    }

    def write(obj: AlternativeRoute) =  {
      JsObject(
        "key"->JsString(obj.key),
        "routes"->obj.routes.toJson,
        "num_routes"->JsString(obj.num_routes)
      )
    }
  }
 */






/*
  implicit val alternativeRoutes = new RootJsonFormat[AlternativeRoute] {
    def write(altroutes:AlternativeRoute) = JsObject(
      "routes" -> altroutes.routes.toJson,
      "key" -> altroutes.key,toJson,
      "numRoutes"->altroutes.numRoutes.toJson
    )
    def read(value: JsValue) = value match {
      case js:JsObject(fields) =>
        var key:String = ""
        var numRoutes:String = ""
        var routes:Map[String,Seq[String]] = Map.empty
        log.info("READING====>"+value)
        fields.map{
          case (k, v) =>
            k match {
              case "key" =>
                key = v
              case "numRoutes "=>
                numRoutes = v
              case "routes" =>
                routes = v
              case _ =>
                deserializationError("Exception Unexpected Value !!!!!!!!!")
            }
        }
        new AlternativeRoute(key=key,numRoutes=numRoutes,routes=routes)
      case x =>
        deserializationError("Expected Array as JsArray, but got " + x)
    }
  }

 */
  //implicit val modelJsonReader = new JsonReader[List[org.ngcdi.User]] {
    // See https://github.com/spray/spray-json on how to implement JsonReader
  //}

  //def parseJson(str: String): List[org.ngcdi.User] = {
    // parse using spray-json
  //  str.parseJson.convertTo[List[org.ngcdi.User]]
  //}

}
//#json-support
