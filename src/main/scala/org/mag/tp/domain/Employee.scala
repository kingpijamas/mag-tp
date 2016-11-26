package org.mag.tp.domain

import akka.actor.{Actor, ActorRef, actorRef2Scala}
import com.softwaremill.tagging.@@
import org.mag.tp.domain.behaviour.RandomBehaviours
import org.mag.tp.util.{ProbabilityBag, Scheduled}

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

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
  trait Permeability

  case class StatusPerception(totalEarnings: Double = 0D, totalWork: Int = 0, totalLoitering: Int = 0) {
    def isWorking: Boolean = totalWork > totalLoitering

    def isLazy: Boolean = totalWork < totalLoitering

    override def toString: String =
      s"StatusPerception(totalEarnings=$totalEarnings, totalWork=$totalWork, totalLoitering=$totalLoitering)"
  }

  private class BehaviourProportions(val workingProportion: Double) {
    val loiteringProportion: Double = 1 - workingProportion

    val (majorityBehaviour: Behaviour, majorityProportion: Double) = if (workingProportion >= 0.5)
      (WorkBehaviour, workingProportion)
    else
      (LoiterBehaviour, loiteringProportion)

    val minorityBehaviour: Behaviour = majorityBehaviour.opposite
    val minorityProportion: Double = 1 - majorityProportion

    def proportion(behaviour: Behaviour): Double = behaviour match {
      case WorkBehaviour => workingProportion
      case LoiterBehaviour => loiteringProportion
    }
  }
}

class Employee(val inertia: Int @@ Employee.Inertia,
               val permeability: Double @@ Employee.Permeability,
               var baseBehaviours: ProbabilityBag[Employee.Behaviour],
               val timerFreq: FiniteDuration @@ Employee.TimerFreq,
               val workArea: ActorRef @@ WorkArea)
  extends Actor with RandomBehaviours with Scheduled {

  import Employee._
  import WorkArea._

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
      baseBehaviours = updateBehaviourProportions(baseBehaviours)
      // println(s"${self}: $oldBehaviours => $baseBehaviours")
    }

    baseBehaviours map {
      case WorkBehaviour => work _
      case LoiterBehaviour => loiter _
    }
  }

  private[this] def behaviourProportions = {
    val workingCount = perceptionsByEmployee.values.count(_.isWorking)
    new BehaviourProportions(workingProportion = workingCount.toDouble / knownEmployees.size.toDouble)
  }

  private[this] def updateBehaviourProportions(baseProbs: ProbabilityBag[Employee.Behaviour]) = {
    val currBehaviourProportions = behaviourProportions
    val preferredBehaviour = if (permeability > 0)
      currBehaviourProportions.majorityBehaviour
    else
      currBehaviourProportions.minorityBehaviour

    val ownProb = baseProbs(preferredBehaviour)
    val globalProb = currBehaviourProportions.proportion(preferredBehaviour)
    val newPreferredBehaviourProb = permeability.abs * globalProb + (1 - permeability.abs) * ownProb
    // println(s"[$preferredBehaviour] ownProb = $ownProb, globalProb = $globalProb, permeability = $permeability => newProb = $newPreferredBehaviourProb")

    ProbabilityBag.complete(
      preferredBehaviour -> newPreferredBehaviourProb,
      preferredBehaviour.opposite -> (1 - newPreferredBehaviourProb)
    )
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
