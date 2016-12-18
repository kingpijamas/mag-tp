package org.mag.tp.util.actor

import akka.actor.Actor
import org.mag.tp.util.actor.Pausing.{Pause, Resume}

object Pausing {
  case object Pause
  case object Resume
}

trait Pausing extends Actor {
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
