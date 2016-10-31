package org.mag.tp

import org.mag.tp.ui.FrontendActor

import com.softwaremill.macwire._
import com.softwaremill.tagging._

import akka.actor.ActorRef
import org.mag.tp.ui.FrontendModule

trait ControllersModule {
  def frontendActor: ActorRef @@ FrontendActor

  lazy val scalatraController = wire[MyScalatraController]
  lazy val uiController = wire[UIController]
}
