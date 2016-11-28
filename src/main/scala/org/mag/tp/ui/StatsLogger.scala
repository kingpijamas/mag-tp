package org.mag.tp.ui

import akka.actor.{Actor, ActorRef, actorRef2Scala}
import com.softwaremill.tagging.@@
import org.mag.tp.domain.WorkArea
import org.mag.tp.util.{Scheduled, Stats}

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

object StatsLogger {
  case object ToggleLogging
  case object FlushLogSummary
}

class StatsLogger(val batchSize: Int,
                  val timerFreq: FiniteDuration,
                  val frontend: ActorRef @@ FrontendActor)
  extends Actor with Scheduled {
  import FrontendActor._
  import WorkArea._
  import StatsLogger._
  import context._

  def timerMessage: Any = FlushLogSummary

  var prevActionsByActor = mutable.Map[ActorRef, mutable.Buffer[WorkArea.Action]]()
  var actionsByActor = mutable.Map[ActorRef, mutable.Buffer[WorkArea.Action]]()

  def receive: Receive = {
    case ToggleLogging => become(loggingEnabled)
    case _ => // ignore messages until logging is toggled
  }

  def loggingEnabled: Receive = {
    case ToggleLogging => unbecome()

    case action: WorkArea.Action =>
      val knownActions = actionsByActor.getOrElse(sender, mutable.Buffer())
      knownActions += action
      actionsByActor(sender) = knownActions

    case FlushLogSummary =>
      val workingCount = actorsCount(Work)
      val newWorkersCount = changedActorsCount(newAction = Work)
      //  val workStats = statsFor(Work)
      val loiteringCount = actorsCount(Loiter)
      val newLoiterersCount = changedActorsCount(newAction = Loiter)
      //  val loiteringStats = statsFor(Loiter)
      prevActionsByActor = actionsByActor
      actionsByActor = mutable.Map[ActorRef, mutable.Buffer[WorkArea.Action]]()
      frontend ! WorkLog(
        workingCount,
        newWorkersCount,
        // workStats,
        loiteringCount,
        newLoiterersCount
        // ,loiteringStats
      )

    case _ => // ignore unknown messages
  }

  private[this] def actorsCount(dominantAction: WorkArea.Action): Int = {
    actionsByActor.values count (isDominant(_, dominantAction))
  }

  private[this] def changedActorsCount(newAction: WorkArea.Action): Int = {
    actionsByActor count { case (actor, actions) =>
      if (isDominant(actions, newAction)) {
        prevActionsByActor.get(actor) match {
          case Some(oldActions) => !isDominant(oldActions, newAction)
          case _ => false
        }
      } else {
        false
      }
    }
  }

  private[this] def isDominant(actions: Traversable[WorkArea.Action], action: WorkArea.Action): Boolean = {
    val dominantActionCount = actions count (_ == action)
    dominantActionCount > (actions.size / 2.0)
  }

  private[this] def statsFor(action: Action): ActionStats = {
    val actions = actionsByActor.values
    Stats.full(actions map (_ count (_ == action)))
  }
}
