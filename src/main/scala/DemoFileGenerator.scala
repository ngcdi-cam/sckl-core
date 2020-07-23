package org.ngcdi.sckl

//import java.util.concurrent.ThreadLocalRandom
import java.util.Random
//import scala.collection.generic.SeqFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.io._

/*
 * It generates synthetic data. It receives as arguments:
 *  - q: quantity of files to generate
 */

object DemoFileGenerator {

  def main (args: Array[String]) : Unit = {

    try{

      val seed:Long = 7 // For consistent random numbers
      val baseValue = 100
      val standardMin = 0.8 // percentage of basevalue
      val diffOutIn = 0.01 // Differenece betwen input value and output
      val qIterations = 70
      val tickStartDegradation = 71//45//20//120//50  // CHANGE WHEN LOW PROFILE START
      val tickEndDegradation = 71//1000//35//1000//90 // COME BACK TO HIGH PROFILE
      val frequencySensing = 10 // How often we sense

      val qDevices = Integer.parseInt(args(0)) // Quantity of devices (files) to generate

      var degradationTriggered = false

      val formatterTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm ss.SSSSSS")

      val startingTime = "2019-07-26 08:48 40.523000"
      val startingTS = LocalDateTime.parse(startingTime, formatterTime)



      val qStartDegradation = 0.2 // INCREASE FOR MORE SERIOUS DEGRADATIONS
      val degradationPace = Seq(0.05,0.02,0.03)
      //val degradationPace = Seq(0.07,0.05,0.06) // FOR MORE SERIOUS DEGRADATIONS
      //val ne_id = Seq("c1","c2","c3","c4","c5", "c6", "c7")
      val ne_id_number = Seq.range(1, qDevices+1, 1)
      val ne_id_prefix = Seq.fill(qDevices+1)("c")
      val ne_id = ne_id_prefix zip ne_id_number

      //val ne_id = Seq("c3")  // FOR INDIVIDUAL ASSETS
      val resourceId = "1234567"
      val metricIds = Seq("1","2","3")
      val metricNames = Seq(
        "CCInbound Throughput (Kbps)",
        "CCOutbound Throughput",
        "Temperature")

      //For temperature
      val heatingRates = Seq(0.06,0.05,0.07,-0.01,-0.005)
      val standardTRates = Seq(0.01,-0.01,0.02,-0.02)
      val generateTemperature = true
      val baseTemperature = 33.0


      val separator = ","

      //val random = ThreadLocalRandom.current()


      val random = new Random(seed)

      for (j<-0 to (ne_id.length - 1)) {
        val fileName = ne_id(j)._1+ne_id(j)._2 + "-in.csv"
        //degradationPace -> stable, vs increasing

        // random.setSeed(1l) // Settings seeds is not supported in ThreadlocalRandom, need to change if required

        // var inValues = scala.collection.mutable.IndexedSeq.empty[Double]
        // var outValues = scala.collection.mutable.IndexedSeq.empty[Double]
        var ticks = scala.collection.mutable.IndexedSeq.empty[Int]
        // var tickTimes = scala.collection.mutable.IndexedSeq.empty[String]
        var values = scala.collection.mutable.IndexedSeq.empty[Double]
        var metricIdList = scala.collection.mutable.IndexedSeq.empty[String]
        var metricNameList = scala.collection.mutable.IndexedSeq.empty[String]


        ticks = ticks :+ 0
        var degradation = 0.0
        var overheating = 0.0

        var currentTime = startingTS
        var timeList = scala.collection.mutable.IndexedSeq.empty[String]

        if (generateTemperature){
          values = values :+ ( baseTemperature * (1 + standardTRates(random.nextInt(standardTRates.length))))
          metricIdList = metricIdList :+ metricIds(2) // Temperature
          metricNameList = metricNameList :+ metricNames(2) // Temperature
          timeList = timeList :+ currentTime.format(formatterTime)
          currentTime = currentTime.plusSeconds(frequencySensing)
        }




        for (i<-1 to qIterations){

          //print("ct-->"+timeList)
          if (tickStartDegradation == i ){//&& tickEndDegradation > i){
            degradation = qStartDegradation
            overheating = baseTemperature
            degradationTriggered = true
          }

          if (tickEndDegradation == i ){//&& tickEndDegradation > i){
            degradation = 0.0
            overheating = 0.0
            degradationTriggered = false
          }

          //else{
          //  degradation = 0.0
          //  overheating = 0.0
          //}


          // Tpt In
          //values = values :+ ((baseValue * random.nextDouble(standardMin, 1)) * (1 - degradation))
          values = values :+ ((baseValue * double2Double(random.doubles(standardMin, 1).findAny().getAsDouble())) * (1 - degradation))
          metricIdList = metricIdList :+ metricIds(0) // TptIn
          metricNameList = metricNameList :+ metricNames(0) // TptIn
          timeList = timeList :+ currentTime.format(formatterTime)


          // Tpt Out
          values = values :+ ((values (values.length - 1) * (1 - diffOutIn )))
          metricIdList = metricIdList :+ metricIds(1) // TptOut
          metricNameList = metricNameList :+ metricNames(1) // TptOut
          if(degradationTriggered && degradation < 0.3) // REPLACE LIMIT for less degradation
            degradation = degradation + degradationPace(random.nextInt(degradationPace.length))
          timeList = timeList :+ currentTime.format(formatterTime)

          //if (degradation > 0.0)
          // Temperature
          values = values :+ (((values (values.length - 3 ) * (1 + standardTRates(random.nextInt(standardTRates.length))) + (overheating * heatingRates(random.nextInt(heatingRates.length))))))
          metricIdList = metricIdList :+ metricIds(2) // Temperature
          metricNameList = metricNameList :+ metricNames(2) // Temperature
          timeList = timeList :+ currentTime.format(formatterTime)

          currentTime = currentTime.plusSeconds(frequencySensing)

        }

        val neList = Seq.fill(values.size)(ne_id(j)._1+ne_id(j)._2)
        val resourceList = Seq.fill(values.size)(resourceId)


        // PrintWriter

        //val pw = new PrintWriter(new File("c1.txt" ))
        //pw.write()
        //pw.close

        // FileWriter
        val file = new File(fileName)
        val bw = new BufferedWriter(new FileWriter(file))

        for (i <-0 to values.length-1){
          bw.write(
            //println(
            neList(i) +
              separator +
              metricIdList(i) +
              separator +
              resourceList(i) +
              separator +
              timeList(i) +
              separator +
              values(i) +
              separator +
              timeList(i).substring(0,10) + // It takes only the date yyyy-MM-dd
              separator +
              metricNameList(i) + "\n"
          )
        }

        bw.close()

      }
    }catch{
      case e:Exception =>
        println("Usage: run command + args, e.g.: sbt \"run 5\", where 5 is qunatity of devices")
        e.printStackTrace()
    }


  }




}
