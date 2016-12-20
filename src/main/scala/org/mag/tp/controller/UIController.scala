package org.mag.tp.controller

import akka.actor.Actor._
import akka.actor.{ActorRef, ActorSystem}
import org.json4s.{DefaultFormats, Formats}
import org.mag.tp.MagTpStack
import org.mag.tp.controller.UIController.RunParams
import org.mag.tp.ui.FrontendActor.{ClientConnected, SimulationStep, StartSimulation}
import org.mag.tp.ui.Run
import org.mag.tp.util.actor.Pausing.{Pause, Resume}
import org.scalatra.SessionSupport
import org.scalatra.atmosphere.{AtmosphereClient, AtmosphereSupport, Disconnected, Error, JsonMessage}
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.collection.{immutable, mutable}

object UIController {
  case class GroupParams(name: String,
                         count: Option[String],
                         workProbability: Option[String],
                         loiteringProbability: Option[String],
                         permeability: Option[String])

  case class RunParams(employeesMemory: Option[String],
                       visibility: Option[String],
                       groups: immutable.Seq[GroupParams],
                       backendTimerFreq: Option[String],
                       loggingTimerFreq: Option[String])

  implicit protected val jsonFormats: Formats = DefaultFormats

  // XXX move!
  // FIXME: these two shouldn't be necessary!
  implicit val GroupParamsFormatter = jsonFormat5(GroupParams)
  implicit val RunParamsFormatter = jsonFormat5(RunParams)
}

class UIController(system: ActorSystem) extends MagTpStack
  with JValueResult
  with JacksonJsonSupport
  with SessionSupport
  with AtmosphereSupport {

  implicit protected def jsonFormats: Formats = DefaultFormats // XXX move!

  private var currentRun: Option[Run] = None
  private var frontendActor: Option[ActorRef] = None
  private var clientUuids = mutable.Buffer[String]()

  get("/") {
    contentType = "text/html"

    frontendActor.foreach(system.stop)

    jade("setup/index.jade")
  }

  post("/simulation") {
    contentType = "text/html"

    // XXX
    val parameters = params("json").parseJson.convertTo[RunParams]
    currentRun = Some(Run(system, parameters))

    jade("simulation/index.jade")
  }

  post("/restart") {
    // XXX
    frontendActor.foreach(system.stop(_))
    val _frontendActor = currentRun.get.createFrontendActor()
    frontendActor = Some(_frontendActor)
    clientUuids.foreach(_frontendActor ! ClientConnected(_))

    frontendActor.foreach(_ ! StartSimulation)
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
