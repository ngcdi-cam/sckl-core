package org.ngcdi.sckl.behaviour.awareness

import scala.concurrent.Promise
import org.ngcdi.sckl.ryuclient.NetworkAwarenessCrossDomainOptimizer

trait AwarenessCrossDomainOptimizerProvider {
  private val awarenessCrossDomainOptimizerPromise = Promise[NetworkAwarenessCrossDomainOptimizer]()
  val awarenessCrossDomainOptimizer = awarenessCrossDomainOptimizerPromise.future

  def setAwarenessCrossDomainOptimizer(optimizer: NetworkAwarenessCrossDomainOptimizer) = {
    awarenessCrossDomainOptimizerPromise.success(optimizer)
  }
}