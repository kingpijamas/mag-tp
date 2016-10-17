package org.tpmag

import scala.concurrent.duration.FiniteDuration

import Employee.Behaviour
import Employee.Socialize
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
            socializationPropensity: Double,
            timerFreq: FiniteDuration,
            productionSupervisor: ActorRef,
            warehouse: ActorRef): Props =
    Props(new EmployeePool(
      targetEmployeeCount,
      workPropensity,
      stealingPropensity,
      socializationPropensity,
      timerFreq,
      productionSupervisor,
      warehouse))
}

class EmployeePool(
  targetEmployeeCount: Int,
  workPropensity: Double,
  stealingPropensity: Double,
  socializationPropensity: Double,
  timerFreq: FiniteDuration,
  productionSupervisor: ActorRef,
  warehouse: ActorRef)
    extends Actor {

  // NOTE: temporary, make this per-employee
  val Behaviours = ProbabilityBag.complete[Behaviour](
    workPropensity -> Work,
    stealingPropensity -> Steal,
    socializationPropensity -> Socialize)

  var nextId = 0
  var employeeCount = 0
  var router = {
    val employees = Vector.fill(targetEmployeeCount) { ActorRefRoutee(hireEmployee()) }
    Router(RoundRobinRoutingLogic(), employees)
  }

  def receive = {
    case Terminated(employee) =>
      // println(s"$employee fired (#employees = ${employees.size})")
      router = router.removeRoutee(employee)
      employeeCount -= 1

      val newEmployee = hireEmployee()
      router = router.addRoutee(newEmployee)
    // println(s"$newEmployee hired (#employees = ${employeeCount})")

    case msg => router.route(msg, sender)
  }

  def hireEmployee(): ActorRef = {
    val employee = context.actorOf(
      Employee.props(timerFreq, Behaviours, self, productionSupervisor, warehouse),
      s"employee$nextId")

    context.watch(employee)
    employeeCount += 1
    nextId += 1
    employee
  }
}
