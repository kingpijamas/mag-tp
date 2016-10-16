package org.tpmag

import scala.concurrent.duration.FiniteDuration

import Employee.Data
import Employee.RandomBehaviour
import Employee.State
import akka.actor.ActorRef
import akka.actor.FSM
import akka.actor.Props
import akka.actor.actorRef2Scala

object Employee {
  case object Act
  case object Fire

  sealed trait State
  case object Untimed extends State
  case object Timed extends State

  sealed trait RandomBehaviour
  case object Work extends RandomBehaviour
  case object Steal extends RandomBehaviour

  sealed trait Data
  case object Uninitialized extends Data
  case class Initialized(time: Time) extends Data

  def props(behaviours: ProbabilityMap[RandomBehaviour],
            timerFreq: FiniteDuration,
            productionSupervisor: ActorRef): Props =
    Props(new Employee(behaviours, timerFreq, productionSupervisor))
}

class Employee(
  behaviours: ProbabilityMap[RandomBehaviour],
  val timerFreq: FiniteDuration,
  productionSupervisor: ActorRef)
    extends FSM[State, Data] with Scheduled {

  import Employee._
  import ProductionSupervisor._

  def timerMessage = Act

  startWith(Untimed, Uninitialized)

  when(Untimed) {
    case Event(Act, _) => {
      productionSupervisor ! GetCurrentTime
      stay
    }
    case Event(CurrentTime(time), _) => {
      goto(Timed) using Initialized(time)
    }
  }

  when(Timed) {
    case Event(Act, Initialized(time)) => {
      behaviours.getRand.get match {
        case Work => {
          println("Work")
          productionSupervisor ! Produce(time)
        }
        case Steal => { println("Bwahaha") }
      }
      stay using Initialized(time + 1)
    }
    case Event(Fire, _) => { stop() }
  }

  initialize()
}
