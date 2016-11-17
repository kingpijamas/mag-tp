package org.mag.tp.domain

import akka.actor.{Actor, ActorRef, Props, actorRef2Scala}
import com.softwaremill.macwire.wire
import com.softwaremill.tagging.@@
import org.mag.tp.domain.behaviour.RandomBehaviours
import org.mag.tp.util.{ProbabilityBag, Scheduled}

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
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
}

class Employee(val inertia: Int @@ Employee.Inertia,
               val cyclicity: Double @@ Employee.Cyclicity,
               val permeability: Double @@ Employee.Permeability,
               var baseBehaviours: ProbabilityBag[Employee.Behaviour],
               val timerFreq: FiniteDuration @@ Employee.TimerFreq,
               val workArea: ActorRef @@ WorkArea)
  extends Actor with RandomBehaviours with Scheduled {

  import Employee._
  import WorkArea._

  println(s"$self: alive and well!")

  def timerMessage: Any = Act

  def randomBehaviourTrigger: Any = Act

  val perceptionsByEmployee = mutable.Map[ActorRef, StatusPerception]()
    .withDefaultValue(StatusPerception())

  def behaviours: Behaviours = {
    def work() = {
      workArea ! Work
    }
    def loiter() = {
      workArea ! Loiter
    }

    if (!knownEmployees.isEmpty) {
      // val oldBehaviours = baseBehaviours
      baseBehaviours = updateBehaviours(baseBehaviours)
      // println(s"${self}: $oldBehaviours => $baseBehaviours")
    }

    baseBehaviours map {
      case WorkBehaviour => work _
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
      updatePerception(employee) { oldP =>
        val newEarnings = oldP.totalEarnings
        oldP.copy(totalEarnings = newEarnings + amount)
      }

    case Work =>
      updatePerception(sender) { oldP =>
        val newWork = oldP.totalWork + 1
        oldP.copy(totalWork = newWork)
      }

    case Loiter =>
      updatePerception(sender) { oldP =>
        val newLoitering = oldP.totalLoitering + 1
        oldP.copy(totalLoitering = newLoitering)
      }
  }

  private[this] def updatePerception(employee: ActorRef)(f: (StatusPerception => StatusPerception)): Unit = {
    val currentPerception = perceptionsByEmployee(employee)
    perceptionsByEmployee(employee) = f(currentPerception)
    // println(s"${self.path}: ${employee.path} -> ${perceptionsByEmployee(employee)}")  }
  }

  private[this] def knownEmployees = perceptionsByEmployee.keys

  private[this] def ownStatus = perceptionsByEmployee(self)
}
