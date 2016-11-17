package org.mag.tp.ui

import akka.actor.{Actor, ActorRef, Props, actorRef2Scala}
import com.softwaremill.macwire.wire
import com.softwaremill.tagging.@@
import org.mag.tp.domain.WorkArea
import org.mag.tp.util.{Scheduled, Stats}

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

object WorkLogger {
  case object ToggleLogging
  case object FlushLogSummary
}

class WorkLogger(val batchSize: Int,
                 val timerFreq: FiniteDuration,
                 val frontend: ActorRef @@ FrontendActor)
  extends Actor with Scheduled {
  import FrontendActor._
  import WorkArea._
  import WorkLogger._
  import context._

  def timerMessage: Any = FlushLogSummary

  val actionsByActor = mutable.Map[ActorRef, mutable.Buffer[WorkArea.Actions]]()

  def receive: Receive = {
    case ToggleLogging => become(loggingEnabled)
    case _ => // ignore messages until logging is toggled
  }

  def loggingEnabled: Receive = {
    case ToggleLogging => unbecome()

    case action: WorkArea.Actions =>
      val knownActions = actionsByActor.getOrElse(sender, mutable.Buffer())
      knownActions += action
      actionsByActor(sender) = knownActions

    case FlushLogSummary =>
      val workStats = statsFor(Work)
      val loiteringStats = statsFor(Loiter)
      actionsByActor.clear()
      frontend ! WorkLog(workStats, loiteringStats)

    case _ => // ignore unknown messages
  }

  private[this] def statsFor(action: Actions): ActionStats = {
    val actions = actionsByActor.values
    Stats.full(actions map (_ count (_ == action)))
  }
}
