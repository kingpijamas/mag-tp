package org.mag.tp.controller

import akka.actor.Actor._
import akka.actor._
import org.json4s.{DefaultFormats, Formats}
import org.mag.tp.MagTpStack
import org.mag.tp.ui.FrontendActor._
import org.mag.tp.ui.FrontendModule
import org.mag.tp.util.PausableActor.{Pause, Resume}
import org.scalatra.SessionSupport
import org.scalatra.atmosphere.{AtmosphereClient, AtmosphereSupport, Connected, JsonMessage}
import org.scalatra.json.{JValueResult, JacksonJsonSupport}

import scala.concurrent.duration._

class UIController(system: ActorSystem) extends MagTpStack
  with JValueResult
  with JacksonJsonSupport
  with SessionSupport
  with AtmosphereSupport {

  implicit protected val jsonFormats: Formats = DefaultFormats // XXX move!

  private var frontendActor: Option[ActorRef] = None

  private[this] def resetFrontendActor(): Unit = {
    frontendActor.foreach(system.stop)
    //  val _employeeTimerFreq = (0.1 seconds).taggedWith[Employee.TimerFreq]
    //  val targetEmployeeCount = 1000.taggedWith[EmployeeCount]
    //  val _memory = Some(1).taggedWith[MemorySize]
    //  val _broadcastability = 5.taggedWith[Broadcastability]
    //  val workersPermeabilityAtStart = -0.3
    //  val workingProportionAtStart = 0.90
    //  val _statsLoggerTimerFreq = (0.5 seconds).taggedWith[StatsLogger.TimerFreq]
    val frontendModule = new FrontendModule(system,
      employeeTimerFreq = 0.2 seconds,
      workingEmployeesCount = 500,
      loiteringEmployeesCount = 500,
      memory = Some(1),
      broadcastability = 5,
      workersPermeabilityAtStart = 0.5,
      loiterersPermeabilityAtStart = 0,
      statsLoggerTimerFreq = 0.7 seconds)
    frontendActor = Some(frontendModule.createFrontendActor())
  }

  get("/") {
    contentType = "text/html"
    jade("main.jade")
  }

  post("/restart") {
    resetFrontendActor()
    frontendActor.foreach(_ ! StartSimulation)
  }

  post("/stop") {
    frontendActor.foreach(_ ! StopSimulation)
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
        case Connected => // ignore
        // case Disconnected(disconnector, Some(error)) =>
        // case Error(Some(error))                      =>
        // case TextMessage(text)                       =>
        case JsonMessage(_) =>
          frontendActor.foreach(_ ! Connection(uuid))

        case msg: Any => // log unhandled messages
          println(msg)
      }
    }
  }
}
