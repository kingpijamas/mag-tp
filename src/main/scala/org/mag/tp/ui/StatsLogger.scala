package org.mag.tp.ui

import akka.actor.{Actor, ActorRef, actorRef2Scala}
import com.softwaremill.tagging.@@
import org.mag.tp.domain.WorkArea.{Action, Loiter, Work}
import org.mag.tp.domain.employee.Group
import org.mag.tp.domain.WorkArea
import org.mag.tp.ui.FrontendActor.StatsLog
import org.mag.tp.ui.StatsLogger.{FlushLogSummary, GroupActionStats, MultiMap}
import org.mag.tp.util.actor.{Pausing, Scheduling}

import scala.collection.{immutable, mutable}
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

object StatsLogger {
  case object FlushLogSummary

  //  val ActionsToLog = immutable.Map(
  //    "work" -> classOf[Work],
  //    "loiter" -> classOf[Loiter]
  //  )

  case class GroupActionStats(currentCount: Int, changedCount: Int)

  type MultiMap[K, V] = mutable.Map[K, mutable.Buffer[V]]

  object MultiMap {
    def apply[K, V](): MultiMap[K, V] = mutable.Map()
  }
}

class StatsLogger(val timerFreq: Option[FiniteDuration] @@ StatsLogger,
                  val frontend: ActorRef @@ FrontendActor)
  extends Actor with Scheduling with Pausing {
  import context._

  def timerMessage: Any = FlushLogSummary

  var prevActionsByGroup = MultiMap[Group, Action]()
  var actionsByGroup = MultiMap[Group, Action]()

  var prevActionsByEmployee = MultiMap[ActorRef, Action]()
  var actionsByEmployee = MultiMap[ActorRef, Action]()

  def receive: Receive = paused

  override def onPauseEnd(): Unit = {
    become(loggingEnabled)
  }

  def loggingEnabled: Receive = respectPauses orElse {
    case action: WorkArea.Action =>
      val knownEmployeeActions = actionsByEmployee.getOrElseUpdate(action.employee, mutable.Buffer())
      knownEmployeeActions += action

      val knownGroupActions = actionsByGroup.getOrElseUpdate(action.group, mutable.Buffer())
      knownGroupActions += action

    case FlushLogSummary =>
      //      val actionStats = ActionsToLog mapValues { actionClass =>
      //        StatsForAction(
      //          currentCount = actorsCountFor(actionClass),
      //          changedCount = changedActorsCountFor(actionClass)
      //        )
      //      }

      val actionStats = immutable.Map(
        "work" -> statsByGroupId[Work],
        "loiter" -> statsByGroupId[Loiter]
      )

      prevActionsByEmployee = actionsByEmployee
      actionsByEmployee = mutable.Map()
      prevActionsByGroup = actionsByGroup
      actionsByGroup = mutable.Map()

      frontend ! StatsLog(actionStats)

    case _ => // ignore unknown messages
  }

  private[this] def statsByGroupId[A: ClassTag]: immutable.Map[String, GroupActionStats] = {
    val relevantActionsByGroup = actionsByGroup filter { case (_, actions) => isDominant[A](actions) }

    val stats = relevantActionsByGroup map { case (group, actions) =>
      val relevantEmployees = actions map (_.employee)
      val changedRelevantActorsCount = relevantEmployees count {
        prevActionsByEmployee.get(_) match {
          case Some(oldActions) => !isDominant[A](oldActions)
          case _ => false
        }
      }
      val actionStats = GroupActionStats(
        currentCount = relevantEmployees.size,
        changedCount = changedRelevantActorsCount
      )
      group.id -> actionStats
    }

    immutable.Map(stats.toSeq: _*)
  }

  private[this] def isDominant[A: ClassTag](actions: Traversable[Action]): Boolean = {
    val actionClass = implicitly[ClassTag[A]].runtimeClass
    val dominantActionCount = actions count actionClass.isInstance
    dominantActionCount > (actions.size / 2.0)
  }
}
