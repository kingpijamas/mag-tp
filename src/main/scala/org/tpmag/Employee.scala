package org.tpmag

import scala.concurrent.duration.FiniteDuration

import Employee.RandomBehaviour
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala

object Employee {
  case object Act
  case object Fire

  sealed trait RandomBehaviour
  case object Work extends RandomBehaviour
  case object Steal extends RandomBehaviour

  def props(behaviours: ProbabilityMap[RandomBehaviour],
            timerFreq: FiniteDuration,
            productionSupervisor: ActorRef): Props =
    Props(new Employee(behaviours, timerFreq, productionSupervisor))
}

class Employee(
  behaviours: ProbabilityMap[RandomBehaviour],
  override val timerFreq: FiniteDuration,
  productionSupervisor: ActorRef)
    extends Actor with Scheduled {

  import Employee._
  import context._
  import ProductionSupervisor._

  var time: Option[Time] = None

  def timerMessage = Act

  def untimed: Receive = {
    case Act => productionSupervisor ! GetCurrentTime
    case CurrentTime(time) =>
      this.time = Some(time)
      become(timed)
  }

  def timed: Receive = {
    case Fire => context.stop(self)
    case Act =>
      behaviours.getRand.get match {
        case Work  => { println("Work"); productionSupervisor ! Produce(time.get) }
        case Steal => { println("Bwahaha") }
      }
      time = time.map(_ + 1)
  }

  def receive = untimed
}
