package org.mag.tp.domain.employee

import akka.actor.{Actor, ActorRef, actorRef2Scala}
import com.softwaremill.tagging.@@
import org.mag.tp.domain.WorkArea
import org.mag.tp.domain.behaviour.RandomBehaviours
import org.mag.tp.util.{PausableActor, ProbabilityBag, Scheduled}

import scala.concurrent.duration.FiniteDuration

object Employee {
  // messages
  case object Act

  case class ActionMemory(action: WorkArea.Action, author: ActorRef)
}

class Employee(val group: Group,
               val timerFreq: FiniteDuration @@ Employee,
               val workArea: ActorRef @@ WorkArea)
  extends Actor with RandomBehaviours with Scheduled with PausableActor {

  import Employee._
  import WorkArea._

  val memory = new Memory(group.maxMemories)
  var _behaviours: ProbabilityBag[Behaviour] = group.baseBehaviours

  def timerMessage: Any = Act

  def randomBehaviourTrigger: Any = Act

  def behaviours: Behaviours = {
    def work() = {
      workArea ! Work(self, group)
    }

    def loiter() = {
      workArea ! Loiter(self, group)
    }

    if (!memory.knownEmployees.isEmpty) {
      // XXX
      updateBehaviours()
    }

    _behaviours map {
      case WorkBehaviour => work _
      case LoiterBehaviour => loiter _
    }
  }

  private[this] def updateBehaviours() = {
    val permeability = group.permeability

    val observations = memory.globalObservations
    val preferredBehaviour = if (permeability > 0) observations.majorityBehaviour else observations.minorityBehaviour

    val ownProb = _behaviours(preferredBehaviour)
    val globalMainProb = observations.majorityProportion
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
