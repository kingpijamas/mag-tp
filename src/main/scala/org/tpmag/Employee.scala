package org.tpmag

import scala.concurrent.duration.FiniteDuration

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.actor.Props

object Employee {
  case object Act
  case object Fire

  def props(workPropensity: Double,
            stealingPropensity: Double,
            timerFreq: FiniteDuration,
            productionSupervisor: ActorRef): Props =
    Props(new Employee(workPropensity, stealingPropensity, timerFreq, productionSupervisor))
}

class Employee(
    val workPropensity: Double,
    val stealingPropensity: Double,
    val timerFreq: FiniteDuration,
    val productionSupervisor: ActorRef) extends Actor with Scheduled {
  import Employee._
  import ProductionSupervisor._

  def timerMessage = Act

  var time: Option[Time] = None

  def receive = {
    case Act if time.isEmpty => { productionSupervisor ! GetCurrentTime }
    case Act => {
      val actions = ProbabilityMap.complete(
        workPropensity -> { () => productionSupervisor ! Produce(time.get) },
        stealingPropensity -> { () => })
      actions.getRand.get.apply()
      time = Some(time.get + 1)
    }
    case CurrentTime(time) => { this.time = Some(time) }
    case Fire              => { context.stop(self) }
  }
}
