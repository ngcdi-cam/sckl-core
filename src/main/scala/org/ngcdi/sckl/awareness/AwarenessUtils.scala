package org.ngcdi.sckl.awareness

object AwarenessUtils {
  def lookUpServiceByIp(services: Seq[AwarenessService], ip: String, srcOnly: Boolean = false) = {
    services.filter { p => 
      p.src == ip || (!srcOnly && p.dst == ip)
    }
  }

  def lookUpServiceBySrcDstIp(services: Seq[AwarenessService], srcIp: String, dstIp: String) = {
    services.filter { p => 
      (p.src == srcIp && p.dst == dstIp) || (p.src == dstIp && p.dst == srcIp)
    }
  }
}