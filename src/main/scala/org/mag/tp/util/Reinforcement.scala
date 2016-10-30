package org.mag.tp.util

import scala.util.Random

object Reinforcement {
  case class ActionAndReward[A, R <: Numeric[R]](action: A, reward: R)
}

trait Reinforcement[A, R <: Numeric[R]] {
  import Reinforcement._

  // private[this] var actionsAndRewards = mutable.Seq[ActionAndReward[A, R]]()
  def actionsAndRewards: Seq[ActionAndReward[A, R]]

  def explorationProb: Double

  def nextAction(implicit ev: Numeric[R]): A =
    if (Random.nextDouble > explorationProb)
      actionsAndRewards(Random.nextInt % actionsAndRewards.size).action
    else
      actionsAndRewards.maxBy(_.reward).action
}
