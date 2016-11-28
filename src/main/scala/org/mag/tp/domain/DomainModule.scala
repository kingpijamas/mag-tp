package org.mag.tp.domain

import akka.actor.{ActorRef, ActorSystem, Props}
import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{@@, Tagger}
import org.mag.tp.domain.Employee._
import org.mag.tp.domain.WorkArea.{Broadcastability, EmployeeCount}
import org.mag.tp.util.ProbabilityBag

import scala.concurrent.duration.DurationDouble
import scala.language.postfixOps
import scala.util.Random

trait DomainModule {
  val memory = None.taggedWith[MemorySize]
  val employeeTimerFreq = (0.1 seconds).taggedWith[Employee.TimerFreq]

  val targetEmployeeCount = 1000.taggedWith[EmployeeCount]
  val broadcastability = 5.taggedWith[Broadcastability]

  val workingProportion = 0.50

  def employeePropsFactory(workArea: ActorRef @@ WorkArea): Props @@ Employee = {
    def permeabilityAndBehaviours(permeability: Double, behaviourProbs: (Behaviour, Double)*) =
      (permeability.taggedWith[Permeability], ProbabilityBag.complete[Employee.Behaviour](behaviourProbs: _*))

    val (permeability, behaviour) = if (Random.nextDouble < workingProportion)
      permeabilityAndBehaviours(0.01, WorkBehaviour -> 0, LoiterBehaviour -> 1)
    else
      permeabilityAndBehaviours(0, WorkBehaviour -> 1, LoiterBehaviour -> 0)

    Props(wire[Employee]).taggedWith[Employee]
  }

  def workAreaPropsFactory(mandatoryBroadcastables: Traversable[ActorRef]): Props @@ WorkArea = {
    val employeeProps = employeePropsFactory _
    Props(wire[WorkArea]).taggedWith[WorkArea]
  }

  def system: ActorSystem
}
