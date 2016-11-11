package org.mag.tp.domain

import scala.concurrent.duration.DurationDouble
import scala.language.postfixOps

import org.mag.tp.domain.Employee.LoiterBehaviour
import org.mag.tp.domain.Employee.WorkBehaviour
import org.mag.tp.domain.WorkArea.Broadcastability
import org.mag.tp.domain.WorkArea.EmployeeCount
import org.mag.tp.domain.Employee._
import org.mag.tp.util.ProbabilityBag

import com.softwaremill.tagging.{ @@ => @@ }
import com.softwaremill.tagging.Tagger
import com.softwaremill.macwire.wireWith

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import scala.util.Random

trait DomainModule {
  val inertia = 10.taggedWith[Inertia]
  //  val cyclicity = 1D.taggedWith[Cyclicity]
  //  val permeability = 0.1D.taggedWith[Permeability]
  val employeeTimerFreq = (0.1 seconds).taggedWith[Employee.TimerFreq]

  val targetEmployeeCount = 100.taggedWith[EmployeeCount]
  val broadcastability = 5.taggedWith[Broadcastability]

  val employerTimerFreq = (employeeTimerFreq * 5).taggedWith[Employer.TimerFreq]

  var laziesToCreate = 40
  def employeePropsFactory(workArea: ActorRef @@ WorkArea): Props @@ Employee = {
    val (behaviour, cyclicity, permeability) = if (laziesToCreate > 0)
      (ProbabilityBag.complete[Employee.Behaviour](WorkBehaviour -> 0, LoiterBehaviour -> 1),
        1D.taggedWith[Cyclicity],
        0.05D.taggedWith[Permeability])
    else
      (ProbabilityBag.complete[Employee.Behaviour](WorkBehaviour -> 1, LoiterBehaviour -> 0),
        1D.taggedWith[Cyclicity],
        0.05D.taggedWith[Permeability])

    laziesToCreate -= 1
    wireWith(Employee.props _).taggedWith[Employee]
  }

  lazy val employerPropsFactory = (workArea: ActorRef @@ WorkArea) =>
    wireWith(Employer.props _).taggedWith[Employer]

  def createWorkArea(): ActorRef @@ WorkArea = {
    val employeeProps = employeePropsFactory _
    val employerProps = employerPropsFactory _
    system.actorOf(wireWith(WorkArea.props _), "work-area").taggedWith[WorkArea]
  }

  def system: ActorSystem
  def loggers: Traversable[ActorRef]
}
