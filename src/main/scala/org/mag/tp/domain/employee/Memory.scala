package org.mag.tp.domain.employee

import akka.actor.ActorRef
import org.mag.tp.domain.WorkArea
import org.mag.tp.domain.WorkArea.{Loiter, Work}
import org.mag.tp.domain.employee.Employee.ActionMemory

import scala.collection.mutable

class Memory(maxSize: Option[Int]) {
  val actionMemories = mutable.Queue[ActionMemory]()
  val memoryCountsByAuthor = mutable.Map[ActorRef, Int]().withDefaultValue(0)
  val totalsByActionClass = mutable.Map[Class[_ <: WorkArea.Action], Int]().withDefaultValue(0)

  def remember(action: WorkArea.Action, author: ActorRef): Unit = {
    if (isFull) {
      forget()
    }
    actionMemories += ActionMemory(action, author)
    memoryCountsByAuthor(author) += 1
    totalsByActionClass(action.getClass) += 1
  }

  private[this] def isFull: Boolean = maxSize.isDefined && maxSize.get == actionMemories.size

  private[this] def forget(): Unit = {
    val ActionMemory(forgottenAction, forgottenAuthor) = actionMemories.dequeue()

    memoryCountsByAuthor(forgottenAuthor) -= 1
    if (memoryCountsByAuthor(forgottenAuthor) == 0) {
      memoryCountsByAuthor -= forgottenAuthor
    }

    totalsByActionClass(forgottenAction.getClass) -= 1
  }

  def knownEmployees: Traversable[ActorRef] = memoryCountsByAuthor.keys

  def rememberedActions: Traversable[WorkArea.Action] = actionMemories map (_.action)

  def globalObservations: GlobalObservations = {
    val workingCount = totalsByActionClass(classOf[Work]).toDouble
    new GlobalObservations(workingProportion = workingCount / actionMemories.size)
  }

  override def toString: String =
    "StatusPerception(" +
      s"actions=$actionMemories, " +
      s"totalWork=${totalsByActionClass(classOf[Work])}, " +
      s"totalLoitering=${totalsByActionClass(classOf[Loiter])})"
}
