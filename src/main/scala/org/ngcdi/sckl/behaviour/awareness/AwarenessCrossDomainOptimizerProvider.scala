package org.ngcdi.sckl.behaviour.awareness

import scala.concurrent.Promise
import org.ngcdi.sckl.awareness.AwarenessCrossDomainOptimizer

trait AwarenessCrossDomainOptimizerProvider {
  private val awarenessCrossDomainOptimizerPromise = Promise[AwarenessCrossDomainOptimizer]()
  val awarenessCrossDomainOptimizer = awarenessCrossDomainOptimizerPromise.future

  def setAwarenessCrossDomainOptimizer(optimizer: AwarenessCrossDomainOptimizer) = {
    awarenessCrossDomainOptimizerPromise.success(optimizer)
  }
}