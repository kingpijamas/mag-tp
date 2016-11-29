package org.mag.tp.domain

import akka.actor.{Actor, ActorRef, actorRef2Scala}
import com.softwaremill.tagging.@@
import org.mag.tp.domain.WorkArea._
import org.mag.tp.domain.behaviour.RandomBehaviours
import org.mag.tp.util.{PausableActor, ProbabilityBag, Scheduled}

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

object Employee {
  // type annotations
  sealed trait TypeAnnotation
  trait TimerFreq extends TypeAnnotation
  trait MemorySize extends TypeAnnotation
  trait Permeability extends TypeAnnotation

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

  private[domain] case class ActionMemory(action: WorkArea.Action, author: ActorRef)

  private[domain] class Memory(maxSize: Option[Int]) {
    val actionMemories = mutable.Queue[ActionMemory]()
    val memoryCountsByAuthor = mutable.Map[ActorRef, Int]().withDefaultValue(0)
    val totalsByAction = mutable.Map[WorkArea.Action, Int]().withDefaultValue(0)

    def remember(action: WorkArea.Action, author: ActorRef): Unit = {
      if (isFull) {
        forget()
      }
      actionMemories += ActionMemory(action, author)
      memoryCountsByAuthor(author) += 1
      totalsByAction(action) += 1
    }

    private[this] def isFull: Boolean = maxSize.isDefined && maxSize.get == actionMemories.size

    private[this] def forget(): Unit = {
      val ActionMemory(forgottenAction, forgottenAuthor) = actionMemories.dequeue()

      memoryCountsByAuthor(forgottenAuthor) -= 1
      if (memoryCountsByAuthor(forgottenAuthor) == 0) {
        memoryCountsByAuthor -= forgottenAuthor
      }

      totalsByAction(forgottenAction) -= 1
    }

    def knownEmployees: Traversable[ActorRef] = memoryCountsByAuthor.keys

    def rememberedActions: Traversable[WorkArea.Action] = actionMemories map (_.action)

    def globalBehaviours: GlobalBehaviourObservations = {
      val workingCount = totalsByAction(Work).toDouble
      new GlobalBehaviourObservations(workingProportion = workingCount / actionMemories.size)
    }

    override def toString: String =
      s"StatusPerception(actions=$actionMemories," +
        " totalWork=${totalsByAction(Work)},"+
        " totalLoitering=${totalsByAction(Loiter)}"
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
  extends Actor with RandomBehaviours with Scheduled with PausableActor {

  import Employee._
  import WorkArea._

  def timerMessage: Any = Act
  def randomBehaviourTrigger: Any = Act

  private[domain] val memory = new Memory(maxMemories)

  def behaviours: Behaviours = {
    def work() = { workArea ! Work }
    def loiter() = { workArea ! Loiter }

    if (!memory.knownEmployees.isEmpty) {
      updateBehaviours()
    }

    baseBehaviours map {
      case WorkBehaviour => work _
      case LoiterBehaviour => loiter _
    }
  }

  private[this] def updateBehaviours() = {
    val globalBehaviours = memory.globalBehaviours
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

  def receive: Receive = respectPauses orElse actRandomly orElse {
    case action: Action => memory.remember(action, sender)
  }
}
