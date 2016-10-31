package org.mag.tp.domain

import scala.concurrent.duration.FiniteDuration

import org.mag.tp.util.ProbabilityBag

import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{ @@ => @@ }
import com.softwaremill.tagging.Tagger

import Employee.Behaviour
import WorkArea.Broadcastability
import WorkArea.EmployeeCount
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.routing.ActorRefRoutee
import akka.routing.RandomRoutingLogic
import akka.routing.Router
import org.mag.tp.util.PartiallyBroadcastingActor

object WorkArea {
  // messages
  object Work
  object Loiter

  // type annotations
  trait EmployeeCount
  trait Broadcastability

  def props(targetEmployeeCount: Int @@ EmployeeCount,
            broadcastability: Int @@ Broadcastability,
            employeePropsFactory: (ActorRef @@ WorkArea => Props @@ Employee),
            employerPropsFactory: (ActorRef @@ WorkArea => Props @@ Employer)): Props =
    Props(wire[WorkArea])
}

class WorkArea(
  val targetEmployeeCount: Int @@ EmployeeCount,
  val broadcastability: Int @@ Broadcastability,
  val employeePropsFactory: (ActorRef @@ WorkArea => Props @@ Employee),
  val employerPropsFactory: (ActorRef @@ WorkArea => Props @@ Employer))
    extends Actor with PartiallyBroadcastingActor {

  var employer = context.actorOf(employerPropsFactory(self.taggedWith[WorkArea]))

  var nextId = 0
  var employeeCount = 0
  var partiallyBroadcastables: Router = {
    val employees = Vector.fill(targetEmployeeCount) { ActorRefRoutee(hireEmployee()) }
    Router(RandomRoutingLogic(), employees)
  }

  def receive = {
    case Terminated(employee) =>
      // println(s"$employee fired (#employees = ${employees.size})")
      partiallyBroadcastables = partiallyBroadcastables.removeRoutee(employee)
      employeeCount -= 1

      val newEmployee = hireEmployee()
      partiallyBroadcastables = partiallyBroadcastables.addRoutee(newEmployee)
    // println(s"$newEmployee hired (#employees = ${employeeCount})")

    case msg =>
      broadcast(msg)
      employer.tell(msg, sender)
  }

  private[this] def hireEmployee(): ActorRef = {
    val employeeProps = employeePropsFactory(self.taggedWith[WorkArea])
    val employee = context.actorOf(employeeProps, s"employee$nextId")

    context.watch(employee)
    employeeCount += 1
    nextId += 1
    employee
  }
}
