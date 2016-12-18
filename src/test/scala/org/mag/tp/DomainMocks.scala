package org.mag.tp

import org.mag.tp.domain.employee.{Behaviour, Group, LoiterBehaviour, WorkBehaviour}
import org.mag.tp.util.ProbabilityBag
import org.scalatest.Suite

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

trait DomainMocks {
  this: Suite =>

  def dummyTime: Option[FiniteDuration] = None

  def testGroup(id: String): Group = Group(
    id = id,
    targetSize = 100,
    permeability = 0.5,
    maxMemories = None,
    baseBehaviours = ProbabilityBag.complete[Behaviour](
      WorkBehaviour -> 0.5,
      LoiterBehaviour -> 0.5
    )
  )
}
