package org.tpmag

import akka.actor.Actor
import scala.concurrent.duration.FiniteDuration
import akka.actor.ActorRef

object ExternallyTimedActor {
  case class CurrentTime(time: Time)
}

trait ExternallyTimedActor extends Actor with Scheduled {
  import ExternallyTimedActor._
  import Timer.GetCurrentTime

  var time: Option[Time] = None

  def timer: ActorRef
  def timed: Receive

  def untimed: Receive = {
    case msg if msg == timerMessage =>
      println(s"$self on $msg: What time is it?")
      timer ! GetCurrentTime

    case CurrentTime(time) =>
      println(s"$self: yay, thanks!")
      this.time = Some(time)
      context.become(timed)
  }

  def receive: Receive = untimed
}

object Timer {
  case object GetCurrentTime
}

trait Timer extends ChainingActor {
  import Timer._
  import ExternallyTimedActor.CurrentTime

  def time: Time

  private[this] def respondToTimeRequests: Receive = {
    case GetCurrentTime =>
      println(s"$self: oh $sender it's $time")
      sender ! CurrentTime(time)
  }

  registerReceive(respondToTimeRequests)
}
