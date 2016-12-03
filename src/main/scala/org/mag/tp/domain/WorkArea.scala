package org.mag.tp.domain

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.routing.{ActorRefRoutee, RandomRoutingLogic, Router}
import com.softwaremill.tagging.{@@, Tagger}
import org.mag.tp.util.PausableActor.{Pause, Resume}
import org.mag.tp.util.{MandatoryBroadcastingActor, PartiallyBroadcastingActor, PausableActor}

import scala.collection.{immutable, mutable}

object WorkArea {
  // messages
  sealed trait Action {
    def group: Employee.Group
  }
  case class Work(group: Employee.Group) extends Action
  case class Loiter(group: Employee.Group) extends Action
}

class WorkArea(val groups: immutable.Seq[Employee.Group],
               val visibility: Int,
               val employeePropsFactory: (ActorRef @@ WorkArea, Employee.Group) => (Props @@ Employee),
               // FIXME: consider crashes!
               val mandatoryBroadcastables: Traversable[ActorRef])
  extends Actor with PartiallyBroadcastingActor with MandatoryBroadcastingActor with PausableActor {

  var nextId = 0
  var employeesPerGroup = mutable.Map(groups.map(_ -> mutable.Set[ActorRef]()): _*)

  var employees: Router = {
    val targetEmployeeCounts = groups map (_.targetSize)
    val employees = Vector.fill(targetEmployeeCounts.sum) {
      ActorRefRoutee(hireEmployee())
    }
    Router(RandomRoutingLogic(), employees)
  }

  def partiallyBroadcastables: Router = employees

  def receive: Receive = respectPauses orElse {
    case Terminated(employee) =>
      employees = employees.removeRoutee(employee)
      val Some(groupEmployees) = employeesPerGroup collectFirst {
        case (_, employees) if employees.contains(employee) => employees
      }
      groupEmployees -= employee
      val newEmployee = hireEmployee()
      employees = employees.addRoutee(newEmployee)

    case msg: Any =>
      partialBroadcast(msg)
      mandatoryBroadcast(msg)
  }

  private[this] def hireEmployee(): ActorRef = {
    val groupsToComplete = employeesPerGroup filter {
      case (group, groupEmployees) => groupEmployees.size < group.targetSize
    }
    val (leastPopulatedGroup, employeesInLeastPopulatedGroup) = groupsToComplete.minBy {
      case (_, employees) => employees.size
    }
    val employeeProps = employeePropsFactory(self.taggedWith[WorkArea], leastPopulatedGroup)
    val employee = context.actorOf(employeeProps) //, s"employee-$nextId")

    context.watch(employee)
    employeesInLeastPopulatedGroup += employee
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
