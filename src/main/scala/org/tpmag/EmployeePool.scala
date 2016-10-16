package org.tpmag

import scala.concurrent.duration.FiniteDuration

import Employee.RandomBehaviour
import Employee.Steal
import Employee.Work
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.routing.ActorRefRoutee
import akka.routing.RoundRobinRoutingLogic
import akka.routing.Router

object EmployeePool {
  def props(targetEmployeeCount: Int,
            workPropensity: Double,
            stealingPropensity: Double,
            timerFreq: FiniteDuration,
            productionSupervisor: ActorRef,
            warehouse: ActorRef): Props =
    Props(new EmployeePool(
      targetEmployeeCount, workPropensity, stealingPropensity, timerFreq, productionSupervisor, warehouse))
}

class EmployeePool(
  targetEmployeeCount: Int,
  workPropensity: Double,
  stealingPropensity: Double,
  timerFreq: FiniteDuration,
  productionSupervisor: ActorRef,
  warehouse: ActorRef)
    extends Actor {

  // NOTE: temporary, make this per-employee
  val Behaviours = ProbabilityBag.complete[RandomBehaviour](
    workPropensity -> Work,
    stealingPropensity -> Steal)

  var nextId = 0
  var employeeCount = 0
  var router = {
    val employees = Vector.fill(targetEmployeeCount) { ActorRefRoutee(hireEmployee()) }
    Router(RoundRobinRoutingLogic(), employees)
  }

  def receive = {
    case Terminated(employee) => {
      // println(s"$employee fired (#employees = ${employees.size})")
      router = router.removeRoutee(employee)
      employeeCount -= 1

      val newEmployee = hireEmployee()
      router = router.addRoutee(newEmployee)
      // println(s"$newEmployee hired (#employees = ${employeeCount})")
    }
  }

  def hireEmployee(): ActorRef = {
    val employee = context.actorOf(
      Employee.props(Behaviours, timerFreq, productionSupervisor, warehouse),
      s"employee$nextId")

    context.watch(employee)
    employeeCount += 1
    nextId += 1
    employee
  }
}
