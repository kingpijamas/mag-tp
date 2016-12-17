package org.mag.tp.domain.employee

import akka.actor.ActorRef
import org.mag.tp.domain.WorkArea
import org.mag.tp.domain.WorkArea.{Loiter, Work}
import org.mag.tp.domain.employee.Memory.{Observations, SingleMemory}

import scala.collection.mutable

object Memory {
  case class SingleMemory(action: WorkArea.Action, author: ActorRef)

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

    override def toString: String =
      "GlobalBehaviourObservations(" +
        s"majorityBehaviour=$majorityBehaviour, " +
        s"majorityProportion=$majorityProportion)"
  }
}

class Memory(maxSize: Option[Int]) {
  val memories = mutable.Queue[SingleMemory]()
  val memoryCountsByAuthor = mutable.Map[ActorRef, Int]().withDefaultValue(0)
  val totalsByActionClass = mutable.Map[Class[_ <: WorkArea.Action], Int]().withDefaultValue(0)

  def remember(action: WorkArea.Action, author: ActorRef): Unit = {
    if (isFull) {
      forget()
    }
    memories += SingleMemory(action, author)
    memoryCountsByAuthor(author) += 1
    totalsByActionClass(action.getClass) += 1
  }

  private[this] def isFull: Boolean = maxSize.isDefined && maxSize.get == memories.size

  private[this] def forget(): Unit = {
    val SingleMemory(forgottenAction, forgottenAuthor) = memories.dequeue()

    memoryCountsByAuthor(forgottenAuthor) -= 1
    if (memoryCountsByAuthor(forgottenAuthor) == 0) {
      memoryCountsByAuthor -= forgottenAuthor
    }

    totalsByActionClass(forgottenAction.getClass) -= 1
  }

  def knownEmployees: Traversable[ActorRef] = memoryCountsByAuthor.keys

  def rememberedActions: Traversable[WorkArea.Action] = memories map (_.action)

  def globalObservations: Observations = {
    val workingCount = totalsByActionClass(classOf[Work]).toDouble
    new Observations(workingProportion = workingCount / memories.size)
  }

  override def toString: String =
    "StatusPerception(" +
      s"actions=$memories, " +
      s"totalWork=${totalsByActionClass(classOf[Work])}, " +
      s"totalLoitering=${totalsByActionClass(classOf[Loiter])})"
}
