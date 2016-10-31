package org.mag.tp.ui

import org.atmosphere.cpr.AtmosphereResourceFactory

import akka.actor.Actor

object FrontendActor {
  case class Connection(clientUuid: String)
}

class FrontendActor extends Actor {
  import FrontendActor._

  def receive: Receive = {
    case Connection(clientUuid: String) =>
      println(clientUuid)
      AtmosphereResourceFactory.getDefault.find(clientUuid).getBroadcaster.broadcast("Welcome!")
    // case WorkComplete(uuid)          =>
    // AtmosphereResourceFactory.getDefault.find(uuid).getBroadcaster.broadcast("WORK COMPLETE")
  }
}