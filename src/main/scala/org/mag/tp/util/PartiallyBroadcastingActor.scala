package org.mag.tp.util

import akka.routing.Router
import akka.actor.Actor

trait PartiallyBroadcastingActor extends Actor {
  def broadcastability: Int
  def partiallyBroadcastables: Router

  def broadcast(msg: Any): Unit = {
    (0 until broadcastability).foreach { _ =>
      partiallyBroadcastables.route(msg, sender)
    }
  }
}
