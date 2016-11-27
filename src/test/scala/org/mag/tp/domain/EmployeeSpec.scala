package org.mag.tp.domain

import akka.actor.Props
import akka.testkit.{TestActorRef, TestProbe}
import com.softwaremill.macwire._
import com.softwaremill.tagging.Tagger
import org.mag.tp.domain.Employee._
import org.mag.tp.util.ProbabilityBag
import org.mag.tp.{ActorSpec, UnitSpec}

import scala.concurrent.duration.DurationDouble
import scala.language.postfixOps

class EmployeeSpec extends UnitSpec with ActorSpec {
  class EmployeeFixture(maxMemories: Option[Int],
                        permeability: Double,
                        workingProb: Double) {
    val _maxMemories = maxMemories.taggedWith[Employee.MemorySize]
    val _permeability = permeability.taggedWith[Employee.Permeability]
    val _timerFreq = (-1 seconds).taggedWith[TimerFreq]
    val behaviours = ProbabilityBag.complete[Employee.Behaviour](
      WorkBehaviour -> workingProb,
      LoiterBehaviour -> (1 - workingProb)
    )

    val workArea = TestProbe()
    val workAreaRef = workArea.ref.taggedWith[WorkArea]

    val props: Props = Props(wire[Employee])
    val subjectRef: TestActorRef[Employee] = TestActorRef.create(system, props, "testEmployee")

    def subject: Employee = subjectRef.underlyingActor
  }

  "An Employee" when {
    "" should {
      "" in new EmployeeFixture(
        maxMemories = None,
        permeability = 0.05,
        workingProb = 1
      ) {
        assert(subject.getClass == classOf[Employee])
      }
    }
  }
}
