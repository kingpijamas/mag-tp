package org.mag.tp.ui

import akka.actor.{ActorRef, ActorSystem, Props}
import com.softwaremill.macwire._
import com.softwaremill.tagging.{@@, Tagger}
import org.mag.tp.domain.DomainModule

import scala.concurrent.duration.DurationDouble
import scala.language.postfixOps

trait FrontendModule extends DomainModule {
  def workLoggerPropsFactory(frontendActor: ActorRef @@ FrontendActor): Props @@ WorkLogger =
    Props(wire[WorkLogger]).taggedWith[WorkLogger]

  def createFrontendActor(): ActorRef @@ FrontendActor = {
    val workAreaProps = workAreaPropsFactory _
    val workLoggerProps = workLoggerPropsFactory _
    system.actorOf(Props(wire[FrontendActor]), "frontend").taggedWith[FrontendActor]
  }

  val batchSize = 10 // FIXME: ignored!
  val workLoggerTimerFreq = 0.5 seconds

  def system: ActorSystem
}
