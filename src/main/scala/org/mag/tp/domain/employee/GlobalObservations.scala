package org.mag.tp.domain.employee

class GlobalObservations(val workingProportion: Double) {
  val loiteringProportion: Double = 1 - workingProportion

  val (majorityBehaviour: Behaviour, majorityProportion: Double) =
    if (workingProportion >= 0.5)
      (WorkBehaviour, workingProportion)
    else
      (LoiterBehaviour, loiteringProportion)

  val minorityBehaviour: Behaviour = majorityBehaviour.opposite
  val minorityProportion: Double = 1 - majorityProportion

  def proportion(behaviour: Behaviour): Double = behaviour match {
    case WorkBehaviour => workingProportion
    case LoiterBehaviour => loiteringProportion
  }

  override def toString: String =
    "GlobalBehaviourObservations(" +
      s"majorityBehaviour=$majorityBehaviour, " +
      s"majorityProportion=$majorityProportion)"
}
