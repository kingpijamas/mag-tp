package org.mag.tp.util.actor

import akka.actor.Actor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.language.postfixOps

trait Scheduling {
  this: Actor =>

  def initialDelay: FiniteDuration = 0 seconds

  def timerMessage: Any

  def timerFreq: FiniteDuration

  val tick = context.system.scheduler.schedule(initialDelay, timerFreq, this.self, timerMessage)

  override def postStop(): Unit = tick.cancel()
}
