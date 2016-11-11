package org.mag.tp.domain

import org.mag.tp.util.MandatoryBroadcastingActor
import org.mag.tp.util.PartiallyBroadcastingActor

import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{ @@ => @@ }
import com.softwaremill.tagging.Tagger

import WorkArea.Broadcastability
import WorkArea.EmployeeCount
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.routing.ActorRefRoutee
import akka.routing.RandomRoutingLogic
import akka.routing.Router

object WorkArea {
  // messages
  sealed trait Actions
  object Work extends Actions
  object Loiter extends Actions

  // type annotations
  trait EmployeeCount
  trait Broadcastability

  def props(targetEmployeeCount: Int @@ EmployeeCount,
            broadcastability: Int @@ Broadcastability,
            employeePropsFactory: (ActorRef @@ WorkArea => Props @@ Employee),
            employerPropsFactory: (ActorRef @@ WorkArea => Props @@ Employer),
            mandatoryBroadcastables: Traversable[ActorRef]): Props =
    Props(wire[WorkArea])
}

class WorkArea(
  val targetEmployeeCount: Int @@ EmployeeCount,
  val broadcastability: Int @@ Broadcastability,
  val employeePropsFactory: (ActorRef @@ WorkArea => Props @@ Employee),
  val employerPropsFactory: (ActorRef @@ WorkArea => Props @@ Employer),
  val baseMandatoryBroadcastables: Traversable[ActorRef])
    extends Actor with PartiallyBroadcastingActor with MandatoryBroadcastingActor {

  println(s"$self: alive and well!")

  // FIXME: consider crashes!
  val employer = context.actorOf(employerPropsFactory(self.taggedWith[WorkArea]), "employer")
  val mandatoryBroadcastables = baseMandatoryBroadcastables ++ Seq(employer)

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
      partialBroadcast(msg)
      mandatoryBroadcast(msg)
  }

  private[this] def hireEmployee(): ActorRef = {
    val employeeProps = employeePropsFactory(self.taggedWith[WorkArea])
    val employee = context.actorOf(employeeProps, s"employee-$nextId")

    context.watch(employee)
    employeeCount += 1
    nextId += 1
    employee
  }
}
