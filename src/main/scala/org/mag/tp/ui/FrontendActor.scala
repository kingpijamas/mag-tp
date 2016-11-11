package org.mag.tp.ui

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import org.atmosphere.cpr.AtmosphereResourceFactory
import com.softwaremill.tagging._

import akka.actor.Actor
import scala.collection.mutable
import akka.actor.Props
import org.mag.tp.domain.WorkArea
import akka.actor.ActorRef
import akka.actor.Kill

object FrontendActor {
  case class Connection(clientUuid: String)
  case object StartSimulation

  case class WorkLog(totalWork: Int, totalLoitering: Int) {
    override def toString: String =
      s"WorkLog(totalWork=$totalWork, totalLoitering=$totalLoitering)"
  }
}

class FrontendActor(workAreaPropsFactory: (() => Props @@ WorkArea)) extends Actor {
  import FrontendActor._

  var connectedClientUuids = mutable.Buffer[String]()
  var workArea: Option[ActorRef] = None
  var restartCount = 0

  def receive: Receive = {
    case Connection(clientUuid: String) =>
      connectedClientUuids += clientUuid
      println(s"connected clients: $connectedClientUuids")

    case StartSimulation =>
      workArea.foreach(context.stop(_))
      workArea = Some(context.actorOf(workAreaPropsFactory(), s"work-area-$restartCount"))
      restartCount +=1

    case msg: WorkLog =>
      val marshalledMsg = marshall(msg)
      connectedClientUuids.foreach { sendTo(_, marshalledMsg) }

    case _ => // ignore unknown messages      
  }

  private[this] def marshall(msg: Any): String = {
    val msgType = ("type" -> typeOf(msg))
    val jsonifiedMsg = msgType ~ jsonify(msg)
    compact(render(jsonifiedMsg))
  }

  private[this] def typeOf(a: Any): String = {
    val simpleClassName = a.getClass.getSimpleName.toCharArray
    new String(simpleClassName.head.toLower +: simpleClassName.tail)
  }

  private[this] def jsonify: PartialFunction[Any, JObject] = {
    case WorkLog(totalWork, totalLoitering) =>
      ("totalWork" -> totalWork) ~ ("totalLoitering", totalLoitering)
  }

  private[this] def sendTo(clientUuid: String, msg: String): Unit = {
    val resourceFactory = Option(AtmosphereResourceFactory.getDefault.find(clientUuid))
    resourceFactory.map(_.getBroadcaster.broadcast(msg))
  }
}
