package org.mag.tp.ui

import akka.actor.{ActorRef, ActorSystem, Props}
import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{@@, Tagger}
import org.mag.tp.domain.DomainModule

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

class FrontendModule(system: ActorSystem,
                     val statsLoggerTimerFreq: FiniteDuration,
                     employeeTimerFreq: FiniteDuration,
                     workingEmployeesCount: Int,
                     loiteringEmployeesCount: Int,
                     memory: Option[Int],
                     broadcastability: Int,
                     workersPermeabilityAtStart: Double,
                     loiterersPermeabilityAtStart: Double)
  extends DomainModule(// XXX
    system,
    employeeTimerFreq: FiniteDuration,
    workingEmployeesCount,
    loiteringEmployeesCount,
    memory,
    broadcastability,
    workersPermeabilityAtStart,
    loiterersPermeabilityAtStart
  ) {
  val _statsLoggerTimerFreq = statsLoggerTimerFreq.taggedWith[StatsLogger.TimerFreq]

  def statsLoggerPropsFactory(frontendActor: ActorRef @@ FrontendActor): Props @@ StatsLogger =
    Props(wire[StatsLogger]).taggedWith[StatsLogger]

  def createFrontendActor(): ActorRef @@ FrontendActor = {
    reset() // XXX
    val workAreaProps = workAreaPropsFactory _
    val statsLoggerProps = statsLoggerPropsFactory _
    system.actorOf(Props(wire[FrontendActor])).taggedWith[FrontendActor]
    // , "frontend"
  }
}
