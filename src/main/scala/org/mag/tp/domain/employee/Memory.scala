package org.mag.tp.domain.employee

import akka.actor.ActorRef
import com.softwaremill.macwire.wire
import org.mag.tp.domain.WorkArea
import org.mag.tp.domain.WorkArea.{ActionType, Loiter, Work}
import org.mag.tp.domain.employee.Memory.Observations

import scala.collection.mutable

object Memory {
  class Observations(val workingProportion: Double) {
    val loiteringProportion: Double = 1 - workingProportion

    val (majorityBehaviour: Behaviour, majorityProportion: Double) =
      if (workingProportion >= 0.5)
        (WorkBehaviour, workingProportion)
      else
        (LoiterBehaviour, loiteringProportion)

    val minorityBehaviour: Behaviour = majorityBehaviour.opposite
    val minorityProportion: Double = 1 - majorityProportion

    def proportion(behaviour: Behaviour): Double = behaviour match {
      case WorkBehaviour => workingProportion
      case LoiterBehaviour => loiteringProportion
    }

    def canEqual(other: Any): Boolean = other.isInstanceOf[Observations]

    override def equals(other: Any): Boolean = other match {
      case that: Observations =>
        (that canEqual this) &&
          loiteringProportion == that.loiteringProportion &&
          majorityBehaviour == that.majorityBehaviour &&
          majorityProportion == that.majorityProportion &&
          minorityBehaviour == that.minorityBehaviour &&
          minorityProportion == that.minorityProportion &&
          workingProportion == that.workingProportion
      case _ => false
    }

    override def hashCode(): Int = {
      val state = Seq(loiteringProportion, majorityBehaviour, majorityProportion, minorityBehaviour,
        minorityProportion, workingProportion)
      val hashCodes = state map (_.hashCode)
      hashCodes.foldLeft(0)((a, b) => 31 * a + b)
    }

    override def toString: String =
      "GlobalBehaviourObservations(" +
        s"majorityBehaviour=$majorityBehaviour, " +
        s"majorityProportion=$majorityProportion)"
  }

  def apply(maxSize: Option[Int] = None): Memory = wire[Memory]
}

class Memory(maxSize: Option[Int]) {
  val rememberedActions = mutable.Queue[WorkArea.Action]()
  val rememberedActionCountsByEmployee = mutable.Map[ActorRef, Int]().withDefaultValue(0)
  val rememberedActionCountsByType = mutable.Map[ActionType, Int]().withDefaultValue(0)

  def remember(action: WorkArea.Action): Unit = {
    if (isFull) {
      forget()
    }
    rememberedActions += action
    rememberedActionCountsByEmployee(action.employee) += 1
    rememberedActionCountsByType(action.getClass) += 1
  }

  private[this] def isFull: Boolean = maxSize.isDefined && maxSize.get == rememberedActions.size

  private[this] def forget(): Unit = {
    val forgottenMemory = rememberedActions.dequeue()
    val forgottenEmployee = forgottenMemory.employee

    rememberedActionCountsByEmployee(forgottenEmployee) -= 1
    if (rememberedActionCountsByEmployee(forgottenEmployee) == 0) {
      rememberedActionCountsByEmployee -= forgottenEmployee
    }

    rememberedActionCountsByType(forgottenMemory.getClass) -= 1
  }

  def knownEmployees: Traversable[ActorRef] = rememberedActionCountsByEmployee.keys

  def globalObservations: Observations = {
    val workingCount = rememberedActionCountsByType(classOf[Work]).toDouble
    new Observations(workingProportion = workingCount / rememberedActions.size)
  }

  override def toString: String =
    "StatusPerception(" +
      s"actions=$rememberedActions, " +
      s"totalWork=${rememberedActionCountsByType(classOf[Work])}, " +
      s"totalLoitering=${rememberedActionCountsByType(classOf[Loiter])})"
}
