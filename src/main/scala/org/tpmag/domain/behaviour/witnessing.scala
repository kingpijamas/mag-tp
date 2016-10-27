package org.tpmag.domain.behaviour

import akka.actor.Actor
import akka.actor.ActorRef
import org.tpmag.util.ChainingActor

trait WitnessingActor extends SocialActor {
  import TheftVictimActor._

  def onTheft(crime: StealingAttempt): Unit

  def witness: Receive = {
    case _: StealingAttempt if sender == this => // disregard it, I know I'm stealing

    case crime: StealingAttempt =>
      println(s"$self :O I saw $sender do $crime")
      onTheft(crime)
  }
}

trait CrimeEnvironment extends ChainingActor {
  import TheftVictimActor._

  def victim: ActorRef
  def witnessPool: ActorRef

  def witnessCount(crime: Any): Int

  def notifyVictim(victim: ActorRef, crimeAttempt: Any): Unit = alert(victim, crimeAttempt)

  registerReceive {
    case crimeAttempt: StealingAttempt =>
      (0 until witnessCount(crimeAttempt)).foreach { _ =>
        alert(witnessPool, crimeAttempt)
      }
  }

  private[this] def alert(who: ActorRef, crime: Any): Unit = { who.forward(crime) }
}
