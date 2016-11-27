package org.mag.tp.domain

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.routing.{ActorRefRoutee, RandomRoutingLogic, Router}
import com.softwaremill.tagging.{@@, Tagger}
import org.mag.tp.domain.WorkArea.{Broadcastability, EmployeeCount}
import org.mag.tp.util.{MandatoryBroadcastingActor, PartiallyBroadcastingActor}

object WorkArea {
  // messages
  sealed trait Action
  object Work extends Action
  object Loiter extends Action

  // type annotations
  trait EmployeeCount
  trait Broadcastability
}

class WorkArea(val targetEmployeeCount: Int @@ EmployeeCount,
               val broadcastability: Int @@ Broadcastability,
               val employeePropsFactory: (ActorRef @@ WorkArea => Props @@ Employee),
               // FIXME: consider crashes!
               val mandatoryBroadcastables: Traversable[ActorRef])
  extends Actor with PartiallyBroadcastingActor with MandatoryBroadcastingActor {

  var nextId = 0
  var employeeCount = 0
  var partiallyBroadcastables: Router = {
    val employees = Vector.fill(targetEmployeeCount) {
      ActorRefRoutee(hireEmployee())
    }
    Router(RandomRoutingLogic(), employees)
  }

  def receive: Receive = {
    case Terminated(employee) =>
      // println(s"$employee fired (#employees = ${employees.size})")
      partiallyBroadcastables = partiallyBroadcastables.removeRoutee(employee)
      employeeCount -= 1

      val newEmployee = hireEmployee()
      partiallyBroadcastables = partiallyBroadcastables.addRoutee(newEmployee)
    // println(s"$newEmployee hired (#employees = ${employeeCount})")

    case msg: Any =>
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
