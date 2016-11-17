package org.mag.tp.controller

import akka.actor._
import akka.actor.Actor
import akka.actor.Actor._
import com.softwaremill.tagging.@@
import org.json4s.{DefaultFormats, Formats}
import org.mag.tp.MagTpStack
import org.mag.tp.ui.FrontendActor
import org.mag.tp.ui.FrontendActor._
import org.scalatra.SessionSupport
import org.scalatra.atmosphere.{AtmosphereClient, AtmosphereSupport, Connected, JsonMessage}
import org.scalatra.json.{JValueResult, JacksonJsonSupport}

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
      def receive: Receive = {
        case Connected => // ignore
        // case Disconnected(disconnector, Some(error)) =>
        // case Error(Some(error))                      =>
        // case TextMessage(text)                       =>
        case JsonMessage(json) =>
          frontendActor ! Connection(uuid)

        case msg: Any => // log unhandled messages
          println(msg)
      }
    }
  }
}