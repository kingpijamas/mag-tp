package org.mag.tp.domain

import akka.actor.{Actor, ActorRef, actorRef2Scala}
import com.softwaremill.tagging.@@
import org.mag.tp.domain.WorkArea._
import org.mag.tp.domain.behaviour.RandomBehaviours
import org.mag.tp.util.{ProbabilityBag, Scheduled}

import scala.collection.mutable
import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration

object Employee {
  // type annotations
  trait TimerFreq
  trait MemorySize
  trait Permeability

  // messages
  case object Act

  sealed trait Behaviour {
    def opposite: Behaviour
  }
  case object WorkBehaviour extends Behaviour {
    lazy val opposite: Behaviour = LoiterBehaviour
  }
  case object LoiterBehaviour extends Behaviour {
    lazy val opposite: Behaviour = WorkBehaviour
  }

  private[domain] class ActionMemory(maxSize: Option[Int]) {
    require(maxSize.forall(_ > 0))

    val actions = mutable.Queue[WorkArea.Action]()
    val totalsByAction = mutable.Map[WorkArea.Action, Int]().withDefaultValue(0)

    def +=(action: WorkArea.Action): this.type = {
      if (isFull) {
        val forgottenAction = actions.dequeue()
        totalsByAction(forgottenAction) -= 1
      }
      actions += action
      totalsByAction(action) += 1
      this
    }

    def isFull: Boolean = maxSize.isDefined && maxSize.get == actions.size

    def total(action: WorkArea.Action): Int = totalsByAction(action)

    def isWorking: Boolean = total(Work) > total(Loiter)

    def isLazy: Boolean = total(Work) < total(Loiter)

    override def toString: String =
      s"StatusPerception(actions=$actions, totalWork=${total(Work)}, totalLoitering=${total(Loiter)}"
  }

  private[domain] class GlobalBehaviourObservations(val workingProportion: Double) {
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

    override def toString: String =
      s"GlobalBehaviourObservations(majorityBehaviour=$majorityBehaviour, majorityProportion=$majorityProportion)"
  }
}

class Employee(val maxMemories: Option[Int] @@ Employee.MemorySize,
               val permeability: Double @@ Employee.Permeability,
               private[domain] var baseBehaviours: ProbabilityBag[Employee.Behaviour],
               val timerFreq: FiniteDuration @@ Employee.TimerFreq,
               val workArea: ActorRef @@ WorkArea)
  extends Actor with RandomBehaviours with Scheduled {

  import Employee._
  import WorkArea._

  def timerMessage: Any = Act
  def randomBehaviourTrigger: Any = Act

  private[domain] val memoryByEmployee = mutable.Map[ActorRef, ActionMemory]()
    .withDefaultValue(new ActionMemory(maxMemories))

  def behaviours: Behaviours = {
    def work() = { workArea ! Work }
    def loiter() = { workArea ! Loiter }

    if (!knownEmployees.isEmpty) {
      updateBehaviours()
    }

    baseBehaviours map {
      case WorkBehaviour => work _
      case LoiterBehaviour => loiter _
    }
  }

  private[this] def updateBehaviours() = {
    def currentGlobalBehaviours = {
      val workingCount: Double = memoryByEmployee.values count (_.isWorking)
      new GlobalBehaviourObservations(workingProportion = workingCount / knownEmployees.size)
    }

    val globalBehaviours = currentGlobalBehaviours
    val preferredBehaviour = if (permeability > 0)
      globalBehaviours.majorityBehaviour
    else
      globalBehaviours.minorityBehaviour

    val ownProb = baseBehaviours(preferredBehaviour)
    val globalMainProb = globalBehaviours.majorityProportion
    val newPreferredBehaviourProb = permeability.abs * globalMainProb + (1 - permeability.abs) * ownProb

    baseBehaviours = ProbabilityBag.complete(
      preferredBehaviour -> newPreferredBehaviourProb,
      preferredBehaviour.opposite -> (1 - newPreferredBehaviourProb)
    )
  }

  def receive: Receive = actRandomly orElse {
    case action: Action =>
      val memoryForEmployee = memoryByEmployee(sender)
      memoryByEmployee(sender) = memoryForEmployee += action
  }

  private[this] def knownEmployees = memoryByEmployee.keys
}
