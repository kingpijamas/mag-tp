package org.mag.tp.ui

import akka.actor.{Actor, ActorRef, actorRef2Scala}
import com.softwaremill.tagging.@@
import org.mag.tp.domain.WorkArea.{Action, Loiter, Work}
import org.mag.tp.domain.{WorkArea, employee}
import org.mag.tp.ui.FrontendActor.StatsLog
import org.mag.tp.ui.StatsLogger.{FlushLogSummary, GroupActionStats}
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
}

class StatsLogger(val employeeGroups: immutable.Seq[employee.Group],
                  val timerFreq: FiniteDuration @@ StatsLogger,
                  val frontend: ActorRef @@ FrontendActor)
  extends Actor with Scheduling with Pausing {
  import context._

  def timerMessage: Any = FlushLogSummary

  var prevActionsByGroup = mutable.Map[employee.Group, mutable.Buffer[WorkArea.Action]]()
  var actionsByGroup = mutable.Map[employee.Group, mutable.Buffer[WorkArea.Action]]()

  var actionsByEmployee = mutable.Map[ActorRef, mutable.Buffer[WorkArea.Action]]()
  var prevActionsByEmployee = mutable.Map[ActorRef, mutable.Buffer[WorkArea.Action]]()

  def receive: Receive = paused

  override def onPauseEnd(): Unit = {
    become(loggingEnabled)
  }

  def loggingEnabled: Receive = respectPauses orElse {
    case action: WorkArea.Action =>
      val knownEmployeeActions = actionsByEmployee.getOrElseUpdate(sender, mutable.Buffer())
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
        "work" -> immutable.Map(statsByGroupId[Work].toSeq: _*),
        "loiter" -> immutable.Map(statsByGroupId[Loiter].toSeq: _*)
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
