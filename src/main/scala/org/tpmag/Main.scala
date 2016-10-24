package org.tpmag

import scala.concurrent.duration.DurationDouble
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import org.tpmag.domain.CompanyGrounds
import org.tpmag.domain.Employee.Socialize
import org.tpmag.domain.Employee.Steal
import org.tpmag.domain.Employee.Work
import org.tpmag.domain.EmployeeCount
import org.tpmag.domain.EmployeePool
import org.tpmag.domain.ProductionSupervisor
import org.tpmag.domain.ProductionSupervisor.MaxDeviationsAllowed
import org.tpmag.domain.ProductionSupervisor.PeriodLength
import org.tpmag.domain.Warehouse
import scala.collection.immutable
import com.softwaremill.tagging.Tagger

import akka.actor.ActorSystem

object Main extends App {
  val system = ActorSystem("tp-mag")

  val maxDeviationsAllowed = 1D
  val periodLength = 5
  val employeeCount = 10.taggedWith[EmployeeCount]
  val productionSupervisor = system.actorOf(
    ProductionSupervisor.props(
      initialTime = 0L,
      timerFreq = 10 seconds,
      periodLength = periodLength.taggedWith[PeriodLength],
      maxDeviationsAllowed = maxDeviationsAllowed.taggedWith[MaxDeviationsAllowed],
      employeeCount = employeeCount))
    .taggedWith[ProductionSupervisor]

  val warehouse = system.actorOf(
    Warehouse.props(
      catchingPropensity = 0.5))
    .taggedWith[Warehouse]

  val behaviours = immutable.Map(
    Work -> 0.5,
    Socialize -> 0.3,
    Steal -> 0.2)
    .toSeq

  val companyGrounds = system.actorOf(
    CompanyGrounds.props(
      employeeCount = employeeCount,
      timerFreq = 0.5 seconds,
      behaviours = behaviours,
      warehouse = warehouse,
      productionSupervisor = productionSupervisor))
}
