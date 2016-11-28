package org.mag.tp.domain

import akka.actor.Props
import akka.testkit.{TestActorRef, TestProbe}
import com.softwaremill.macwire._
import com.softwaremill.tagging.{Tagger, _}
import org.mag.tp.domain.Employee._
import org.mag.tp.domain.WorkArea._
import org.mag.tp.util.ProbabilityBag
import org.mag.tp.{ActorSpec, UnitSpec}

import scala.concurrent.duration.DurationDouble
import scala.language.postfixOps

class EmployeeSpec extends UnitSpec with ActorSpec {
  trait WorkingProb

  class EmployeeTest(implicit maxMemories: Option[Int] = None,
                     permeability: Double @@ Permeability = 0.5.taggedWith[Permeability],
                     workingProb: Double @@ WorkingProb = 0.5.taggedWith[WorkingProb]) {
    val _maxMemories = maxMemories.taggedWith[Employee.MemorySize]
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
    def workingProbability = subject.baseBehaviours(WorkBehaviour)
    def loiteringProbability = subject.baseBehaviours(LoiterBehaviour)

    def influenceTo(action: WorkArea.Action, times: Int = 1): Unit = {
      (0 until times).foreach { _ => subjectRef ! action }
      subjectRef ! Act
    }
  }

  "An Employee" when {
    "witnessing an Action" should {
      "register it" in new EmployeeTest {
        subjectRef ! Work
        rememberedActions should contain(Work)
      }
    }
  }

  "An impermeable Employee" when {
    implicit val permeability = 0D.taggedWith[Permeability]

    "acting randomly" should {
      "maintain its tendency to work when influenced to loiter" in {
        implicit val workingProb = 0D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceTo(Loiter, times = 1)
          workingProbability should be(0)
          loiteringProbability should be(1)
        }
      }

      "maintain its to loiter when influenced to work" in {
        implicit val workingProb = 1D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceTo(Work, times = 1)
          workingProbability should be(1)
          loiteringProbability should be(0)
        }
      }
    }
  }

  "A fully positively-permeable Employee" when {
    implicit val permeability = 1D.taggedWith[Permeability]

    "acting randomly" should {
      "entirely drop its tendency to work when influenced to loiter" in {
        implicit val workingProb = 0D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceTo(Loiter, times = 1)
          workingProbability should be(0)
          loiteringProbability should be(1)
        }
      }

      "entirely drop its tendency to loiter when influenced to work" in {
        implicit val workingProb = 1D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceTo(Work, times = 1)
          workingProbability should be(1)
          loiteringProbability should be(0)
        }
      }
    }
  }

  "A fully negatively-permeable Employee" when {
    implicit val permeability = -1D.taggedWith[Permeability]

    "acting randomly" should {
      "entirely drop its tendency to loiter when influenced to loiter" in {
        implicit val workingProb = 0D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceTo(Loiter, times = 1)
          workingProbability should be(1)
          loiteringProbability should be(0)
        }
      }

      "entirely drop its tendency to work when influenced to work" in {
        implicit val workingProb = 1D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceTo(Work, times = 1)
          workingProbability should be(0)
          loiteringProbability should be(1)
        }
      }
    }
  }
}
