package org.mag.tp.ui

import akka.actor.{ActorRef, ActorSystem, Props}
import com.softwaremill.macwire._
import com.softwaremill.tagging.{@@, Tagger}
import org.mag.tp.domain.DomainModule

import scala.concurrent.duration.DurationDouble
import scala.language.postfixOps

trait FrontendModule extends DomainModule {
  val statsLoggerTimerFreq = (0.5 seconds).taggedWith[StatsLogger.TimerFreq]

  def statsLoggerPropsFactory(frontendActor: ActorRef @@ FrontendActor): Props @@ StatsLogger =
    Props(wire[StatsLogger]).taggedWith[StatsLogger]

  def createFrontendActor(): ActorRef @@ FrontendActor = {
    val workAreaProps = workAreaPropsFactory _
    val statsLoggerProps = statsLoggerPropsFactory _
    system.actorOf(Props(wire[FrontendActor]), "frontend").taggedWith[FrontendActor]
  }

  def system: ActorSystem
}
