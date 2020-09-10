package org.ngcdi.sckl.behaviour.awareness

import scala.concurrent.Promise
import org.ngcdi.sckl.ryuclient.NetworkAwarenessService

trait AwarenessManagedGlobalServicesProvider {
  private val awarenessManagedGlobalServicesPromise = Promise[Seq[NetworkAwarenessService]]()
  val awarenessManagedGlobalServices = awarenessManagedGlobalServicesPromise.future

  def setAwarenessManagedGlobalServices(services: Seq[NetworkAwarenessService]) = {
    awarenessManagedGlobalServicesPromise.success(services)
  }
}