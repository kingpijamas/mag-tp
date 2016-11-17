package org.mag.tp.ui

import akka.actor.{Actor, ActorRef, Props}
import com.softwaremill.tagging._
import org.atmosphere.cpr.AtmosphereResourceFactory
import org.mag.tp.domain.WorkArea
import org.mag.tp.util.Stats.FullStats
import spray.json._
import DefaultJsonProtocol._

import scala.collection.mutable
import org.mag.tp.ui.WorkLogger.ToggleLogging

object FrontendActor {
  type ActionStats = FullStats[Int]

  // FIXME: these two shouldn't be necessary!
  implicit def fullStatsFormatter: JsonFormat[ActionStats] = jsonFormat7(FullStats.apply[Int])

  implicit val workLogFormatter = jsonFormat3(WorkLog)

  case class Connection(clientUuid: String)

  // messages
  case object StartSimulation
  // FIXME: type won't be necessary once json-shapeless comes into action
  case class WorkLog(workStats: ActionStats, loiteringStats: ActionStats, `type`: String = "workLog")
}

class FrontendActor(workAreaPropsFactory: (Traversable[ActorRef] => Props @@ WorkArea),
                    workLoggersFactory: ((ActorRef @@ FrontendActor) => (Props @@ WorkLogger)))
  extends Actor {
  import FrontendActor._

  var connectedClientUuids = mutable.Buffer[String]()
  var workArea = Option.empty[ActorRef]
  var workLoggers = Seq[ActorRef]()
  var restartCount = 0

  def receive: Receive = {
    case Connection(clientUuid: String) =>
      connectedClientUuids += clientUuid
      println(s"connected clients: $connectedClientUuids")

    case StartSimulation =>
      workArea.foreach(context.stop)
      workLoggers.foreach(context.stop)
      createWorkLogger()
      workArea = Some(createWorkArea())
      restartCount += 1

    case workLog: WorkLog =>
      val msg = asMsg(workLog)
      println(msg)
      connectedClientUuids.foreach(sendTo(_, msg))

    case _ => // ignore unknown messages
  }

  private[this] def createWorkLogger() = {
    val workLogger = context.actorOf(workLoggersFactory(self.taggedWith[FrontendActor]))
    workLoggers = Seq(workLogger)
    workLogger ! ToggleLogging
    workLogger
  }

  private[this] def createWorkArea() = {
    context.actorOf(workAreaPropsFactory(workLoggers), s"work-area-$restartCount")
  }

  private[this] def sendTo(clientUuid: String, msg: String): Unit = {
    val resourceFactory = Option(AtmosphereResourceFactory.getDefault.find(clientUuid))
    resourceFactory map (_.getBroadcaster.broadcast(msg))
  }

  private[this] def asMsg = {
    def jsonify: PartialFunction[Any, JsValue] = {
      case workLog: WorkLog => (workLog: WorkLog).toJson
    }

    jsonify andThen (_.compactPrint)
  }
}
