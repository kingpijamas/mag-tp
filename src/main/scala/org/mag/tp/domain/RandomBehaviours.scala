package org.mag.tp.domain

import akka.actor.Actor
import org.mag.tp.util.ProbabilityBag

trait RandomBehaviours {
  this: Actor =>

  type Behaviours = ProbabilityBag[() => _]

  def behaviours: Behaviours

  def randomBehaviourTrigger: Any

  def actRandomly: Receive = {
    case msg: Any if msg == randomBehaviourTrigger =>
      val behaviour = behaviours.nextValue.get
      behaviour.apply()
  }
}
