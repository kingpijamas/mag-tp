package org.mag.tp.util

import akka.actor.Actor
import akka.routing.Router

trait PartiallyBroadcastingActor extends Actor {
  def visibility: Int
  def partiallyBroadcastables: Router

  def partialBroadcast(msg: Any): Unit = {
    (0 until visibility).foreach { _ =>
      partiallyBroadcastables.route(msg, sender)
    }
  }
}
