package org.tpmag

import scala.collection.immutable
import scala.concurrent.duration.DurationDouble
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import org.tpmag.domain.CompanyGrounds
import org.tpmag.domain.Employee.Socialize
import org.tpmag.domain.Employee.Steal
import org.tpmag.domain.Employee.Work
import org.tpmag.domain.EmployeeCount
import org.tpmag.domain.EmployeePool.TimerFreq
import org.tpmag.domain.HumanResources.AccusationReceptionTime
import org.tpmag.domain.HumanResources.VeredictVotesReceptionTime
import org.tpmag.domain.ProductionSupervisor
import org.tpmag.domain.ProductionSupervisor.MaxDeviationsAllowed
import org.tpmag.domain.ProductionSupervisor.PeriodLength
import org.tpmag.domain.Warehouse

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

  val employeeTimerFreq = 0.5 seconds
  val accusationReceptionTime = 5 seconds
  val veredictVotesReceptionTime = 5 seconds
  val companyGrounds = system.actorOf(
    CompanyGrounds.props(
      employeeCount = employeeCount,
      timerFreq = employeeTimerFreq.taggedWith[TimerFreq],
      behaviours = behaviours,
      warehouse = warehouse,
      productionSupervisor = productionSupervisor,
      accusationReceptionTime = accusationReceptionTime.taggedWith[AccusationReceptionTime],
      veredictVotesReceptionTime = veredictVotesReceptionTime.taggedWith[VeredictVotesReceptionTime]))
}
