package org.mag.tp

import org.json4s.DefaultFormats
import org.json4s.Formats
import org.mag.tp.ui.FrontendActor
import org.mag.tp.ui.FrontendActor._
import org.scalatra.SessionSupport
import org.scalatra.atmosphere.AtmosphereClient
import org.scalatra.atmosphere.AtmosphereSupport
import org.scalatra.atmosphere.Connected
import org.scalatra.atmosphere.JsonMessage
import org.scalatra.json.JValueResult
import org.scalatra.json.JacksonJsonSupport

import com.softwaremill.tagging.{ @@ => @@ }

import akka.actor.ActorRef
import akka.actor.actorRef2Scala

class UIController(frontendActor: ActorRef @@ FrontendActor) extends MagTpStack
    with JValueResult
    with JacksonJsonSupport
    with SessionSupport
    with AtmosphereSupport {

  implicit protected val jsonFormats: Formats = DefaultFormats

  get("/") {
    contentType="text/html"
    jade("main.jade")
  }

  post("/restart") {
    frontendActor ! StartSimulation
  }

  atmosphere("/ui") {
    new AtmosphereClient {
      def receive = {
        case Connected => // ignore
        // case Disconnected(disconnector, Some(error)) =>
        // case Error(Some(error))                      =>
        // case TextMessage(text)                       =>
        case JsonMessage(json) =>
          frontendActor ! Connection(uuid)

        case msg => // log unhandled messages
          println(msg)

      }
    }
  }
}
