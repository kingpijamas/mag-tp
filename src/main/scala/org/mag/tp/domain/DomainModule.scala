package org.mag.tp.domain

import scala.concurrent.duration.DurationDouble
import scala.language.postfixOps

import org.mag.tp.domain.Employee.LoiterBehaviour
import org.mag.tp.domain.Employee.WorkBehaviour
import org.mag.tp.domain.WorkArea.Broadcastability
import org.mag.tp.domain.WorkArea.EmployeeCount
import org.mag.tp.util.ProbabilityBag

import com.softwaremill.tagging.{ @@ => @@ }
import com.softwaremill.tagging.Tagger
import com.softwaremill.macwire.wireWith

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

trait DomainModule {
  val behaviours = ProbabilityBag.complete[Employee.Behaviour](
    WorkBehaviour -> 0.75,
    LoiterBehaviour -> 0.25)

  val envy = 0.25
  val targetEmployeeCount = 5.taggedWith[EmployeeCount]
  val broadcastability = 5.taggedWith[Broadcastability]
  val employeeTimerFreq = (0.5 seconds).taggedWith[Employee.TimerFreq]
  val employerTimerFreq = (employeeTimerFreq * 5).taggedWith[Employer.TimerFreq]

  lazy val employeePropsFactory = (workArea: ActorRef @@ WorkArea) =>
    wireWith(Employee.props _).taggedWith[Employee]

  lazy val employerPropsFactory = (workArea: ActorRef @@ WorkArea) =>
    wireWith(Employer.props _).taggedWith[Employer]

  def createWorkArea(): ActorRef @@ WorkArea = {
    val employeeProps = employeePropsFactory _
    val employerProps = employerPropsFactory _
    system.actorOf(wireWith(WorkArea.props _)).taggedWith[WorkArea]
  }

  def system: ActorSystem
  def loggers: Traversable[ActorRef]
}
