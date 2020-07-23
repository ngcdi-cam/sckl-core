package org.ngcdi.sckl

//import java.time.format.DateTimeFormatter
//import java.time.ZoneId
//import java.util.{Calendar} //Locale

case object StatisticsUtils {


  /*
   * Calculates the Mean of given list of elements
   * x_i: elements
   * n: size of sample
   */

  def calculateMean (x_i:Seq[Double]):Double = {

    try{
      val x_m = (0.0 /: x_i){_ + _} / x_i.length
//      Alternatively:
//      val x_m =  x_i.foldLeft(0.0)(
//        {
//          (z,l) =>
//          z + l
//        }
//      )/x_i.size
      x_m

    }catch{
      case ex:IndexOutOfBoundsException => {
        print("Not enough values")
        0d
      }
    }
  }

  def calculateVariance(x_i:Seq[Double]) : Double = {
    val x_m = calculateMean(x_i)
    val x_2 = x_i.foldLeft(0.0d)((r,i)=>{
    val x_i2 = math.pow(i - x_m,2)
      r + x_i2
    })
    x_2 / x_i.length
  }

  def calculateSD(x_i:Seq[Double]) : Double = {
    if(!x_i.isEmpty && x_i.size>1)
      math.sqrt(calculateVariance(x_i))
    else
      -1.0
  }



/*  def calculateSDSample (x_i:Seq[Double]) = {
    try{
      val x_m = calculateMean(x_i)
      val sd = x_i.map(
        (total:Double,i:Double) => (
          {
            val x = i
            val r = x - x_m
            x
          }
        )
      )

    }catch{
      case ex:IndexOutOfBoundsException => {
        print("Not enough values")
      }
    }
  }
 */

}
