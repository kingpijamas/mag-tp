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
import scala.collection.mutable
import scala.concurrent.duration._

class UIController(system: ActorSystem) extends MagTpStack
  with JValueResult
  with JacksonJsonSupport
  with SessionSupport
  with AtmosphereSupport {

  implicit protected val jsonFormats: Formats = DefaultFormats // XXX move!

  private var frontendModule: Option[FrontendModule] = None
  private var frontendActor: Option[ActorRef] = None
  private var clientUuids = mutable.Buffer[String]()

  get("/") {
    contentType = "text/html"

    frontendActor.foreach(system.stop)

    jade("setup.jade")
  }

  post("/simulation") {
    contentType = "text/html"

    val workersCount: Int = params.get("workersCount").map(_.toInt).getOrElse(500)
    val loiterersCount: Int = params.get("loiterersCount").map(_.toInt).getOrElse(500)
    val employeesMemory: Option[Int] = params.get("employeesMemory") match {
      case None => None
      case Some(str) if str.isEmpty => None
      case Some(str) => Some(str.toInt)
    }
    val broadcastability: Int = params.get("broadcastability").map(_.toInt).getOrElse(5)
    val workersPermeability: Double = params.get("workersPermeability").map(_.toDouble).getOrElse(0.5)
    val loiterersPermeability: Double = params.get("loiterersPermeability").map(_.toDouble).getOrElse(0)
    val backendTimerFreq: Double = params.get("backendTimerFreq").map(_.toDouble).getOrElse(0.2)
    val loggingTimerFreq: Double = params.get("loggingTimerFreq").map(_.toDouble).getOrElse(0.7)

    println(params.toMap)

    frontendModule = Some(new FrontendModule(system,
      workingEmployeesCount = workersCount,
      loiteringEmployeesCount = loiterersCount,
      memory = employeesMemory,
      broadcastability = broadcastability,
      workersPermeabilityAtStart = workersPermeability,
      loiterersPermeabilityAtStart = loiterersPermeability,
      statsLoggerTimerFreq = loggingTimerFreq seconds,
      employeeTimerFreq = backendTimerFreq seconds
    ))

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
    frontendActor.foreach(system.stop(_))
    val _frontendActor = frontendModule.get.createFrontendActor()
    frontendActor = Some(_frontendActor)
    clientUuids.foreach(_frontendActor ! Connection(_))
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
          clientUuids += uuid
          frontendActor.foreach(_ ! Connection(uuid))

        case msg: Any => // log unhandled messages
          println(msg)
      }
    }
  }
}
