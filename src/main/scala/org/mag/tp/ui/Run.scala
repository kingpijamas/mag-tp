package org.mag.tp.ui

import akka.actor.ActorSystem
import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{@@, Tagger}
import org.mag.tp.domain.employee.{Employee, LoiterBehaviour, WorkBehaviour}
import org.mag.tp.domain.{DomainModule, employee}
import org.mag.tp.util.ProbabilityBag

import scala.collection.immutable
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.language.postfixOps

object Run {
  def apply(system: ActorSystem, params: Map[String, String]): Run = {
    def getOptionalInt(key: String) = params.get(key) map (_.toInt)

    def getInt(key: String, defaultValue: Int) = getOptionalInt(key) getOrElse (defaultValue)

    def getDouble(key: String, defaultValue: Double) = params.get(key) map (_.toDouble) getOrElse (defaultValue)

    def getFreq[T](key: String, defaultValue: Double) = Some(getDouble(key, defaultValue).seconds).taggedWith[T]

    val employeesMemory = getOptionalInt("employeesMemory")
    val visibility = getInt("visibility", defaultValue = 5)

    val backendTimerFreq = getFreq[Employee]("backendTimerFreq", defaultValue = 0.2)
    val loggingTimerFreq = getFreq[StatsLogger]("loggingTimerFreq", defaultValue = 0.7)

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

// FIXME turn into case class!
class Run(val system: ActorSystem,
          val employeeGroups: immutable.Seq[employee.Group],
          val employeeTimerFreq: Option[FiniteDuration] @@ Employee,
          val visibility: Int,
          val statsLoggerTimerFreq: Option[FiniteDuration] @@ StatsLogger)
  extends DomainModule with FrontendModule
