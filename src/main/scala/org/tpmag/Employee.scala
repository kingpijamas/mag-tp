package org.tpmag

import scala.util.Random

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.actorRef2Scala

import scala.concurrent._
import ExecutionContext.Implicits.global

import akka.actor.ActorSystem
import akka.actor.Props
import scala.concurrent.duration._
import org.tpmag.Employee._
import org.tpmag.ProductionSupervisor._

object Employee {
  case object Act
  case object Fire
}

class Employee(
    val workPropensity: Double,
    val timerFreq: FiniteDuration,
    val productionSupervisor: ActorRef) extends Actor with Scheduled {
  import Employee._
  import ProductionSupervisor._

  def timerMessage = Act

  var time: Option[Time] = None

  def receive = {
    case Act if time.isEmpty => { productionSupervisor ! GetCurrentTime }
    case Act => {
      if (Random.nextDouble <= workPropensity) {
        productionSupervisor ! Produce(time.get)
      }
      time = Some(time.get + 1)
    }
    case CurrentTime(time) => { this.time = Some(time) }
    case Fire              => { context.stop(self) }
  }
}
