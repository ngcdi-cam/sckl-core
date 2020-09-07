package org.ngcdi.sckl.ryuclient

object NetworkAwarenessUtils {
  def lookUpServiceByIp(services: Seq[NetworkAwarenessService], ip: String) = {
    services.filter { p => 
      p.src == ip || p.dst == ip
    }
  }

  def lookUpServiceBySrcDstIp(services: Seq[NetworkAwarenessService], srcIp: String, dstIp: String) = {
    services.filter { p => 
      (p.src == srcIp && p.dst == dstIp) || (p.src == dstIp && p.dst == srcIp)
    }
  }
}