package org.tpmag
import scala.concurrent._
import ExecutionContext.Implicits.global

import akka.actor.ActorSystem
import akka.actor.Props
import scala.concurrent.duration._
import org.tpmag.Employee._
import org.tpmag.ProductionSupervisor._
import scala.language.postfixOps

object Main extends App {
  val system = ActorSystem("tp-mag")

  val employeeCount = 10
  val productionSupervisor = system.actorOf(
    ProductionSupervisor.props(
      initialTime = 0L,
      periodLength = 5,
      maxDeviationsAllowed = 1D,
      employeeCount,
      timerFreq = 10 seconds))

  val warehouse = system.actorOf(
    Warehouse.props(
      catchingPropensity = 0.5))

  val employeePool = system.actorOf(
    EmployeePool.props(
      employeeCount,
      workPropensity = 0.5,
      stealingPropensity = 0.2,
      socializationPropensity = 0.3,
      timerFreq = 0.5 seconds,
      productionSupervisor,
      warehouse))
}
