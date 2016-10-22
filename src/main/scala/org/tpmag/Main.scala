package org.tpmag

import scala.concurrent.duration.DurationDouble
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import org.tpmag.domain.Employee.Socialize
import org.tpmag.domain.Employee.Steal
import org.tpmag.domain.Employee.Work
import org.tpmag.domain.EmployeePool
import org.tpmag.domain.ProductionSupervisor
import org.tpmag.domain.ProductionSupervisor.EmployeeCount
import org.tpmag.domain.ProductionSupervisor.MaxDeviationsAllowed
import org.tpmag.domain.ProductionSupervisor.PeriodLength
import org.tpmag.domain.Warehouse

import com.softwaremill.tagging.Tagger

import akka.actor.ActorSystem

object Main extends App {
  val system = ActorSystem("tp-mag")

  val maxDeviationsAllowed = 1D
  val periodLength = 5
  val employeeCount = 10
  val productionSupervisor = system.actorOf(
    ProductionSupervisor.props(
      initialTime = 0L,
      timerFreq = 10 seconds,
      periodLength = periodLength.taggedWith[PeriodLength],
      maxDeviationsAllowed = maxDeviationsAllowed.taggedWith[MaxDeviationsAllowed],
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
