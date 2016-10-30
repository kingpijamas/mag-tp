package org.mag.tp.util

import akka.actor.Actor.Receive

trait ReceiveChaining {
  def chain(receives: Traversable[Receive]): Receive = receives.reduce(_ orElse _)
  def chain(receives: Receive*): Receive = chain(receives)
}
