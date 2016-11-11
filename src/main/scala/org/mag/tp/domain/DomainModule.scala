package org.mag.tp.domain

import scala.concurrent.duration.DurationDouble
import scala.language.postfixOps
import scala.util.Random

import org.mag.tp.domain.Employee.Cyclicity
import org.mag.tp.domain.Employee.Inertia
import org.mag.tp.domain.Employee.LoiterBehaviour
import org.mag.tp.domain.Employee.Permeability
import org.mag.tp.domain.Employee.WorkBehaviour
import org.mag.tp.domain.WorkArea.Broadcastability
import org.mag.tp.domain.WorkArea.EmployeeCount
import org.mag.tp.util.ProbabilityBag

import com.softwaremill.macwire.wireWith
import com.softwaremill.tagging.{ @@ => @@ }
import com.softwaremill.tagging.Tagger

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

trait DomainModule {
  val inertia = 10.taggedWith[Inertia]
  val cyclicity = 1D.taggedWith[Cyclicity]
  val permeability = 0.1D.taggedWith[Permeability]
  val employeeTimerFreq = (0.1 seconds).taggedWith[Employee.TimerFreq]

  val targetEmployeeCount = 5.taggedWith[EmployeeCount]
  val broadcastability = 5.taggedWith[Broadcastability]

  val employerTimerFreq = (employeeTimerFreq * 5).taggedWith[Employer.TimerFreq]

  def employeePropsFactory(workArea: ActorRef @@ WorkArea): Props @@ Employee = {
    val (behaviour, cyclicity, permeability) = if (Random.nextDouble < 0.4)
      (ProbabilityBag.complete[Employee.Behaviour](WorkBehaviour -> 0, LoiterBehaviour -> 1),
        1D.taggedWith[Cyclicity],
        0.05D.taggedWith[Permeability])
    else
      (ProbabilityBag.complete[Employee.Behaviour](WorkBehaviour -> 1, LoiterBehaviour -> 0),
        1D.taggedWith[Cyclicity],
        0.05D.taggedWith[Permeability])

    wireWith(Employee.props _).taggedWith[Employee]
  }

  def employerPropsFactory(workArea: ActorRef @@ WorkArea): Props @@ Employer =
    wireWith(Employer.props _).taggedWith[Employer]

  lazy val workAreaPropsFactory = () => {
    val employeeProps = employeePropsFactory _
    val employerProps = employerPropsFactory _
    wireWith(WorkArea.props _).taggedWith[WorkArea]
  }

  def system: ActorSystem
  def loggers: Traversable[ActorRef]
}
