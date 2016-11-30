package org.mag.tp.domain

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.routing.{ActorRefRoutee, Broadcast, RandomRoutingLogic, Router}
import com.softwaremill.tagging.{@@, Tagger}
import org.mag.tp.domain.WorkArea.{Broadcastability, EmployeeCount}
import org.mag.tp.util.PausableActor.{Pause, Resume}
import org.mag.tp.util.{MandatoryBroadcastingActor, PartiallyBroadcastingActor, PausableActor}

object WorkArea {
  // messages
  sealed trait Action
  object Work extends Action
  object Loiter extends Action

  // type annotations
  sealed trait TypeAnnotation
  trait EmployeeCount extends TypeAnnotation
  trait Broadcastability extends TypeAnnotation
}

class WorkArea(val targetEmployeeCount: Int @@ EmployeeCount,
               val broadcastability: Int @@ Broadcastability,
               val employeePropsFactory: (ActorRef @@ WorkArea => Props @@ Employee),
               // FIXME: consider crashes!
               val mandatoryBroadcastables: Traversable[ActorRef])
  extends Actor with PartiallyBroadcastingActor with MandatoryBroadcastingActor with PausableActor {

  var nextId = 0
  var employeeCount = 0
  var partiallyBroadcastables: Router = {
    val employees = Vector.fill(targetEmployeeCount) {
      ActorRefRoutee(hireEmployee())
    }
    Router(RandomRoutingLogic(), employees)
  }

  def receive: Receive = respectPauses orElse {
    case Terminated(employee) =>
      partiallyBroadcastables = partiallyBroadcastables.removeRoutee(employee)
      employeeCount -= 1

      val newEmployee = hireEmployee()
      partiallyBroadcastables = partiallyBroadcastables.addRoutee(newEmployee)

    case msg: Any =>
      partialBroadcast(msg)
      mandatoryBroadcast(msg)
  }

  private[this] def hireEmployee(): ActorRef = {
    val employeeProps = employeePropsFactory(self.taggedWith[WorkArea])
    val employee = context.actorOf(employeeProps) //, s"employee-$nextId")

    context.watch(employee)
    employeeCount += 1
    nextId += 1
    employee
  }

  override def onPauseStart(): Unit = {
    super.onPauseStart()
    partiallyBroadcastables.routees.foreach(_.send(Pause, sender))
  }

  override def onPauseEnd(): Unit = {
    super.onPauseEnd()
    partiallyBroadcastables.routees.foreach(_.send(Resume, sender))
  }
}
