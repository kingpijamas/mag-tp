package org.tpmag.domain

import scala.concurrent.duration.FiniteDuration
import scala.math.ceil
import scala.math.min
import scala.util.Random

import org.tpmag.domain.behaviour.CrimeEnvironment
import org.tpmag.domain.behaviour.TheftVictimActor

import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{ @@ => @@ }
import com.softwaremill.tagging.Tagger

import Employee.Behaviour
import EmployeePool.TimerFreq
import HumanResources.AccusationReceptionTime
import HumanResources.VeredictVotesReceptionTime
import akka.actor.ActorRef
import akka.actor.Props

object CompanyGrounds {
  def props(employeeCount: Int @@ EmployeeCount,
            timerFreq: FiniteDuration @@ TimerFreq,
            accusationReceptionTime: FiniteDuration @@ AccusationReceptionTime,
            veredictVotesReceptionTime: FiniteDuration @@ VeredictVotesReceptionTime,
            behaviours: Seq[(Behaviour, Double)],
            warehouse: ActorRef @@ Warehouse,
            productionSupervisor: ActorRef @@ ProductionSupervisor): Props =
    Props(wire[CompanyGrounds])
}

class CompanyGrounds(
  val employeeCount: Int @@ EmployeeCount,
  timerFreq: FiniteDuration @@ TimerFreq,
  accusationReceptionTime: FiniteDuration @@ AccusationReceptionTime,
  veredictVotesReceptionTime: FiniteDuration @@ VeredictVotesReceptionTime,
  behaviours: Seq[(Behaviour, Double)],
  warehouse: ActorRef @@ Warehouse,
  productionSupervisor: ActorRef @@ ProductionSupervisor)
    extends CrimeEnvironment {
  import TheftVictimActor._

  val employeePool = context.actorOf(EmployeePool.props(
    employeeCount,
    behaviours,
    timerFreq,
    productionSupervisor,
    companyGrounds = self.taggedWith[CompanyGrounds]))

  val humanResources = context.actorOf(HumanResources.props(accusationReceptionTime,
    veredictVotesReceptionTime,
    juryPool = employeePool.taggedWith[EmployeePool]))

  def witnessPool: ActorRef = employeePool

  // TODO: check relation with Warehouse (supervision? death-watch?)

  def victim: ActorRef = warehouse

  def witnessCount(crime: Any): Int = crime match {
    case StealingAttempt(_, amount) =>
      val proposedWitnessCount = ceil(Random.nextDouble * amount).toInt // TODO: check!
      min(proposedWitnessCount, employeeCount)
  }
}
