package org.tpmag
import scala.concurrent._
import ExecutionContext.Implicits.global

import akka.actor.ActorSystem
import akka.actor.Props
import scala.concurrent.duration._
import org.tpmag.Employee._
import org.tpmag.ProductionSupervisor._

object Main extends App {
  val system = ActorSystem("tp-mag")
  val initialTime = 0L
  val periodLength = 5
  val maxDeviationsAllowed = 1D
  val employeeCount = 100
  val productionSupervisor = system.actorOf(Props(classOf[ProductionSupervisor],
    initialTime, periodLength, maxDeviationsAllowed, employeeCount, 10 seconds))

  val workPropensity = 0.7
  val employeePool = system.actorOf(Props(classOf[EmployeePool],
    employeeCount, workPropensity, 0.5 seconds, productionSupervisor))
}
