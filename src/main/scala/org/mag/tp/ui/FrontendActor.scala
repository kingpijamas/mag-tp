package org.mag.tp.ui

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import org.atmosphere.cpr.AtmosphereResourceFactory

import akka.actor.Actor
import scala.collection.mutable

object FrontendActor {
  case class Connection(clientUuid: String)

  case class WorkLog(totalWork: Int, totalLoitering: Int) {
    override def toString: String =
      s"WorkLog(totalWork=$totalWork, totalLoitering=$totalLoitering)"
  }
}

class FrontendActor extends Actor {
  import FrontendActor._

  var connectedClientUuids = mutable.Buffer[String]()

  def receive: Receive = {
    case Connection(clientUuid: String) =>
      connectedClientUuids += clientUuid
      sendTo(clientUuid, "Welcome!")

    case msg: WorkLog =>
      val jsonifiedMsg = jsonify(msg)
      val compactJsonifiedMsg = compact(render(jsonifiedMsg))
      connectedClientUuids.foreach { sendTo(_, compactJsonifiedMsg) }

    case _ => // ignore unknown messages      
  }

  private[this] def jsonify: PartialFunction[Any, JValue] = {
    case WorkLog(totalWork, totalLoitering) =>
      ("totalWork" -> totalWork) ~ ("totalLoitering", totalLoitering)
  }

  private[this] def sendTo(clientUuid: String, msg: String): Unit = {
    AtmosphereResourceFactory.getDefault.find(clientUuid).getBroadcaster.broadcast(msg)
  }
}
