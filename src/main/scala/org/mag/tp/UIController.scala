package org.mag.tp

import com.softwaremill.tagging._
import org.json4s.DefaultFormats
import org.json4s.Formats
import org.scalatra.ScalatraServlet
import org.scalatra.SessionSupport
import org.scalatra.atmosphere.AtmosphereClient
import org.scalatra.atmosphere.AtmosphereSupport
import org.scalatra.atmosphere.Connected
import org.scalatra.atmosphere.Disconnected
import org.scalatra.atmosphere.Error
import org.scalatra.atmosphere.JsonMessage
import org.scalatra.atmosphere.TextMessage
import org.scalatra.json.JValueResult
import org.scalatra.json.JacksonJsonSupport

import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import org.mag.tp.ui.FrontendActor
import org.mag.tp.ui.FrontendActor._

class UIController(frontendActor: ActorRef @@ FrontendActor) extends ScalatraServlet
    with JValueResult
    with JacksonJsonSupport
    with SessionSupport
    with AtmosphereSupport {

  implicit protected val jsonFormats: Formats = DefaultFormats

  atmosphere("/") {
    new AtmosphereClient {
      def receive = {
        case Connected => // ignore
        // case Disconnected(disconnector, Some(error)) =>
        // case Error(Some(error))                      =>
        // case TextMessage(text)                       =>
        case JsonMessage(json) =>
          frontendActor ! Connection(uuid)

        case msg =>
          println(msg)

      }
    }
  }
}
