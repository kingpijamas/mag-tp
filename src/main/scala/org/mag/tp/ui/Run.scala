package org.mag.tp.ui

import akka.actor.ActorSystem
import com.softwaremill.tagging.{@@, Tagger}
import org.mag.tp.controller.UIController.{GroupParams, RunParams}
import org.mag.tp.domain.employee.{WorkBehaviour, _}
import org.mag.tp.domain.{DomainModule, employee}
import org.mag.tp.util.ProbabilityBag

import scala.collection.immutable
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.language.postfixOps

object Run {
  val DefaultVisibility = 5

  val DefaultTargetSize = 0
  val DefaultPermeability = 0.5
  val DefaultWorkProbability = 0.5
  val DefaultLoiteringProbability = 0.5

  val DefaultBackendTimerFreq = 0.2 seconds
  val DefaultFrontendTimerFreq = 0.7 seconds

  def apply(system: ActorSystem, params: RunParams): Run = {
    // FIXME: apparently Scalatra has validators/converters of its own, change this!
    def asInt(param: Option[String], default: Int): Int =
      param map (_.toInt) getOrElse (default)

    def asDouble(param: Option[String], default: Double): Double =
      param map (_.toDouble) getOrElse (default)

    def asDuration(param: Option[String], default: FiniteDuration): FiniteDuration =
      param map (_.toDouble.seconds) getOrElse (default)

    val maxMemories = params.employeesMemory map (_.toInt)

    val groups = params.groups map {
      case GroupParams(name, count, workProbability, loiteringProbability, permeability) =>
        val behaviours = ProbabilityBag.complete[Behaviour](
          WorkBehaviour -> asDouble(workProbability, DefaultWorkProbability),
          LoiterBehaviour -> asDouble(loiteringProbability, DefaultLoiteringProbability)
        )

        Group(id = name,
          targetSize = asInt(count, DefaultTargetSize),
          permeability = asDouble(permeability, DefaultPermeability),
          maxMemories = maxMemories,
          baseBehaviours = behaviours)
    }

    val visibility = asInt(params.visibility, default = DefaultVisibility)

    val employeeTimerFreq = asDuration(params.backendTimerFreq, DefaultBackendTimerFreq)
    val statsLoggerTimerFreq = asDuration(params.loggingTimerFreq, DefaultFrontendTimerFreq)

    new Run(
      system = system,
      employeeGroups = groups,
      visibility = visibility,
      employeeTimerFreq = Some(employeeTimerFreq).taggedWith[Employee],
      statsLoggerTimerFreq = Some(statsLoggerTimerFreq).taggedWith[StatsLogger]
    )
  }
}

// FIXME turn into case class!
class Run(val system: ActorSystem,
          val employeeGroups: immutable.Seq[employee.Group],
          val employeeTimerFreq: Option[FiniteDuration] @@ Employee,
          val visibility: Int,
          val statsLoggerTimerFreq: Option[FiniteDuration] @@ StatsLogger)
  extends DomainModule with FrontendModule {

  val employeesCount: Int = employeeGroups map (_.targetSize) sum
  val groupNames: Traversable[String] = employeeGroups map (_.id)
}
