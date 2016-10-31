package org.mag.tp.util

import akka.routing.Router
import akka.actor.Actor
import akka.actor.ActorRef

trait MandatoryBroadcastingActor extends Actor {
  def mandatoryBroadcastables: Traversable[ActorRef]

  def mandatoryBroadcast(msg: Any): Unit = {
    mandatoryBroadcastables.foreach(_.tell(msg, sender))
  }
}
