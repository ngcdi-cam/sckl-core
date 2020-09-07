package org.ngcdi.sckl.behaviour.awareness

import scala.concurrent.Promise
import org.ngcdi.sckl.ryuclient.NetworkAwarenessService

trait AwarenessManagedServicesProvider {
  private val awarenessManagedServicesPromise = Promise[Seq[NetworkAwarenessService]]()
  val awarenessManagedServices = awarenessManagedServicesPromise.future

  def setAwarenessManagedServices(services: Seq[NetworkAwarenessService]) = {
    awarenessManagedServicesPromise.success(services)
  }
}