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
    println("Sneaking in...")
    spendTime()
    theftVictim ! StealingAttempt(time.get, amount)
  }

  def stealingFollowup: Receive = {
    case Success(_) => println("Bwahaha!")
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
        println("Aha! thief!")
        onTheftFailure(time, amount)
      } else {
        onTheftSuccess(time, amount)
        sender ! Success(amount)
      }
  }
}
