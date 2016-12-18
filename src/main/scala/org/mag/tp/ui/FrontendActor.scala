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
  implicit val GroupActionStatsFormatter = jsonFormat2(GroupActionStats)
  implicit val StatsLogFormatter = jsonFormat2(StatsLog)

  // messages
  case class ClientConnected(clientUuid: String)
  case object StartSimulation
  case object StopSimulation
  case object SimulationStep
  // FIXME: type won't be necessary once json-shapeless comes into action
  case class StatsLog(stats: immutable.Map[String, immutable.Map[String, GroupActionStats]],
                      `type`: String = "statsLog")
}

class FrontendActor(timerFreq: Option[FiniteDuration] @@ StatsLogger,
                    workAreaPropsFactory: (Traversable[ActorRef] => Props @@ WorkArea),
                    statsLoggersFactory: ((ActorRef @@ FrontendActor) => (Props @@ StatsLogger)))
  extends Actor {
  import context._

  var connectedClientUuids = mutable.Buffer[String]()
  var children = immutable.Seq[ActorRef]()
  var workArea = Option.empty[ActorRef]
  var statsLoggers = immutable.Seq[ActorRef]()
  var stopsCount = 0

  def receive: Receive = {
    case ClientConnected(clientUuid: String) =>
      connectedClientUuids += clientUuid
      println(s"connected clients: $connectedClientUuids")

    case StartSimulation =>
      stopChildren()
      createChildren()

    case Pause => pauseChildren()

    case Resume => resumeChildren()

    case SimulationStep =>
      timerFreq.foreach { freq =>
        pauseChildren()
        context.system.scheduler.scheduleOnce(freq) { resumeChildren() }
        context.system.scheduler.scheduleOnce(freq * 2) { pauseChildren() }
      }

    case StopSimulation =>
      stopChildren()

    case statsLog: StatsLog =>
      val statsMsg = asMsg(statsLog)
      connectedClientUuids.foreach(sendTo(_, statsMsg))

    case _ => // ignore unknown messages
  }

  private[this] def stopChildren() = {
    stopsCount += 1
    children.foreach(context.stop)
  }

  private[this] def createChildren() = {
    def createStatsLogger() = {
      context.actorOf(statsLoggersFactory(self.taggedWith[FrontendActor]))
    }

    def createWorkArea(statsLoggers: immutable.Seq[ActorRef]) = {
      context.actorOf(workAreaPropsFactory(statsLoggers)) // , s"work-area-$stopsCount"
    }

    statsLoggers = immutable.Seq(createStatsLogger())
    workArea = Some(createWorkArea(statsLoggers))
    children = workArea.get +: statsLoggers

    statsLoggers.foreach(_ ! Resume)
  }

  private[this] def pauseChildren(): Unit = {
    children.foreach(_ ! Pause)
  }

  private[this] def resumeChildren(): Unit = {
    children.foreach(_ ! Resume)
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

}
