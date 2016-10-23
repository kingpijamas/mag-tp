package org.tpmag.domain

import java.lang.Math.min

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

import org.tpmag.domain.behaviour.ExternallyTimedActor
import org.tpmag.util.ProbabilityBag
import org.tpmag.domain.behaviour.RandomBehaviours

import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{ @@ => @@ }

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.actor.actorRef2Scala
import org.tpmag.util.ReceiveChaining
import org.tpmag.domain.behaviour.SocialActor
import org.tpmag.domain.behaviour.StealingActor
import org.tpmag.domain.behaviour.ProducerActor

object Employee {
  case object Act
  case object Fire

  sealed trait Behaviour
  case object Work extends Behaviour
  case object Socialize extends Behaviour
  case object Steal extends Behaviour

  def props(timerFreq: FiniteDuration,
            behaviours: ProbabilityBag[Behaviour],
            employees: ActorRef @@ EmployeePool,
            productionSupervisor: ActorRef @@ ProductionSupervisor,
            warehouse: ActorRef @@ Warehouse): Props =
    Props(wire[Employee])
}

class Employee(
  val timerFreq: FiniteDuration,
  behaviourOdds: ProbabilityBag[Employee.Behaviour],
  employees: ActorRef @@ EmployeePool,
  productionSupervisor: ActorRef @@ ProductionSupervisor,
  warehouse: ActorRef @@ Warehouse)
    extends Actor
    with ReceiveChaining
    with ExternallyTimedActor
    with ProducerActor
    with SocialActor
    with StealingActor
    with RandomBehaviours {
  import Employee._

  def timer = productionSupervisor
  def productionReceiver = productionSupervisor
  def socialPool = employees
  def theftVictim = warehouse

  def timerMessage = Act
  def randomBehaviourTrigger = Act

  val behaviours: ProbabilityBag[() => _] =
    behaviourOdds.map {
      case Work      => (() => produce())
      case Socialize => socialize _
      case Steal     => (() => steal(10)) // FIXME: magic number!
    }

  def receiveOrders: Receive = {
    case Fire => context.stop(self)
  }

  def timed: Receive =
    actRandomly orElse
      respondToSocialization orElse
      stealingFollowup orElse
      receiveOrders
}
