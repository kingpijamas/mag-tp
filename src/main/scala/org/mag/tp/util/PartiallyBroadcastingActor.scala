package org.mag.tp.util

import akka.actor.Actor
import akka.routing.Router

trait PartiallyBroadcastingActor extends Actor {
  def broadcastability: Int
  def partiallyBroadcastables: Router

  def partialBroadcast(msg: Any): Unit = {
    (0 until broadcastability).foreach { _ =>
      partiallyBroadcastables.route(msg, sender)
    }
  }
}
