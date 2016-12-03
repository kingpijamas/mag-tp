package org.mag.tp.domain

import akka.actor.{Actor, ActorRef, actorRef2Scala}
import com.softwaremill.tagging.@@
import org.mag.tp.domain.Employee.{Group, TimerFreq}
import org.mag.tp.domain.WorkArea.{Loiter, Work}
import org.mag.tp.domain.behaviour.RandomBehaviours
import org.mag.tp.util.{PausableActor, ProbabilityBag, Scheduled}

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

object Employee {
  sealed trait TypeAnnotation
  trait TimerFreq extends TypeAnnotation

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

  case class Group(id: Int,
                   targetSize: Int,
                   permeability: Double,
                   maxMemories: Option[Int],
                   baseBehaviours: ProbabilityBag[Behaviour])

  case class ActionMemory(action: WorkArea.Action, author: ActorRef)

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

    def globalBehaviours: GlobalBehaviourObservations = {
      val workingCount = totalsByActionClass(classOf[Work]).toDouble
      new GlobalBehaviourObservations(workingProportion = workingCount / actionMemories.size)
    }

    override def toString: String =
      s"StatusPerception(actions=$actionMemories," +
        s" totalWork=${totalsByActionClass(classOf[Work])}," +
        s" totalLoitering=${totalsByActionClass(classOf[Loiter])}"
  }

  class GlobalBehaviourObservations(val workingProportion: Double) {
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

class Employee(val group: Group,
               val timerFreq: FiniteDuration @@ TimerFreq,
               val workArea: ActorRef @@ WorkArea)
  extends Actor with RandomBehaviours with Scheduled with PausableActor {

  import Employee._
  import WorkArea._

  val memory = new Memory(group.maxMemories)
  var _behaviours: ProbabilityBag[Behaviour] = group.baseBehaviours

  def timerMessage: Any = Act

  def randomBehaviourTrigger: Any = Act

  def behaviours: Behaviours = {
    def work() = { workArea ! Work(group) }
    def loiter() = { workArea ! Loiter(group) }

    if (!memory.knownEmployees.isEmpty) { // XXX
      updateBehaviours()
    }

    _behaviours map {
      case WorkBehaviour => work _
      case LoiterBehaviour => loiter _
    }
  }

  private[this] def updateBehaviours() = {
    val permeability = group.permeability

    val globalBehaviours = memory.globalBehaviours
    val preferredBehaviour = if (permeability > 0)
      globalBehaviours.majorityBehaviour
    else
      globalBehaviours.minorityBehaviour

    val ownProb = _behaviours(preferredBehaviour)
    val globalMainProb = globalBehaviours.majorityProportion
    val newPreferredBehaviourProb = permeability.abs * globalMainProb + (1 - permeability.abs) * ownProb

    _behaviours = ProbabilityBag.complete(
      preferredBehaviour -> newPreferredBehaviourProb,
      preferredBehaviour.opposite -> (1 - newPreferredBehaviourProb)
    )
  }

  def receive: Receive = respectPauses orElse actRandomly orElse {
    case action: Action => memory.remember(action, sender)
  }
}
