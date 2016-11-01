package org.mag.tp.ui

import scala.annotation.migration
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

import org.mag.tp.domain.WorkArea
import org.mag.tp.util.Scheduled

import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{ @@ => @@ }

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props

object WorkLogger {
  case object FlushLogSummary

  def props(batchSize: Int,
            timerFreq: FiniteDuration,
            frontend: ActorRef @@ FrontendActor): Props =
    Props(wire[WorkLogger])
}

class WorkLogger(
  val batchSize: Int,
  val timerFreq: FiniteDuration,
  val frontend: ActorRef)
    extends Actor with Scheduled {
  import FrontendActor._
  import WorkLogger._
  import WorkArea._

  def timerMessage: Any = FlushLogSummary

  val actionsByActor = mutable.Map[ActorRef, mutable.Buffer[WorkArea.Actions]]()

  def receive: Receive = {
    case action: WorkArea.Actions =>
      val knownActions = actionsByActor.getOrElse(sender, mutable.Buffer())
      knownActions += action
      actionsByActor(sender) = knownActions

    case FlushLogSummary =>
      val allActions = actionsByActor.values.flatten
      val totalWork = allActions.count { _ == Work }
      val totalLoitering = allActions.count { _ == Loiter }
      println(s"${self.path}: ${WorkLog(totalWork, totalLoitering)}")
      actionsByActor.clear()
      frontend ! WorkLog(totalWork, totalLoitering)

    case _ => // ignore unknown messages
  }
}
