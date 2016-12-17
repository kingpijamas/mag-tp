package org.mag.tp.controller

import akka.actor.Actor._
import akka.actor.{ActorRef, ActorSystem}
import org.json4s.{DefaultFormats, Formats}
import org.mag.tp.MagTpStack
import org.mag.tp.ui.FrontendActor.{ClientConnected, SimulationStep, StartSimulation}
import org.mag.tp.ui.Run
import org.mag.tp.util.actor.Pausing.{Pause, Resume}
import org.scalatra.SessionSupport
import org.scalatra.atmosphere.{AtmosphereClient, AtmosphereSupport, Disconnected, Error, JsonMessage}
import org.scalatra.json.{JValueResult, JacksonJsonSupport}

import scala.collection.mutable

class UIController(system: ActorSystem) extends MagTpStack
  with JValueResult
  with JacksonJsonSupport
  with SessionSupport
  with AtmosphereSupport {

  implicit protected val jsonFormats: Formats = DefaultFormats // XXX move!

  private var currentRun: Option[Run] = None
  private var frontendActor: Option[ActorRef] = None
  private var clientUuids = mutable.Buffer[String]()

  get("/") {
    contentType = "text/html"

    frontendActor.foreach(system.stop)

    jade("setup.jade")
  }

  post("/simulation") {
    contentType = "text/html"

    println(params.toMap)
    val cleanParams = params filter { case (_, value) => !value.isEmpty }

    currentRun = Some(Run(system, cleanParams))
    jade("simulation.jade")
  }

  post("/restart") {
    resetFrontendActor()
    frontendActor.foreach(_ ! StartSimulation)
  }

  //  post("/stop") {
  //    frontendActor.foreach(_ ! StopSimulation)
  //    resetFrontendActor()
  //  }

  private[this] def resetFrontendActor(): Unit = {
    // XXX
    frontendActor.foreach(system.stop(_))
    val _frontendActor = currentRun.get.createFrontendActor()
    frontendActor = Some(_frontendActor)
    clientUuids.foreach(_frontendActor ! ClientConnected(_))
  }

  post("/pause") {
    frontendActor.foreach(_ ! Pause)
  }

  post("/resume") {
    frontendActor.foreach(_ ! Resume)
  }

  post("/step") {
    frontendActor.foreach(_ ! SimulationStep)
  }

  atmosphere("/ui") {
    new AtmosphereClient {
      def receive: Receive = {
        case (Disconnected | Error) =>
          clientUuids -= uuid

        case JsonMessage(_) =>
          clientUuids += uuid
          frontendActor.foreach(_ ! ClientConnected(uuid))

        case msg: Any => // log unhandled messages
          println(msg)
      }
    }
  }
}
