package org.mag.tp.ui

import akka.actor.{ActorRef, ActorSystem, Props}
import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{@@, Tagger}
import org.mag.tp.domain.DomainModule

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

trait FrontendModule {
  self: DomainModule =>

  def system: ActorSystem

  def statsLoggerTimerFreq: FiniteDuration @@ StatsLogger.TimerFreq

  def statsLoggerPropsFactory(frontendActor: ActorRef @@ FrontendActor): Props @@ StatsLogger =
    Props(wire[StatsLogger]).taggedWith[StatsLogger]

  def createFrontendActor(): ActorRef @@ FrontendActor = {
    val workAreaProps = workAreaPropsFactory _
    val statsLoggerProps = statsLoggerPropsFactory _
    system.actorOf(Props(wire[FrontendActor])).taggedWith[FrontendActor]
    // , "frontend"
  }
}
