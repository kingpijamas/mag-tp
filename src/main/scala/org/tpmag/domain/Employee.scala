package org.tpmag.domain

import java.lang.Math.min

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

import org.tpmag.domain.behaviour.ExternallyTimedActor
import org.tpmag.util.ProbabilityBag
import org.tpmag.util.RandomBehaviours

import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{ @@ => @@ }

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.actor.actorRef2Scala
import org.tpmag.util.ReceiveChaining
import org.tpmag.domain.behaviour.Socialization
import org.tpmag.domain.behaviour.StealingActor

object Employee {
  case object Act
  case object Fire

  sealed trait Behaviour
  case object Work extends Behaviour
  case object Steal extends Behaviour
  case object Socialize extends Behaviour

  def props(timerFreq: FiniteDuration,
            behaviours: ProbabilityBag[Behaviour],
            employees: ActorRef @@ EmployeePool,
            productionSupervisor: ActorRef @@ ProductionSupervisor,
            warehouse: ActorRef @@ Warehouse): Props =
    Props(wire[Employee])
}

class Employee(
  override val timerFreq: FiniteDuration,
  override val behaviours: ProbabilityBag[Employee.Behaviour],
  employees: ActorRef @@ EmployeePool,
  productionSupervisor: ActorRef @@ ProductionSupervisor,
  warehouse: ActorRef @@ Warehouse)
    extends Actor
    with ReceiveChaining
    with ExternallyTimedActor
    with RandomBehaviours[Employee.Behaviour]
    with Socialization
    with StealingActor {
  import Employee._
  import ProductionSupervisor._

  def timerMessage = Act
  def timer = productionSupervisor
  def socialPool = employees
  def theftVictim = warehouse

  // TODO: randomly(steal, prob=0.1)

  def timed: Receive =
    actRandomly orElse
      respondToSocialization orElse
      stealingFollowup

  def actRandomly: Receive = {
    case Act =>
      randBehaviour match {
        case Work =>
          println("Working")
          productionSupervisor ! Produce(time.get)

        case Socialize => socialize()
        case Steal     => steal(10) // FIXME: magic number
      }
      time = time.map(_ + 1)
  }

  def receiveOrders: Receive = {
    case Fire => context.stop(self)
  }
}
