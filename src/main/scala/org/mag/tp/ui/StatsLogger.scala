package org.mag.tp.ui

import akka.actor.{Actor, ActorRef, actorRef2Scala}
import com.softwaremill.tagging.@@
import org.mag.tp.domain.WorkArea.{Action, Loiter, Work}
import org.mag.tp.domain.employee.{Employee, Group}
import org.mag.tp.domain.WorkArea
import org.mag.tp.ui.FrontendActor.StatsLog
import org.mag.tp.ui.StatsLogger.{FlushLogSummary, GroupActionStats, MultiMap, State}
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


  type MultiMap[K, V] = mutable.Map[K, mutable.Buffer[V]]

  object MultiMap {
    def apply[K, V](): MultiMap[K, V] = mutable.Map()
  }

  case class State(actionsByGroup: MultiMap[Group, Action] = mutable.Map(),
                   actionsByEmployee: MultiMap[ActorRef, Action] = mutable.Map())

  case class GroupActionStats(currentCount: Int, changedCount: Int)
}

class StatsLogger(val timerFreq: Option[FiniteDuration] @@ StatsLogger,
                  val frontend: ActorRef @@ FrontendActor)
  extends Actor with Scheduling with Pausing {
  import context._

  def timerMessage: Any = FlushLogSummary

  var prevState = State()
  var state = State()

  def receive: Receive = paused

  override def onPauseEnd(): Unit = {
    become(loggingEnabled)
  }

  def loggingEnabled: Receive = respectPauses orElse {
    case action: WorkArea.Action =>
      val knownEmployeeActions = state.actionsByEmployee.getOrElseUpdate(action.employee, mutable.Buffer())
      knownEmployeeActions += action

      val knownGroupActions = state.actionsByGroup.getOrElseUpdate(action.group, mutable.Buffer())
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

      prevState = state
      state = State()

      frontend ! StatsLog(actionStats)

    case _ => // ignore unknown messages
  }

  private[this] def statsByGroupId[A: ClassTag]: immutable.Map[String, GroupActionStats] = {
    val relevantActionsByGroup = state.actionsByGroup filter { case (_, actions) =>
      isDominant[A](actions)
    }

    val stats = relevantActionsByGroup map { case (group, actions) =>
      val employeesInGroup = actions map (_.employee) toSet
      val changedEmployeesCount = employeesInGroup count (hasChanged[A](_))

      group.id -> GroupActionStats(
        currentCount = actions.size,
        changedCount = changedEmployeesCount
      )
    }

    immutable.Map(stats.toSeq: _*)
  }

  private[this] def hasChanged[A: ClassTag](employee: ActorRef): Boolean = {
    val previouslyKnownActions = prevState.actionsByEmployee.get(employee)

    previouslyKnownActions match {
      case Some(actions) => !isDominant[A](actions)
      case _ => true // TODO: check!
    }
  }

  private[this] def isDominant[A: ClassTag](actions: Traversable[Action]): Boolean = {
    val actionClass = implicitly[ClassTag[A]].runtimeClass
    val dominantActionCount = actions count actionClass.isInstance
    dominantActionCount > (actions.size / 2.0)
  }
}
