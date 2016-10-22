package org.tpmag.util

trait RandomBehaviours[B] {
  def behaviours: ProbabilityBag[B]

  def randBehaviour: B = { behaviours.getRand.get }
}
