package org.tpmag.domain

import Employee.Behaviour
import scala.math._
import scala.collection.mutable
import scala.util.Random

import com.softwaremill.macwire.wire
import com.softwaremill.tagging._

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import org.tpmag.domain.behaviour.TheftVictimActor

import akka.actor.Actor
import org.tpmag.domain.behaviour.CrimeEnvironment
import akka.actor.ActorRef
import org.tpmag.util.ProbabilityBag
import scala.concurrent.duration.FiniteDuration

object CompanyGrounds {
  def props(employeeCount: Int @@ EmployeeCount,
            timerFreq: FiniteDuration,
            behaviours: Seq[(Behaviour, Double)],
            warehouse: ActorRef @@ Warehouse,
            productionSupervisor: ActorRef @@ ProductionSupervisor): Props =
    Props(wire[CompanyGrounds])
}

class CompanyGrounds(
  val employeeCount: Int @@ EmployeeCount,
  timerFreq: FiniteDuration,
  behaviours: Seq[(Behaviour, Double)],
  warehouse: ActorRef @@ Warehouse,
  productionSupervisor: ActorRef @@ ProductionSupervisor)
    extends CrimeEnvironment {
  import TheftVictimActor._

  //  val employeePool = system.actorOf(
  //    EmployeePool.props(
  //      employeeCount,
  //      behaviours,
  //      timerFreq = 0.5 seconds,
  //      productionSupervisor))
  //    .taggedWith[EmployeePool]

  val employeePool = context.actorOf(EmployeePool.props(
    employeeCount,
    behaviours,
    timerFreq,
    productionSupervisor,
    companyGrounds = self.taggedWith[CompanyGrounds]))

  def witnessPool: ActorRef = employeePool

  // TODO: check relation with Warehouse (supervision? death-watch?)

  def victim: ActorRef = warehouse

  def witnessCount(crime: Any): Int = crime match {
    case StealingAttempt(_, amount) =>
      val proposedWitnessCount = ceil(Random.nextDouble * amount).toInt // TODO: check!
      min(proposedWitnessCount, employeeCount)
  }
}
