package org.mag.tp.ui

import akka.actor.{Actor, ActorRef, actorRef2Scala}
import com.softwaremill.tagging.@@
import org.mag.tp.domain.WorkArea
import org.mag.tp.util.{PausableActor, Scheduled, Stats}

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

object StatsLogger {
  sealed trait TypeAnnotation
  trait TimerFreq extends TypeAnnotation

  case object FlushLogSummary
}

class StatsLogger(val timerFreq: FiniteDuration @@ StatsLogger.TimerFreq,
                  val frontend: ActorRef @@ FrontendActor)
  extends Actor with Scheduled with PausableActor {
  import FrontendActor._
  import WorkArea._
  import StatsLogger._
  import context._

  def timerMessage: Any = FlushLogSummary

  var prevActionsByActor = mutable.Map[ActorRef, mutable.Buffer[WorkArea.Action]]()
  var actionsByActor = mutable.Map[ActorRef, mutable.Buffer[WorkArea.Action]]()

  def receive: Receive = paused

  override def onPauseEnd(): Unit = {
    become(loggingEnabled)
  }

  def loggingEnabled: Receive = respectPauses orElse {
    case action: WorkArea.Action =>
      val knownActions = actionsByActor.getOrElse(sender, mutable.Buffer())
      knownActions += action
      actionsByActor(sender) = knownActions

    case FlushLogSummary =>
      val workingCount = actorsCount[Work]
      val newWorkersCount = changedActorsCount[Work]
      //  val workStats = statsFor(Work)
      val loiteringCount = actorsCount[Loiter]
      val newLoiterersCount = changedActorsCount[Loiter]
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

  private[this] def actorsCount[A:ClassTag]: Int = {
    actionsByActor.values count (isDominant[A](_))
  }

  private[this] def changedActorsCount[A:ClassTag]: Int = {
    actionsByActor count { case (actor, actions) =>
      if (isDominant[A](actions)) {
        prevActionsByActor.get(actor) match {
          case Some(oldActions) => !isDominant[A](oldActions)
          case _ => false
        }
      } else {
        false
      }
    }
  }

  private[this] def isDominant[A:ClassTag](actions: Traversable[WorkArea.Action]): Boolean = {
    val clazz = implicitly[ClassTag[A]].runtimeClass
    val dominantActionCount = actions count (clazz.isInstance(_))
    dominantActionCount > (actions.size / 2.0)
  }

  private[this] def statsFor[A:ClassTag]: ActionStats = {
    val clazz = implicitly[ClassTag[A]].runtimeClass
    val actions = actionsByActor.values
    Stats.full(actions map (_ count (clazz.isInstance(_))))
  }
}
