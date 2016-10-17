package org.tpmag

import java.lang.Math.min

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.util.Random

import Employee.Behaviour
import ProductionSupervisor.CurrentTime
import ProductionSupervisor.GetCurrentTime
import ProductionSupervisor.Produce
import Warehouse.Goods
import Warehouse.StealGoods
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.actor.Terminated

object Employee {
  case object Act
  case object Fire
  case object Talk
  case class TalkBack(newRelation: Double)

  sealed trait Behaviour
  case object Work extends Behaviour
  case object Steal extends Behaviour
  case object Socialize extends Behaviour

  def props(
    timerFreq: FiniteDuration,
    behaviours: ProbabilityBag[Behaviour],
    employees: ActorRef,
    productionSupervisor: ActorRef,
    warehouse: ActorRef): Props =
    Props(new Employee(timerFreq, behaviours, employees, productionSupervisor, warehouse))
}

class Employee(
  override val timerFreq: FiniteDuration,
  override val behaviours: ProbabilityBag[Behaviour],
  employees: ActorRef,
  productionSupervisor: ActorRef,
  warehouse: ActorRef)
    extends Actor with Scheduled with RandomBehaviours[Behaviour] {

  import Employee._
  import context._

  var time: Option[Time] = None
  val relations = mutable.Map[ActorRef, Double](self -> 1)

  def timerMessage = Act

  def untimed: Receive = {
    case Act => productionSupervisor ! GetCurrentTime

    case CurrentTime(time) =>
      this.time = Some(time)
      become(timed)
  }

  def timed: Receive = {
    case Act =>
      randBehaviour match {
        case Work =>
          println("Working")
          productionSupervisor ! Produce(time.get)
        case Socialize =>
          println("Blah blah")
          employees ! Talk
        case Steal =>
          println("Sneaking in...")
          warehouse ! StealGoods(time.get, 10)
      }
      time = time.map(_ + 1)

    case Talk if sender != self =>
      val relation: Double = relations.getOrElse(sender, 0)
      val newRelation = min(relation + Random.nextDouble, 1)
      relations(sender) = newRelation
      context.watch(sender)
      sender ! TalkBack(newRelation)

    case TalkBack(newRelation) =>
      relations(sender) = newRelation
      context.watch(sender)
      println(s"$self $relations")

    case Terminated(employee) =>
      relations -= sender; println("Goodbye friend!")
    case Goods(_)             => println("Bwahaha!")
    case Fire                 => context.stop(self)
  }

  def receive = untimed
}
