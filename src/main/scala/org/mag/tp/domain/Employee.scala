package org.mag.tp.domain

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

import org.mag.tp.domain.behaviour.RandomBehaviours
import org.mag.tp.util.ProbabilityBag
import org.mag.tp.util.Scheduled

import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{ @@ => @@ }

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import scala.collection.immutable
import scala.util.Random

object Employee {
  // messages
  case object Act
  case class Paycheck(employee: ActorRef, amount: Double)

  sealed trait Behaviour {
    def opposite: Behaviour
  }
  case object WorkBehaviour extends Behaviour {
    lazy val opposite: Behaviour = LoiterBehaviour
  }
  case object LoiterBehaviour extends Behaviour {
    lazy val opposite: Behaviour = WorkBehaviour
  }

  // type annotations
  trait TimerFreq
  trait Inertia
  trait Cyclicity
  trait Permeability

  case class StatusPerception(totalEarnings: Double = 0D, totalWork: Int = 0, totalLoitering: Int = 0) {
    def isWorking: Boolean = totalWork > totalLoitering
    
    def isLazy: Boolean = totalWork < totalLoitering

    override def toString: String =
      s"StatusPerception(totalEarnings=$totalEarnings, totalWork=$totalWork, totalLoitering=$totalLoitering)"
  }

  def props(inertia: Int @@ Inertia,
            cyclicity: Double @@ Cyclicity,
            permeability: Double @@ Permeability,
            behaviours: ProbabilityBag[Behaviour],
            timerFreq: FiniteDuration @@ TimerFreq,
            workArea: ActorRef @@ WorkArea): Props = {
    Props(wire[Employee])
  }
}

class Employee(
  inertia: Int @@ Employee.Inertia,
  cyclicity: Double @@ Employee.Cyclicity,
  permeability: Double @@ Employee.Permeability,
  var baseBehaviours: ProbabilityBag[Employee.Behaviour],
  val timerFreq: FiniteDuration,
  val workArea: ActorRef)
    extends Actor with RandomBehaviours with Scheduled {
  import Employee._
  import WorkArea._

  println(s"$self: alive and well!")

  def timerMessage: Any = Act
  def randomBehaviourTrigger: Any = Act

  val perceptionsByEmployee = mutable.Map[ActorRef, StatusPerception]()
    .withDefaultValue(StatusPerception())

  def behaviours: Behaviours = {
    if (!knownEmployees.isEmpty) {
      // val oldBehaviours = baseBehaviours
      baseBehaviours = updateBehaviours(baseBehaviours)
      // println(s"${self}: $oldBehaviours => $baseBehaviours")
    }

    baseBehaviours map {
      case WorkBehaviour   => work _
      case LoiterBehaviour => loiter _
    }
  }

  private[this] def updateBehaviours(baseBehaviours: ProbabilityBag[Employee.Behaviour]) = {
    val workingCount: Double = perceptionsByEmployee.values.count(_.isWorking)
    val workingProportion = workingCount / knownEmployees.size.toDouble
    val (majorityBehaviour, majorityProportion) = if (workingProportion >= 0.5)
      (WorkBehaviour, workingProportion)
    else
      (LoiterBehaviour, 1 - workingProportion)

    val preferredBehaviour = if (Random.nextDouble < cyclicity)
      majorityBehaviour
    else
      majorityBehaviour.opposite
      
    // println(s"($preferredBehaviour) = ${baseBehaviours(preferredBehaviour)} + $permeability * $majorityProportion = ${baseBehaviours(preferredBehaviour) + permeability * majorityProportion}")

    val proposedPreferredBehaviourProb =
      baseBehaviours(preferredBehaviour) + permeability * majorityProportion
    val newPreferredBehaviourProb =
      Math.min(Math.max(proposedPreferredBehaviourProb, 0), 1)

    ProbabilityBag.complete(
      preferredBehaviour -> newPreferredBehaviourProb,
      preferredBehaviour.opposite -> (1 - newPreferredBehaviourProb))
  }

  def receive: Receive = actRandomly orElse {
    case Paycheck(employee, amount) =>
      updatePerception(employee) { oldPerception =>
        oldPerception.copy(totalEarnings = oldPerception.totalEarnings + amount)
      }

    case Work =>
      updatePerception(sender) { oldPerception =>
        oldPerception.copy(totalWork = oldPerception.totalWork + 1)
      }

    case Loiter =>
      updatePerception(sender) { oldPerception =>
        oldPerception.copy(totalLoitering = oldPerception.totalLoitering + 1)
      }
  }

  private[this] def updatePerception(employee: ActorRef)(f: (StatusPerception => StatusPerception)): Unit = {
    val currentPerception = perceptionsByEmployee(employee)
    perceptionsByEmployee(employee) = f(currentPerception)
    // println(s"${self.path}: ${employee.path} -> ${perceptionsByEmployee(employee)}")  }
  }

  private[this] def work() = { workArea ! Work }
  private[this] def loiter() = { workArea ! Loiter }
  private[this] def knownEmployees = perceptionsByEmployee.keys
  private[this] def ownStatus = perceptionsByEmployee(self)
}
