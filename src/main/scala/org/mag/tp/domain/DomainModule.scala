package org.mag.tp.domain

import scala.collection.immutable
import scala.concurrent.duration.DurationDouble
import scala.language.postfixOps

import com.softwaremill.macwire._
import com.softwaremill.macwire.tagging._
import akka.actor.ActorSystem
import scala.collection.immutable
import org.mag.tp.domain.Employee.LoiterBehaviour
import org.mag.tp.domain.Employee.WorkBehaviour
import org.mag.tp.domain.WorkArea.Broadcastability
import org.mag.tp.domain.WorkArea.EmployeeCount
import org.mag.tp.domain.WorkArea.EmployeeTimerFreq
import org.mag.tp.domain.WorkArea.EmployerTimerFreq
import akka.actor.Props
import org.mag.tp.util.ProbabilityBag

trait DomainModule {
  val behaviours = ProbabilityBag.complete[Employee.Behaviour](
    WorkBehaviour -> 0.75,
    LoiterBehaviour -> 0.25)

  val envy = 0.25
  val targetEmployeeCount = 5.taggedWith[EmployeeCount]
  val broadcastability = 5.taggedWith[Broadcastability]
  val employeeTimerFreq = (0.5 seconds).taggedWith[EmployeeTimerFreq]
  val employerTimerFreq = (employeeTimerFreq * 5).taggedWith[EmployerTimerFreq]

  def createWorkArea() = {
    system.actorOf(Props(wire[WorkArea]))
  }

  def system: ActorSystem
}
