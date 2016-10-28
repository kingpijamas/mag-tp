package org.tpmag.domain.behaviour

import org.tpmag.util.Scheduled
import org.tpmag.domain.Time

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import org.tpmag.util.ChainingActor

object ExternallyTimedActor {
  case class CurrentTime(time: Time)
}

trait ExternallyTimedActor extends Actor with Scheduled {
  import ExternallyTimedActor._
  import TimerActor.GetCurrentTime

  // TODO: move elsewhere!
  var time: Option[Time] = None
  //

  def timer: ActorRef
  def timed: Receive

  // TODO: move elsewhere!
  def spendTime(time: Time = 1): Unit = { this.time = this.time.map(_ + 1) }
  def recoverTime(time: Time = 1): Unit = { this.time = this.time.map(_ - 1) }
  //

  def untimed: Receive = {
    case msg if msg == timerMessage =>
      println(s"$self: What time is it?")
      timer ! GetCurrentTime

    case CurrentTime(time) =>
      println(s"$self: yay, thanks!")
      this.time = Some(time)
      context.become(timed)
  }

  def receive: Receive = untimed
}

object TimerActor {
  case object GetCurrentTime
}

trait TimerActor extends ChainingActor {
  import ExternallyTimedActor.CurrentTime
  import TimerActor._

  def time: Time

  registerReceive {
    case GetCurrentTime =>
      println(s"$self: oh $sender it's $time")
      sender ! CurrentTime(time)
  }
}
