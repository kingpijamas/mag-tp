package org.mag.tp.ui

import akka.actor.{Actor, ActorRef, Props}
import com.softwaremill.tagging.{@@, Tagger}
import org.atmosphere.cpr.AtmosphereResourceFactory
import org.mag.tp.domain.WorkArea
import org.mag.tp.ui.FrontendActor.{ClientConnected, SimulationStep, StartSimulation, StatsLog, StopSimulation}
import org.mag.tp.ui.StatsLogger.GroupActionStats
import org.mag.tp.util.Stats.FullStats
import org.mag.tp.util.actor.Pausing.{Pause, Resume}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.collection.{immutable, mutable}
import scala.concurrent.duration.FiniteDuration

object FrontendActor {
  type ActionStats = FullStats[Int]

  // FIXME: these two shouldn't be necessary!
  // implicit def fullStatsFormatter: JsonFormat[ActionStats] = jsonFormat7(FullStats.apply[Int])
  implicit val GroupActionStatsFormatter = jsonFormat2(GroupActionStats)
  implicit val StatsLogFormatter = jsonFormat2(StatsLog)

  case class ClientConnected(clientUuid: String)

  // messages
  case object StartSimulation
  case object StopSimulation
  case object SimulationStep
  // FIXME: type won't be necessary once json-shapeless comes into action
  case class StatsLog(stats: immutable.Map[String, immutable.Map[String, GroupActionStats]],
                      `type`: String = "statsLog")
  //  workingCount: Int,
  //  newWorkersCount: Int,
  //  // workStats: ActionStats,
  //  loiteringCount: Int,
  //  newLoiterersCount: Int,
  //  // loiteringStats: ActionStats,
}

class FrontendActor(timerFreq: FiniteDuration @@ StatsLogger,
                    workAreaPropsFactory: (Traversable[ActorRef] => Props @@ WorkArea),
                    statsLoggersFactory: ((ActorRef @@ FrontendActor) => (Props @@ StatsLogger)))
  extends Actor {
  import context._

  var connectedClientUuids = mutable.Buffer[String]()
  var workArea = Option.empty[ActorRef]
  var statsLoggers = Seq[ActorRef]()
  var stopsCount = 0

  def receive: Receive = {
    case ClientConnected(clientUuid: String) =>
      connectedClientUuids += clientUuid
      println(s"connected clients: $connectedClientUuids")

    case StartSimulation =>
      stopChildren()
      createstatsLogger()
      workArea = Some(createWorkArea())

    case Pause =>
      pauseChildren()

    case Resume =>
      resumeChildren()

    case SimulationStep =>
      pauseChildren()
      context.system.scheduler.scheduleOnce(timerFreq) {
        resumeChildren()
      }
      context.system.scheduler.scheduleOnce(timerFreq * 2) {
        pauseChildren()
      }

    case StopSimulation =>
      stopChildren()

    case statsLog: StatsLog =>
      val statsMsg = asMsg(statsLog)
      connectedClientUuids.foreach(sendTo(_, statsMsg))

    case _ => // ignore unknown messages
  }

  private[this] def createstatsLogger() = {
    val statsLogger = context.actorOf(statsLoggersFactory(self.taggedWith[FrontendActor]))
    statsLoggers = Seq(statsLogger)
    statsLogger ! Resume
    statsLogger
  }

  private[this] def createWorkArea() = {
    context.actorOf(workAreaPropsFactory(statsLoggers)) // , s"work-area-$stopsCount"
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
      case statsLog: StatsLog => (statsLog: StatsLog).toJson
    }

    jsonify andThen (_.compactPrint)
  }

  private[this] def pauseChildren(): Unit = {
    workArea.foreach(_ ! Pause)
    statsLoggers.foreach(_ ! Pause)
  }

  private[this] def resumeChildren(): Unit = {
    workArea.foreach(_ ! Resume)
    statsLoggers.foreach(_ ! Resume)
  }
}
