package org.mag.tp.domain

import org.mag.tp.util.ProbabilityBag

package object employee {
  sealed trait Behaviour {
    def opposite: Behaviour
  }
  case object WorkBehaviour extends Behaviour {
    lazy val opposite: Behaviour = LoiterBehaviour
  }
  case object LoiterBehaviour extends Behaviour {
    lazy val opposite: Behaviour = WorkBehaviour
  }

  case class Group(id: String,
                   targetSize: Int,
                   permeability: Double,
                   maxMemories: Option[Int],
                   baseBehaviours: ProbabilityBag[Behaviour])
}
