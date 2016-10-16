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
  val initialTime = 0L
  val periodLength = 5
  val maxDeviationsAllowed = 1D
  val employeeCount = 1
  val productionSupervisor = system.actorOf(
    ProductionSupervisor.props(initialTime, periodLength, maxDeviationsAllowed, employeeCount, 10 seconds))

  val workPropensity = 0.7
  val stealingPropensity = 0.3
  val employeePool = system.actorOf(
    EmployeePool.props(employeeCount, workPropensity, stealingPropensity, 0.5 seconds, productionSupervisor))
}
