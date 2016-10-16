package org.tpmag

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

import Employee._
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.routing.ActorRefRoutee
import akka.routing.Router
import akka.routing.RoundRobinRoutingLogic

object EmployeePool {
  def props(targetEmployeeCount: Int,
            workPropensity: Double,
            stealingPropensity: Double,
            timerFreq: FiniteDuration,
            productionSupervisor: ActorRef): Props =
    Props(new EmployeePool(
      targetEmployeeCount, workPropensity, stealingPropensity, timerFreq, productionSupervisor))
}

class EmployeePool(
  targetEmployeeCount: Int,
  workPropensity: Double,
  stealingPropensity: Double,
  timerFreq: FiniteDuration,
  productionSupervisor: ActorRef)
    extends Actor {

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
      println(s"$newEmployee hired (#employees = ${employeeCount})")
    }
  }

  def hireEmployee(): ActorRef = {
    val id = employeeCount
    val behaviours = ProbabilityMap.complete[RandomBehaviour](
      workPropensity -> Work,
      stealingPropensity -> Steal)

    //FIXME, s"employee$id"
    val employee = context.actorOf(Employee.props(behaviours, timerFreq, productionSupervisor))
    context.watch(employee)
    employeeCount += 1
    employee
  }
}
