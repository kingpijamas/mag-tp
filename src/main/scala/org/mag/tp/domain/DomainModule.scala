package org.mag.tp.domain

import akka.actor.{ActorRef, ActorSystem, Props}
import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{@@, Tagger}
import org.mag.tp.domain.Employee._
import org.mag.tp.domain.WorkArea.{Broadcastability, EmployeeCount}
import org.mag.tp.util.ProbabilityBag

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

class DomainModule(val system: ActorSystem,
                   val employeeTimerFreq: FiniteDuration,
                   val workingEmployeesCount: Int,
                   val loiteringEmployeesCount: Int,
                   val memory: Option[Int],
                   val broadcastability: Int,
                   val workersPermeabilityAtStart: Double,
                   val loiterersPermeabilityAtStart: Double) {
  val _employeeTimerFreq = employeeTimerFreq.taggedWith[Employee.TimerFreq]
  val _memory = memory.taggedWith[MemorySize]
  val targetEmployeeCount = (workingEmployeesCount + loiteringEmployeesCount).taggedWith[EmployeeCount]
  val _broadcastability = broadcastability.taggedWith[Broadcastability]

  // XXX
  private var workingEmployeesToCreate = workingEmployeesCount
  private var loiteringEmployeesToCreate = loiteringEmployeesCount

  def employeePropsFactory(workArea: ActorRef @@ WorkArea): Props @@ Employee = {
    def permeabilityAndBehaviours(permeability: Double, behaviourProbs: (Behaviour, Double)*) =
      (permeability.taggedWith[Permeability], ProbabilityBag.complete[Employee.Behaviour](behaviourProbs: _*))

    val (permeability, behaviour) = if (workingEmployeesToCreate > 0) {
      workingEmployeesToCreate -= 1 // XXX
      permeabilityAndBehaviours(workersPermeabilityAtStart, WorkBehaviour -> 1, LoiterBehaviour -> 0)
    } else {
      loiteringEmployeesToCreate -= 1 // XXX
      permeabilityAndBehaviours(loiterersPermeabilityAtStart, WorkBehaviour -> 0, LoiterBehaviour -> 1)
    }

    Props(wire[Employee]).taggedWith[Employee]
  }

  def workAreaPropsFactory(mandatoryBroadcastables: Traversable[ActorRef]): Props @@ WorkArea = {
    val employeeProps = employeePropsFactory _
    Props(wire[WorkArea]).taggedWith[WorkArea]
  }

  def reset(): Unit = { // XXX
    workingEmployeesToCreate = workingEmployeesCount
    loiteringEmployeesToCreate = loiteringEmployeesCount
  }
}
