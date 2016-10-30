package org.mag.tp.domain.behaviour

import akka.actor.Actor
import org.mag.tp.util.ProbabilityBag

trait RandomBehaviours {
  this: Actor =>
  // self: ExternallyTimedActor =>

  def behaviours: ProbabilityBag[() => _]

  def randomBehaviourTrigger: Any

  def actRandomly: Receive = {
    case msg if msg == randomBehaviourTrigger =>
      val behaviour = behaviours.getRand.get
      behaviour.apply()
  }
}
