package org.mag.tp.controller

import akka.actor.ActorRef
import com.softwaremill.macwire._
import com.softwaremill.tagging._
import org.mag.tp.ui.FrontendActor


trait ControllersModule {
  def frontendActor: ActorRef @@ FrontendActor

  lazy val uiController = wire[UIController]
}
