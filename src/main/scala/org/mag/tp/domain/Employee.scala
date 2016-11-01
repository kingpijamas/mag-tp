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

object Employee {
  // messages
  case object Act
  case class Paycheck(employee: ActorRef, amount: Double)

  trait TimerFreq

  sealed trait Behaviour
  case object WorkBehaviour extends Behaviour
  case object LoiterBehaviour extends Behaviour

  case class StatusPerception(totalEarnings: Double = 0D, totalWork: Int = 0, totalLoitering: Int = 0) {
    override def toString: String =
      s"StatusPerception(totalEarnings=$totalEarnings, totalWork=$totalWork, totalLoitering=$totalLoitering)"
  }

  def props(behaviours: ProbabilityBag[Behaviour],
            envy: Double,
            timerFreq: FiniteDuration @@ TimerFreq,
            workArea: ActorRef @@ WorkArea): Props = {
    // val behavioursBag = ProbabilityBag.complete(behaviours: _*) // TODO: make this per-employee
    Props(wire[Employee])
  }
}

class Employee(
  val envy: Double,
  val timerFreq: FiniteDuration,
  baseBehaviours: ProbabilityBag[Employee.Behaviour],
  val workArea: ActorRef)
    extends Actor with RandomBehaviours with Scheduled {
  import Employee._
  import WorkArea._

  def timerMessage: Any = Act
  def randomBehaviourTrigger: Any = Act

  val perceptionsByEmployee = mutable.Map[ActorRef, StatusPerception]()
    .withDefaultValue(StatusPerception())

  val behaviours: ProbabilityBag[() => _] = baseBehaviours map {
    case WorkBehaviour   => work _
    case LoiterBehaviour => loiter _
  }

  def receive = actRandomly orElse respondToStimuli

  private[this] def respondToStimuli: Receive = {
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
    // println(s"${self.path}: ${employee.path} -> ${perceptionsByEmployee(employee)}")
  }

  private[this] def work(): Unit = {
    // println(s"$self: Working")
    workArea ! Work
  }

  private[this] def loiter(): Unit = {
    // println(s"$self: Loitering")
    workArea ! Loiter
  }

  private[this] def ownStatus = perceptionsByEmployee(self)
}
