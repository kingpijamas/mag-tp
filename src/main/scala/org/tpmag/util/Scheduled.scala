package org.tpmag.util

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps
import akka.actor.Actor

trait Scheduled {
  self: Actor =>

  def initialDelay = 0 seconds
  def timerMessage: Any
  def timerFreq: FiniteDuration

  val tick = context.system.scheduler.schedule(initialDelay, timerFreq, this.self, timerMessage)
  override def postStop(): Unit = tick.cancel()
}
