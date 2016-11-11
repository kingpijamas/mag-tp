package org.mag.tp.ui

import scala.concurrent.duration.DurationDouble
import scala.language.postfixOps

import org.mag.tp.domain.DomainModule

import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{ @@ => @@ }
import com.softwaremill.tagging.Tagger

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

trait FrontendModule extends DomainModule {
  def createFrontendActor(): ActorRef @@ FrontendActor = {
    val workAreaProps = workAreaPropsFactory _
    system.actorOf(Props(wire[FrontendActor]), "frontend").taggedWith[FrontendActor]
  }

  val batchSize = 10 // FIXME: ignored!
  val workLoggerTimerFreq = 0.5 seconds

  def createWorkLogger(frontendActor: ActorRef): ActorRef @@ WorkLogger = {
    system.actorOf(Props(wire[WorkLogger]), "work-logger").taggedWith[WorkLogger]
  }

  def system: ActorSystem
}
