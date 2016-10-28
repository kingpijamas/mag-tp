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
import org.tpmag.domain.behaviour.WitnessingActor
import org.tpmag.domain.behaviour.TheftVictimActor
import org.tpmag.domain.behaviour.JuryActor
import org.tpmag.domain.behaviour.AccusingActor

object Employee {
  case object Act
  case object Fire

  sealed trait Behaviour
  case object Work extends Behaviour
  case object Socialize extends Behaviour
  case object Steal extends Behaviour

  def props(timerFreq: FiniteDuration,
            behaviours: ProbabilityBag[Behaviour],
            guiltyProbability: Double,
            theftVictim: ActorRef @@ CompanyGrounds,
            employees: ActorRef @@ EmployeePool,
            productionSupervisor: ActorRef @@ ProductionSupervisor): Props =
    Props(wire[Employee])
}

class Employee(
  val timerFreq: FiniteDuration,
  val theftVictim: ActorRef @@ CompanyGrounds,
  guiltyProbability: Double,
  behaviourOdds: ProbabilityBag[Employee.Behaviour],
  employees: ActorRef @@ EmployeePool,
  productionSupervisor: ActorRef @@ ProductionSupervisor)
    extends Actor
    with ReceiveChaining
    with ExternallyTimedActor
    with ProducerActor
    with SocialActor
    with StealingActor
    with RandomBehaviours
    with WitnessingActor
    with AccusingActor
    with JuryActor {
  import Employee._
  import TheftVictimActor._
  import JuryActor._

  def timer = productionSupervisor
  def productionReceiver = productionSupervisor
  def socialPool = employees

  def timerMessage = Act
  def randomBehaviourTrigger = Act

  val behaviours: ProbabilityBag[() => _] =
    behaviourOdds.map {
      case Work      => produce _
      case Socialize => socialize _
      case Steal     => (() => steal(10)) // FIXME: magic number!
    }

  def onTheft(crime: StealingAttempt): Unit = {
    val knownNonFriends = relations.filter { case (other, _) => !isFriend(other) }

    if (!knownNonFriends.isEmpty) {
      val (other, _) = relations.minBy { case (_, relation) => relation }
      accuse(other, crime)
    }
    // TODO: 'else' flow

    //  def proposeCulprit: Option[ActorRef] = {
    //    val knownNonFriends = relations.filter { case (other, _) => !isFriend(other) }
    //
    //    if (!knownNonFriends.isEmpty) {
    //      val (other, _) = relations.minBy { case (_, relation) => relation }
    //      Some(other)
    //    } else {
    //      None
    //    }
    //  }
  }

  def giveVeredict(accused: ActorRef, crime: StealingAttempt): Veredict =
    // FIXME
    if (Random.nextDouble < guiltyProbability)
      Guilty
    else
      NotGuilty

  def receiveOrders: Receive = {
    case Fire => context.stop(self)
  }

  def timed: Receive = chain(
    actRandomly,
    respondToSocialization,
    stealingFollowup,
    receiveOrders,
    witness)
}
