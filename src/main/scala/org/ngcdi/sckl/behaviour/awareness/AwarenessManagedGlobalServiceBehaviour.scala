package org.ngcdi.sckl.behaviour.awareness

import scala.concurrent.Promise
import org.ngcdi.sckl.awareness.AwarenessService

trait AwarenessManagedGlobalServicesProvider {
  private val awarenessManagedGlobalServicesPromise = Promise[Seq[AwarenessService]]()
  val awarenessManagedGlobalServices = awarenessManagedGlobalServicesPromise.future

  def setAwarenessManagedGlobalServices(services: Seq[AwarenessService]) = {
    awarenessManagedGlobalServicesPromise.success(services)
  }
}