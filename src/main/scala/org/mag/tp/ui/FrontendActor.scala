package org.mag.tp.ui

import akka.actor.{Actor, ActorRef, Props}
import com.softwaremill.tagging._
import org.atmosphere.cpr.AtmosphereResourceFactory
import org.mag.tp.domain.WorkArea
import org.mag.tp.ui.StatsLogger.ToggleLogging
import org.mag.tp.util.Stats.FullStats
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.collection.mutable

object FrontendActor {
  type ActionStats = FullStats[Int]

  // FIXME: these two shouldn't be necessary!
  implicit def fullStatsFormatter: JsonFormat[ActionStats] = jsonFormat7(FullStats.apply[Int])

  implicit val workLogFormatter = jsonFormat5(WorkLog)

  case class Connection(clientUuid: String)

  // messages
  case object StartSimulation
  case object StopSimulation
  // FIXME: type won't be necessary once json-shapeless comes into action
  case class WorkLog(workingCount: Int,
                     newWorkersCount: Int,
                     // workStats: ActionStats,
                     loiteringCount: Int,
                     newLoiterersCount: Int,
                     // loiteringStats: ActionStats,
                     `type`: String = "workLog")
}

class FrontendActor(workAreaPropsFactory: (Traversable[ActorRef] => Props @@ WorkArea),
                    statsLoggersFactory: ((ActorRef @@ FrontendActor) => (Props @@ StatsLogger)))
  extends Actor {
  import FrontendActor._

  var connectedClientUuids = mutable.Buffer[String]()
  var workArea = Option.empty[ActorRef]
  var statsLoggers = Seq[ActorRef]()
  var stopsCount = 0

  def receive: Receive = {
    case Connection(clientUuid: String) =>
      connectedClientUuids += clientUuid
      println(s"connected clients: $connectedClientUuids")

    case StartSimulation =>
      stopChildren()
      createWorkLogger()
      workArea = Some(createWorkArea())

    case StopSimulation =>
      stopChildren()

    case workLog: WorkLog =>
      val msg = asMsg(workLog)
      connectedClientUuids.foreach(sendTo(_, msg))

    case _ => // ignore unknown messages
  }

  private[this] def createWorkLogger() = {
    val workLogger = context.actorOf(statsLoggersFactory(self.taggedWith[FrontendActor]))
    statsLoggers = Seq(workLogger)
    workLogger ! ToggleLogging
    workLogger
  }

  private[this] def createWorkArea() = {
    context.actorOf(workAreaPropsFactory(statsLoggers), s"work-area-$stopsCount")
  }

  private[this] def stopChildren() = {
    stopsCount += 1
    workArea.foreach(context.stop)
    statsLoggers.foreach(context.stop)
  }

  private[this] def sendTo(clientUuid: String, msg: String) = {
    val resourceFactory = Option(AtmosphereResourceFactory.getDefault.find(clientUuid))
    resourceFactory.foreach(_.getBroadcaster.broadcast(msg))
  }

  private[this] def asMsg = {
    def jsonify: PartialFunction[Any, JsValue] = {
      case workLog: WorkLog => (workLog: WorkLog).toJson
    }

    jsonify andThen (_.compactPrint)
  }
}
