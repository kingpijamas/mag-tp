package org.tpmag.domain.behaviour

import org.tpmag.util.ProbabilityBag

trait RandomBehaviours {
  self: ExternallyTimedActor =>

  def behaviours: ProbabilityBag[() => _]

  def randomBehaviourTrigger: Any

  def actRandomly: Receive = {
    case msg if msg == randomBehaviourTrigger =>
      val behaviour = behaviours.getRand.get
      behaviour.apply()
  }
}
