package org.tpmag.domain

import scala.concurrent.duration.FiniteDuration

import org.tpmag.util.ProbabilityBag

import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{ @@ => @@ }
import com.softwaremill.tagging.Tagger

import Employee.Behaviour
import WorkArea.Broadcastability
import WorkArea.EmployeeCount
import WorkArea.EmployeeTimerFreq
import WorkArea.EmployerTimerFreq
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.routing.ActorRefRoutee
import akka.routing.RandomRoutingLogic
import akka.routing.Router

object WorkArea {
  // messages
  object Work
  object Loiter

  // type annotations
  trait EmployeeCount
  trait Broadcastability
  trait EmployerTimerFreq
  trait EmployeeTimerFreq

  def props(targetEmployeeCount: Int @@ EmployeeCount,
            broadcastability: Int @@ Broadcastability,
            envy: Double,
            behaviours: Seq[(Behaviour, Double)],
            employeeTimerFreq: FiniteDuration @@ EmployeeTimerFreq,
            employerTimerFreq: FiniteDuration @@ EmployerTimerFreq): Props = {
    val behavioursBag = ProbabilityBag.complete(behaviours: _*) // TODO: make this per-employee
    Props(wire[WorkArea])
  }
}

class WorkArea(
  val targetEmployeeCount: Int @@ EmployeeCount,
  val broadcastability: Int @@ Broadcastability,
  val envy: Double,
  val behaviours: ProbabilityBag[Employee.Behaviour],
  val employeeTimerFreq: FiniteDuration @@ EmployeeTimerFreq,
  val employerTimerFreq: FiniteDuration @@ EmployerTimerFreq)
    extends Actor {

  var employer = context.actorOf(Employer.props(employerTimerFreq, self.taggedWith[WorkArea]))

  var nextId = 0
  var employeeCount = 0
  var router = {
    val employees = Vector.fill(targetEmployeeCount) { ActorRefRoutee(hireEmployee()) }
    Router(RandomRoutingLogic(), employees)
  }

  def receive = {
    case Terminated(employee) =>
      // println(s"$employee fired (#employees = ${employees.size})")
      router = router.removeRoutee(employee)
      employeeCount -= 1

      val newEmployee = hireEmployee()
      router = router.addRoutee(newEmployee)
    // println(s"$newEmployee hired (#employees = ${employeeCount})")

    case msg =>
      (0 until broadcastability).foreach { _ =>
        router.route(msg, sender)
      }
      employer.tell(msg, sender)
  }

  private[this] def hireEmployee(): ActorRef = {
    val employeeProps = Employee.props(envy, behaviours, employeeTimerFreq, self.taggedWith[WorkArea])
    val employee = context.actorOf(employeeProps, s"employee$nextId")

    context.watch(employee)
    employeeCount += 1
    nextId += 1
    employee
  }
}
