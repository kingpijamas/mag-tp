package org.tpmag
import scala.concurrent._
import ExecutionContext.Implicits.global

import akka.actor.ActorSystem
import akka.actor.Props
import scala.concurrent.duration._
import org.tpmag.Employee._
import org.tpmag.ProductionSupervisor._
import scala.language.postfixOps
import com.softwaremill.macwire._
import com.softwaremill.tagging._
import ProductionSupervisor._

object Main extends App {
  val system = ActorSystem("tp-mag")

  val periodLength = 5
  val employeeCount = 10
  val productionSupervisor = system.actorOf(
    ProductionSupervisor.props(
      initialTime = 0L,
      maxDeviationsAllowed = 1D,
      timerFreq = 10 seconds,
      periodLength = periodLength.taggedWith[PeriodLength],
      employeeCount = employeeCount.taggedWith[EmployeeCount]))
    .taggedWith[ProductionSupervisor]

  val warehouse = system.actorOf(
    Warehouse.props(
      catchingPropensity = 0.5))
    .taggedWith[Warehouse]

  val behaviours = Map(
    Work -> 0.5,
    Socialize -> 0.3,
    Steal -> 0.2).map { case (bh, prob) => (prob, bh) }.toSeq

  val employeePool = system.actorOf(
    EmployeePool.props(
      employeeCount,
      behaviours,
      timerFreq = 0.5 seconds,
      productionSupervisor,
      warehouse))
}
