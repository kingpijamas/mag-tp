package org.tpmag

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

import Employee._
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated

object EmployeePool {
  def props(employeeCount: Int,
            workPropensity: Double,
            stealingPropensity: Double,
            timerFreq: FiniteDuration,
            productionSupervisor: ActorRef): Props =
    Props(new EmployeePool(
      employeeCount, workPropensity, stealingPropensity, timerFreq, productionSupervisor))
}

class EmployeePool(
    employeeCount: Int,
    workPropensity: Double,
    stealingPropensity: Double,
    timerFreq: FiniteDuration,
    productionSupervisor: ActorRef) extends Actor {

  val employees = mutable.Set[ActorRef]()

  (0 until employeeCount).foreach { _ => hireEmployee() }

  def receive = {
    case Terminated(employee) => {
      employees -= employee
      // println(s"$employee fired (#employees = ${employees.size})")
      val newEmployee = hireEmployee()
      println(s"$newEmployee hired (#employees = ${employees.size})")
    }
  }

  def hireEmployee(): ActorRef = {
    val id = employees.size
    val behaviours = ProbabilityMap.complete[RandomBehaviour](
      workPropensity -> Work,
      stealingPropensity -> Steal
    )

    //FIXME, s"employee$id"
    val employee = context.actorOf(Employee.props(behaviours, timerFreq, productionSupervisor))
    context.watch(employee) // FIXME change to supervision
    employees += employee
    employee
  }
}
