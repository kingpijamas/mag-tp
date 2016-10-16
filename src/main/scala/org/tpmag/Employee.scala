package org.tpmag

import scala.concurrent.duration.FiniteDuration

import Employee.RandomBehaviour
import ProductionSupervisor.CurrentTime
import ProductionSupervisor.GetCurrentTime
import ProductionSupervisor.Produce
import Warehouse.Goods
import Warehouse.StealGoods
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

  def props(behaviours: ProbabilityBag[RandomBehaviour],
            timerFreq: FiniteDuration,
            productionSupervisor: ActorRef,
            warehouse: ActorRef): Props =
    Props(new Employee(behaviours, timerFreq, productionSupervisor, warehouse))
}

class Employee(
  behaviours: ProbabilityBag[RandomBehaviour],
  override val timerFreq: FiniteDuration,
  productionSupervisor: ActorRef,
  warehouse: ActorRef)
    extends Actor with Scheduled {

  import Employee._
  import context._

  var time: Option[Time] = None

  def timerMessage = Act

  def untimed: Receive = {
    case Act => productionSupervisor ! GetCurrentTime
    case CurrentTime(time) =>
      this.time = Some(time)
      become(timed)
  }

  def timed: Receive = {
    case Act =>
      behaviours.getRand.get match {
        case Work  => { println("Working"); productionSupervisor ! Produce(time.get) }
        case Steal => { println("Sneaking in..."); warehouse ! StealGoods(time.get, 10) }
      }
      time = time.map(_ + 1)

    case Goods(_) => println("Bwahaha!")
    case Fire     => context.stop(self)
  }

  def receive = untimed
}
