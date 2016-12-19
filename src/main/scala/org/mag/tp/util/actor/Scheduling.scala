package org.mag.tp.util.actor

import akka.actor.{Actor, Cancellable}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.language.postfixOps

trait Scheduling {
  this: Actor =>

  def initialDelay: FiniteDuration = 0 seconds

  def timerMessage: Any

  def timerFreq: Option[FiniteDuration]

  val tick: Option[Cancellable] = timerFreq match {
    case Some(freq) =>
      val scheduler = context.system.scheduler
      val scheduledInterruption = scheduler.schedule(initialDelay, freq, this.self, timerMessage)
      Some(scheduledInterruption)

    case _ => None
  }

  override def postStop(): Unit = tick.foreach(_.cancel())
}
