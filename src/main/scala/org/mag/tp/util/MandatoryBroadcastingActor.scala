package org.mag.tp.util

import akka.actor.{Actor, ActorRef}

trait MandatoryBroadcastingActor extends Actor {
  def mandatoryBroadcastables: Traversable[ActorRef]

  def mandatoryBroadcast(msg: Any): Unit = {
    mandatoryBroadcastables.foreach(_.tell(msg, sender))
  }
}
