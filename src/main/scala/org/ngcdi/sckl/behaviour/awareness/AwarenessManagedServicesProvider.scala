package org.ngcdi.sckl.behaviour.awareness

import scala.concurrent.Promise
import org.ngcdi.sckl.awareness.AwarenessService

trait AwarenessManagedServicesProvider {
  private val awarenessManagedServicesPromise = Promise[Seq[AwarenessService]]()
  val awarenessManagedServices = awarenessManagedServicesPromise.future

  def setAwarenessManagedServices(services: Seq[AwarenessService]) = {
    awarenessManagedServicesPromise.success(services)
  }
}