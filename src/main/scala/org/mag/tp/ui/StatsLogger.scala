package org.mag.tp.ui

import akka.actor.{Actor, ActorRef, actorRef2Scala}
import com.softwaremill.tagging.@@
import org.mag.tp.domain.WorkArea
import org.mag.tp.util.{PausableActor, Scheduled}

import scala.collection.{immutable, mutable}
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

object StatsLogger {
  sealed trait TypeAnnotation
  trait TimerFreq extends TypeAnnotation

  case object FlushLogSummary

  //  val ActionsToLog = immutable.Map(
  //    "work" -> classOf[Work],
  //    "loiter" -> classOf[Loiter]
  //  )

  case class StatsForAction(currentCount: Int, changedCount: Int)
}

class StatsLogger(val timerFreq: FiniteDuration @@ StatsLogger.TimerFreq,
                  val frontend: ActorRef @@ FrontendActor)
  extends Actor with Scheduled with PausableActor {
  import FrontendActor._
  import StatsLogger._
  import WorkArea._
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
      //      val actionStats = ActionsToLog mapValues { actionClass =>
      //        StatsForAction(
      //          currentCount = actorsCountFor(actionClass),
      //          changedCount = changedActorsCountFor(actionClass)
      //        )
      //      }
      val actionStats = immutable.Map(
        "work" -> StatsForAction(currentCount = actorsCountFor[Work], changedCount = changedActorsCountFor[Work]),
        "loiter" -> StatsForAction(currentCount = actorsCountFor[Loiter], changedCount = changedActorsCountFor[Loiter])
      )
      prevActionsByActor = actionsByActor
      actionsByActor = mutable.Map[ActorRef, mutable.Buffer[WorkArea.Action]]()
      frontend ! StatsLog(actionStats)

    case _ => // ignore unknown messages
  }

  private[this] def actorsCountFor[A: ClassTag]: Int =
    actionsByActor.values count isDominant[A]

  private[this] def changedActorsCountFor[A: ClassTag]: Int = actionsByActor count {
    case (actor, actions) if (isDominant[A](actions)) =>
      prevActionsByActor.get(actor) match {
        case Some(oldActions) => !isDominant[A](oldActions)
        case _ => false
      }
    case _ => false
  }

  private[this] def isDominant[A: ClassTag](actions: Traversable[Action]): Boolean = {
    val actionClass = implicitly[ClassTag[A]].runtimeClass
    val dominantActionCount = actions count actionClass.isInstance
    dominantActionCount > (actions.size / 2.0)
  }
}
