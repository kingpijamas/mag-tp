package org.mag.tp

import java.util.Date
import org.scalatra.json.JValueResult
import org.json4s.Formats
import org.scalatra.ScalatraServlet
import org.scalatra.atmosphere.JsonMessage
import org.scalatra.atmosphere.AtmosphereSupport
import org.scalatra.atmosphere.AtmosphereClient
import org.scalatra.atmosphere.TextMessage
import akka.actor.ActorRef
import org.json4s.DefaultFormats
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.atmosphere.Connected
import org.scalatra.atmosphere.Disconnected
import org.scalatra.SessionSupport

class AkkaServlet(frontendActor: ActorRef) extends ScalatraServlet
    with JValueResult
    with JacksonJsonSupport
    with SessionSupport
    with AtmosphereSupport {

  implicit protected val jsonFormats: Formats = DefaultFormats

  atmosphere("/akka") {
    new AtmosphereClient {
      def receive = {
        case Connected                               =>
        case Disconnected(disconnector, Some(error)) =>
        // case Error(Some(error))                   =>
        case TextMessage(text)                       =>
        case JsonMessage(json) =>
          frontendActor ! (uuid, json.toString)
      }
    }
  }
}
