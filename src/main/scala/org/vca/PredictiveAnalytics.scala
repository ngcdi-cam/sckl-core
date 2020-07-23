package org.vca

import org.ngcdi.sckl._
import org.ngcdi.sckl.msgs._
import org.ngcdi.sckl.Constants._
import org.ngcdi.sckl.adm._
//dl4j

//import org.deeplearning4j.nn.modelimport.keras.KerasModelImport
//import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
//import org.nd4j.linalg.api.ndarray.INDArray
//import org.nd4j.linalg.factory.Nd4j
//import org.nd4j.linalg.io.ClassPathResource
//import org.nd4j.linalg.io._


//final case class ResultAD(tick:Int,deviceId:String,resultAD:Option[Int], details:Seq[Tuple2[String,String]])

trait PredictiveAnalytics extends DecisionMaking{
  this: ScklActor with ServiceView =>

  var reportedAssets:Seq[Tuple2[String,Double]]= Seq.empty // (Asset,RUL)

  val predictiveBehaviour:Receive = {
    //case RunPredictiveModel(assets:Seq[Tuple2[String,Seq[Double]]]) =>
    case RunPredictiveModel =>
      processResultPrediction(
        runPredictiveModel(aggregatedView)
      )
  }

  /*
  *Run pre-trained predictive model for the assets of interest
   * loads model, preprocess with recent data and use model to predict
   * returns RUL per asset
  */
  def runPredictiveModel(measurements:Seq[Measurement]):Seq[Tuple2[String,Double]] = {
    //assets:Seq[Tuple2[String,Seq[Double]]]
    log.info("tick ("+nRuns+") Running predictive model using the most updated condition...")

    // load the model
    //val p = ClassPathResource.
//    val simpleMlp = new ClassPathResource("my_model.h5")
//      .getFile().getPath()
//    val model = KerasModelImport.
 //     importKerasSequentialModelAndWeights(simpleMlp)

  //  log.info("KERAS Model Loaded!!!!:)")

    log.debug("Measurements for prediction:"+measurements.size)
    measurements
    .filter(_.resourceId == "1234567")
      .groupBy(_.neId)
      .map{
        m =>
        m._2
          .groupBy(_.metricId)
          .map{
            mt =>
            mt._2.map{
              ms => log.debug("NE "+ms.neId+" *Metric: "+mt._1+" ==> "+ms)
            }
          }
      }


    val conditionView = measurements
      .filter{
        m => conditionKPIs contains(m.metricName)
      }


   val results = conditionView
     .groupBy(_.neId)
     .map{
       av =>
       (av._1,   //Asset id
         calculateRULAsset(av._2) //Calculated RUL
       )
     }.toSeq


    log.debug("End predictions==>"+results )
    results
  }

  /*
   * Check against threshold for RUL of assets in order to trigger alarm to user
   */

  def processResultPrediction(
//    tick:Int,
    predictions:Seq[Tuple2[String,Double]]
  ):Unit ={

    log.debug("predicions are:==>"+predictions)
    val riskyAssets =
      predictions
        .filter(
          _._2 <= 8 //days
        )
        .collect{
          case x => (x._1,x._2)
        }
    log.info("Risky assets are===>"+riskyAssets)

    val newRiskyAssets:Seq[String] = riskyAssets.map(_._1).diff(reportedAssets.map(_._1)) //diff does not seem to return tuples

    val fullNewRiskyAssets =
      newRiskyAssets
        .map{
          raid =>
          riskyAssets.
            filter(_._1==raid)
          .head
        }.toSeq

    if(newRiskyAssets.size > 0){ //Only reports assets not reported already
      self ! ToConfirmMaintenanceP(fullNewRiskyAssets)
      reportedAssets = reportedAssets ++ fullNewRiskyAssets
    }

    //TODO Clear reported asset: it might be clear according to policy, e.g. clear after 24h
    //and not flagged by user...
  }



  def predictivePreStart():Unit ={

  }

  /*
   * Route health calculation => Average RUL in days
   */
  override def calculateRouteHealth(route:Seq[String],view:Seq[Measurement]):Double={
    route
      .map(a=>calculateRULAsset(view.filter(_.neId == a)))
      .sum / route.size
  }

  /*
   *
   * Asset health calculation for an asset whose measurements are supplied
   */

  override def calculateRULAsset(conditionView:Seq[Measurement]):Double={
    if(conditionView.size > 0)
      log.info(">======>tick ("+nRuns+") PRED Calculating RUL for asset:"+conditionView.head.neId+"<====<")
    conditionView
      .groupBy(_.metricName)
      .map(m=>log.info("PRED Group Metric ==>"+m))

    val rul =
      if(conditionView.size > 0){

        val v = calculatedRUL.get(conditionView.head.neId).get(nRuns)
        publish("ngcdi.da.calculated","RUL",conditionView.head.neId,conditionView.head.neIp,v)
        v
      }else
         0d
    rul
  }
}
