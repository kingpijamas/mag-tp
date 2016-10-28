package org.tpmag.domain.behaviour

import akka.actor.ActorRef
import org.tpmag.domain.Time
import akka.actor.Actor
import scala.util.Random
import org.tpmag.util.ChainingActor

object StealingActor {
  case class Success(amount: Int)
}

trait StealingActor extends ExternallyTimedActor {
  import StealingActor._
  import TheftVictimActor._

  def theftVictim: ActorRef

  def steal(amount: Int): Unit = {
    spendTime()
    val victim = theftVictim
    println(s"$self: Sneaking in $victim...")
    victim ! StealingAttempt(time.get, amount)
  }

  def stealingFollowup: Receive = {
    case Success(_) => println(s"$self: Bwahaha!")
  }
}

object TheftVictimActor {
  case class StealingAttempt(time: Time, amount: Int)
}

trait TheftVictimActor extends ChainingActor {
  import StealingActor._
  import TheftVictimActor._

  def catchingPropensity: Double
  def onTheftSuccess(time: Time, amount: Int): Unit
  def onTheftFailure(time: Time, amount: Int): Unit

  registerReceive {
    case StealingAttempt(time, amount) =>
      val thiefCaught = Random.nextDouble < catchingPropensity

      if (thiefCaught) {
        println(s"$self: Aha! $sender is a thief!")
        onTheftFailure(time, amount)
      } else {
        println(s"$self: Take my goods $sender")
        onTheftSuccess(time, amount)
        sender ! Success(amount)
      }
  }
}
