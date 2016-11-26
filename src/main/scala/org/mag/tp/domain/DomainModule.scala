package org.mag.tp.domain

import akka.actor.{ActorRef, ActorSystem, Props}
import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{@@, Tagger}
import org.mag.tp.domain.Employee._
import org.mag.tp.domain.WorkArea.{Broadcastability, EmployeeCount}
import org.mag.tp.util.ProbabilityBag

import scala.concurrent.duration.DurationDouble
import scala.language.postfixOps
import scala.util.Random

trait DomainModule {
  val inertia = 10.taggedWith[Inertia]
  val permeability = 0.1D.taggedWith[Permeability]
  val employeeTimerFreq = (0.1 seconds).taggedWith[Employee.TimerFreq]

  val targetEmployeeCount = 10.taggedWith[EmployeeCount]
  val broadcastability = 5.taggedWith[Broadcastability]

  val employerTimerFreq = (employeeTimerFreq * 5).taggedWith[Employer.TimerFreq]

  def employeePropsFactory(workArea: ActorRef @@ WorkArea): Props @@ Employee = {
    val (behaviour, permeability) = if (Random.nextDouble < 0.5)
      (ProbabilityBag.complete[Employee.Behaviour](WorkBehaviour -> 0, LoiterBehaviour -> 1),
        0.9D.taggedWith[Permeability])
    else
      (ProbabilityBag.complete[Employee.Behaviour](WorkBehaviour -> 1, LoiterBehaviour -> 0),
        0D.taggedWith[Permeability])

    Props(wire[Employee]).taggedWith[Employee]
  }

  def employerPropsFactory(workArea: ActorRef @@ WorkArea): Props @@ Employer =
    Props(wire[Employer]).taggedWith[Employer]

  def workAreaPropsFactory(mandatoryBroadcastables: Traversable[ActorRef]): Props @@ WorkArea = {
    val employeeProps = employeePropsFactory _
    val employerProps = employerPropsFactory _
    Props(wire[WorkArea]).taggedWith[WorkArea]
  }

  def system: ActorSystem
}
