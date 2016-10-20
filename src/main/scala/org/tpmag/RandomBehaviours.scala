package org.tpmag

trait RandomBehaviours[B] {
  def behaviours: ProbabilityBag[B]

  def randBehaviour: B = { behaviours.getRand.get }
}
