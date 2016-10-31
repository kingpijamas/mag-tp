package org.mag.tp.domain.ui

import akka.actor.Actor
import akka.actor.ActorRef
import org.atmosphere.cpr.AtmosphereResourceFactory
import org.atmosphere.cpr.AtmosphereConfig

class FrontendActor extends Actor {
  def receive = {
    case (uuid: String, str: String) =>
      println(uuid, str)
      //     clusterClient ! ClusterClient.Send("/user/backendSingletonProxyActor", PrintString(self, uuid, str), localAffinity = false)
      AtmosphereResourceFactory.getDefault.find(uuid).getBroadcaster.broadcast("WORK QUEUED")
    // case WorkComplete(uuid)          =>
    // AtmosphereResourceFactory.getDefault.find(uuid).getBroadcaster.broadcast("WORK COMPLETE")
  }
}
