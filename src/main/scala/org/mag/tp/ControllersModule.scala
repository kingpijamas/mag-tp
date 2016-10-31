package org.mag.tp

import org.mag.tp.ui.FrontendActor

import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{ @@ => @@ }

import akka.actor.ActorRef

trait ControllersModule {
  def frontendActor: ActorRef @@ FrontendActor

  lazy val scalatraController = wire[MyScalatraController]
  lazy val uiController = wire[UIController]
}
