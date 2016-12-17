package org.mag.tp.util.actor

import akka.actor.{Actor, ActorRef}

trait MandatoryBroadcasts extends Actor {
  def mandatoryBroadcastables: Traversable[ActorRef]

  def mandatoryBroadcast(msg: Any): Unit = {
    mandatoryBroadcastables.foreach(_.tell(msg, sender))
  }
}
