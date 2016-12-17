package org.mag.tp.ui

import akka.actor.ActorSystem
import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{@@, Tagger}
import org.mag.tp.domain.employee.{Employee, LoiterBehaviour, WorkBehaviour}
import org.mag.tp.domain.{DomainModule, employee}
import org.mag.tp.util.ProbabilityBag

import scala.collection.immutable
import scala.concurrent.duration.{FiniteDuration, _}

object Run {
  def apply(system: ActorSystem, params: Map[String, String]): Run = {
    def getOptionalInt(key: String) = params.get(key) map (_.toInt)

    def getInt(key: String, defaultValue: Int) = getOptionalInt(key) getOrElse (defaultValue)

    def getDouble(key: String, defaultValue: Double) = params.get(key) map (_.toDouble) getOrElse (defaultValue)

    val employeesMemory = getOptionalInt("employeesMemory")
    val visibility = getInt("visibility", defaultValue = 5)

    val backendTimerFreq = getDouble("backendTimerFreq", defaultValue = 0.2).seconds.taggedWith[Employee]
    val loggingTimerFreq = getDouble("loggingTimerFreq", defaultValue = 0.7).seconds.taggedWith[StatsLogger]

    val workingGroup = employee.Group(
      id = "workers",
      targetSize = getInt("workersCount", defaultValue = 500),
      permeability = getDouble("workersPermeability", defaultValue = 0.5),
      maxMemories = employeesMemory,
      baseBehaviours = ProbabilityBag.complete[employee.Behaviour](WorkBehaviour -> 1, LoiterBehaviour -> 0)
    )
    val loiteringGroup = employee.Group(
      id = "loiterers",
      targetSize = getInt("loiterersCount", defaultValue = 500),
      permeability = getDouble("loiterersPermeability", defaultValue = 0),
      maxMemories = employeesMemory,
      baseBehaviours = ProbabilityBag.complete[employee.Behaviour](WorkBehaviour -> 0, LoiterBehaviour -> 1)
    )

    val groups = immutable.Seq(workingGroup, loiteringGroup)

    wire[Run]
  }
}

class Run(val system: ActorSystem,
          val employeeGroups: immutable.Seq[employee.Group],
          val employeeTimerFreq: FiniteDuration @@ Employee,
          val visibility: Int,
          val statsLoggerTimerFreq: FiniteDuration @@ StatsLogger)
  extends DomainModule with FrontendModule
