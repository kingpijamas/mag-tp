package org.tpmag.domain

import scala.concurrent.duration.FiniteDuration

import org.tpmag.util.ProbabilityBag

import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{ @@ => @@ }
import com.softwaremill.tagging.Tagger

import Employee.Behaviour
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.routing.ActorRefRoutee
import akka.routing.RoundRobinRoutingLogic
import akka.routing.Router

object EmployeePool {
  def props(targetEmployeeCount: Int,
            propensities: Seq[(Double, Behaviour)],
            timerFreq: FiniteDuration,
            productionSupervisor: ActorRef @@ ProductionSupervisor,
            warehouse: ActorRef @@ Warehouse): Props = {
    val behaviours = ProbabilityBag.complete(propensities: _*) // TODO: make this per-employee
    Props(wire[EmployeePool])
  }
}

class EmployeePool(
  targetEmployeeCount: Int,
  behaviours: ProbabilityBag[Behaviour],
  timerFreq: FiniteDuration,
  productionSupervisor: ActorRef @@ ProductionSupervisor,
  warehouse: ActorRef @@ Warehouse)
    extends Actor {

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
      Employee.props(
        timerFreq,
        behaviours,
        self.taggedWith[EmployeePool],
        productionSupervisor,
        warehouse),
      s"employee$nextId")

    context.watch(employee)
    employeeCount += 1
    nextId += 1
    employee
  }
}
