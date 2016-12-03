package org.mag.tp.controller

import akka.actor.Actor._
import akka.actor.{ActorRef, ActorSystem}
import com.softwaremill.macwire.wire
import com.softwaremill.tagging._
import org.json4s.{DefaultFormats, Formats}
import org.mag.tp.MagTpStack
import org.mag.tp.domain.Employee.{LoiterBehaviour, WorkBehaviour}
import org.mag.tp.domain.{DomainModule, Employee}
import org.mag.tp.ui.FrontendActor.{Connection, SimulationStep, StartSimulation}
import org.mag.tp.ui.{FrontendModule, StatsLogger}
import org.mag.tp.util.PausableActor.{Pause, Resume}
import org.mag.tp.util.ProbabilityBag
import org.scalatra.SessionSupport
import org.scalatra.atmosphere.{AtmosphereClient, AtmosphereSupport, Disconnected, Error, JsonMessage}
import org.scalatra.json.{JValueResult, JacksonJsonSupport}

import scala.collection.{immutable, mutable}
import scala.concurrent.duration._

object UIController {
  object Run {
    def apply(system: ActorSystem, params: Map[String, String]): Run = {
      val cleanParams = params filter { case (_, value) => !value.isEmpty }

      def getOptionalInt(key: String) = cleanParams.get(key).map(_.toInt)
      def getInt(key: String, defaultValue: Int) = getOptionalInt(key).getOrElse(defaultValue)
      def getDouble(key: String, defaultValue: Double) = cleanParams.get(key).map(_.toDouble).getOrElse(defaultValue)

      val employeesMemory = getOptionalInt("employeesMemory")
      val broadcastability = getInt("broadcastability", defaultValue = 5)

      val backendTimerFreq = getDouble("backendTimerFreq", defaultValue = 0.2).seconds.taggedWith[Employee.TimerFreq]
      val loggingTimerFreq = getDouble("loggingTimerFreq", defaultValue = 0.7).seconds.taggedWith[StatsLogger.TimerFreq]

      val workingGroup = Employee.Group(
        id = 1,
        targetSize = getInt("workersCount", defaultValue = 500),
        permeability = getDouble("workersPermeability", defaultValue = 0.5),
        maxMemories = employeesMemory,
        baseBehaviours = ProbabilityBag.complete[Employee.Behaviour](WorkBehaviour -> 1, LoiterBehaviour -> 0)
      )
      val loiteringGroup = Employee.Group(
        id = 2,
        targetSize = getInt("loiterersCount", defaultValue = 500),
        permeability = getDouble("loiterersPermeability", defaultValue = 0),
        maxMemories = employeesMemory,
        baseBehaviours = ProbabilityBag.complete[Employee.Behaviour](WorkBehaviour -> 0, LoiterBehaviour -> 1)
      )

      val groups = immutable.Seq(workingGroup, loiteringGroup)

      wire[Run]
    }
  }

  class Run(val system: ActorSystem,
            val employeeGroups: immutable.Seq[Employee.Group],
            val employeeTimerFreq: FiniteDuration @@ Employee.TimerFreq,
            val visibility: Int,
            val statsLoggerTimerFreq: FiniteDuration @@ StatsLogger.TimerFreq)
    extends DomainModule with FrontendModule
}

class UIController(system: ActorSystem) extends MagTpStack
  with JValueResult
  with JacksonJsonSupport
  with SessionSupport
  with AtmosphereSupport {

  import UIController._

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
    currentRun = Some(Run(system, params))
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
        case (Disconnected | Error) =>
          clientUuids -= uuid

        case JsonMessage(_) =>
          clientUuids += uuid
          frontendActor.foreach(_ ! Connection(uuid))

        case msg: Any => // log unhandled messages
          println(msg)
      }
    }
  }
}
