package org.mag.tp.controller

import akka.actor.ActorSystem
import com.softwaremill.macwire.wire

trait ControllersModule {
  //  def frontendActor: ActorRef @@ FrontendActor
  def system: ActorSystem

  lazy val uiController = wire[UIController]
}
