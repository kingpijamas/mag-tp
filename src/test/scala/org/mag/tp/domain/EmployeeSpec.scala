package org.mag.tp.domain

import akka.actor.Props
import akka.testkit.{TestActorRef, TestProbe}
import com.softwaremill.macwire._
import com.softwaremill.tagging.Tagger
import org.mag.tp.domain.Employee._
import org.mag.tp.domain.WorkArea._
import org.mag.tp.util.ProbabilityBag
import org.mag.tp.{ActorSpec, UnitSpec}

import scala.concurrent.duration.DurationDouble
import scala.language.postfixOps

class EmployeeSpec extends UnitSpec with ActorSpec {
  class EmployeeTest(maxMemories: Option[Int] = None,
                     permeability: Double = 0.5,
                     workingProb: Double = 0.5) {
    val _maxMemories = maxMemories.taggedWith[Employee.MemorySize]
    val _permeability = permeability.taggedWith[Employee.Permeability]
    val _timerFreq = (-1 seconds).taggedWith[TimerFreq]
    val behaviours = ProbabilityBag.complete[Employee.Behaviour](
      WorkBehaviour -> workingProb,
      LoiterBehaviour -> (1 - workingProb)
    )
    val workArea = TestProbe()
    val workAreaRef = workArea.ref.taggedWith[WorkArea]

    val subjectRef: TestActorRef[Employee] = TestActorRef.create(system, Props(wire[Employee]))
    val subject: Employee = subjectRef.underlyingActor

    def rememberedActions = subject.memoryByEmployee.values.flatMap(_.actions)
  }

  "An Employee" when {
    "witnessing an Action" should {
      "register it" in new EmployeeTest {
        subjectRef ! Work
        rememberedActions should contain(Work)
      }
    }

    "acting randomly" should {
      "completely change its tendency to work when fully permeable and influenced to loiter" in new EmployeeTest(
        permeability = 1, workingProb = 0
      ) {
        subjectRef ! Loiter
        subjectRef ! Act
        subject.baseBehaviours(WorkBehaviour) should be(1)
        subject.baseBehaviours(LoiterBehaviour) should be(0)
      }
    }
  }
}
