package org.mag.tp.domain.behaviour

import org.mag.tp.util.ProbabilityBag

import akka.actor.Actor

trait RandomBehaviours {
  this: Actor =>
  // self: ExternallyTimedActor =>

  type Behaviours = ProbabilityBag[() => _]

  def behaviours: Behaviours

  def randomBehaviourTrigger: Any

  def actRandomly: Receive = {
    case msg if msg == randomBehaviourTrigger =>
      val behaviour = behaviours.nextValue.get
      behaviour.apply()
  }
}
