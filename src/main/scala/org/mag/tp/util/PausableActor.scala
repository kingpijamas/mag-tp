package org.mag.tp.util

import akka.actor.Actor

object PausableActor {
  case object Pause
  case object Resume
}

trait PausableActor extends Actor {
  import PausableActor._
  import context._

  def paused: Receive = {
    case Resume => onPauseEnd()
    case _ => // ignore it
  }

  def unpaused: Receive = {
    case Pause => onPauseStart()
  }
  def respectPauses: Receive = unpaused

  def onPauseEnd(): Unit = {
    become(receive)
  }

  def onPauseStart(): Unit = {
    become(paused)
  }
}
