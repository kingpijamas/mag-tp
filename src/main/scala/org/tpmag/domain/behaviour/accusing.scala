package org.tpmag.domain.behaviour

import org.tpmag.util.ChainingActor

import akka.actor.ActorRef
import scala.collection.mutable
import scala.concurrent.duration._
import org.tpmag.domain.behaviour.TheftVictimActor.StealingAttempt
import org.tpmag.domain.behaviour.AccusationsReceiver.VeredictVote

trait AccusingActor extends SocialActor {
  import AccusationsReceiver._

  def accusationsReceiver: ActorRef

  def accuse(accused: ActorRef, crime: StealingAttempt): Unit = {
    accusationsReceiver ! Accusation(accused, crime)
  }

  //  def proposeCulprit: Option[ActorRef] = {
  //    val knownNonFriends = relations.filter { case (other, _) => !isFriend(other) }
  //
  //    if (!knownNonFriends.isEmpty) {
  //      val (other, _) = relations.minBy { case (_, relation) => relation }
  //      Some(other)
  //    } else {
  //      None
  //    }
  //  }
}

object AccusationsReceiver {
  import TheftVictimActor._
  import JuryActor._

  // messages
  case class Accusation(accused: ActorRef, crime: StealingAttempt)
  case class CaseStart(crime: StealingAttempt)
  case class VeredictVote(crime: StealingAttempt, veredict: Veredict)
  case class VotingEnd(crime: StealingAttempt)

  sealed trait Case
  case class CaseFormulation(accuseds: mutable.Buffer[ActorRef] = mutable.Buffer()) extends Case
  case class StartedCase(accused: ActorRef, votes: mutable.Buffer[Veredict] = mutable.Buffer()) extends Case
  case object EndedCase extends Case
}

trait AccusationsReceiver extends ChainingActor {
  import AccusationsReceiver._
  import TheftVictimActor._
  import JuryActor._
  import context._

  def accusationReceptionTime: FiniteDuration
  def veredictVotesReceptionTime: FiniteDuration
  def onGuiltyVeredict(accused: ActorRef): Unit
  def onNotGuiltyVeredict(accused: ActorRef): Unit

  def jury: ActorRef

  val casesByCrime = mutable.Map[StealingAttempt, Case]()

  private[this] def isCaseStarted(crime: StealingAttempt): Boolean = casesByCrime.get(crime) match {
    case None | Some(CaseFormulation(_)) => true
    case _                               => false
  }

  private[this] def isVotingEnded(crime: StealingAttempt): Boolean = casesByCrime.get(crime) match {
    case Some(StartedCase(_, _)) => true
    case _                       => false
  }

  protected def chooseAccused(crime: StealingAttempt): ActorRef = {
    val CaseFormulation(accuseds) = casesByCrime(crime)
    val accusedsGroupedByAccusations = accuseds.groupBy(identity)
    val (accused, _) = accusedsGroupedByAccusations.maxBy {
      case (_, accusations) => accusations.size
    }
    accused
  }

  protected def isGuilty(votes: Traversable[Veredict]): Boolean =
    votes.count(_.isGuilty) > votes.count(!_.isGuilty)

  registerReceive {
    case Accusation(accused, crime) if !isCaseStarted(crime) =>
      val CaseFormulation(knownAccuseds) = casesByCrime.getOrElse(crime, CaseFormulation())
      knownAccuseds += accused
      if (knownAccuseds.size == 1) {
        context.system.scheduler.scheduleOnce(accusationReceptionTime, self, CaseStart(crime))
      }
    case Accusation(_, _) => // ignore accusation, you are late mate

    case CaseStart(crime) if !isVotingEnded(crime) =>
      casesByCrime(crime) = StartedCase(chooseAccused(crime))

    case VeredictVote(crime: StealingAttempt, vote: Veredict) =>
      val StartedCase(accused, votes) = casesByCrime(crime)
      votes += vote

      if (votes.size == 1) {
        context.system.scheduler.scheduleOnce(veredictVotesReceptionTime, self, VotingEnd(crime))
      }
    case CaseStart(_) => // ignore vote, you are late mate

    case VotingEnd(crime) =>
      val StartedCase(accused, votes) = casesByCrime(crime)
      if (isGuilty(votes)) {
        onGuiltyVeredict(accused)
      } else {
        onNotGuiltyVeredict(accused)
      }
      casesByCrime(crime) = EndedCase
  }
}

object JuryActor {
  // messages
  case class VeredictRequest(accused: ActorRef, crime: StealingAttempt)

  sealed trait Veredict {
    def isGuilty: Boolean
  }
  case object Guilty extends Veredict {
    def isGuilty: Boolean = true
  }
  case object NotGuilty extends Veredict {
    def isGuilty: Boolean = false
  }
}

trait JuryActor extends SocialActor {
  import JuryActor._

  def giveVeredict(accused: ActorRef, crime: StealingAttempt): Veredict

  def respondToVeredictRequests: Receive = {
    case VeredictRequest(accused: ActorRef, crime: StealingAttempt) =>
      sender() ! VeredictVote(crime, giveVeredict(accused, crime))
  }
}
