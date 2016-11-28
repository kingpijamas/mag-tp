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
                     val originalWorkingProb: Double @@ WorkingProb = 0.5.taggedWith[WorkingProb]) {
    val _maxMemories = maxMemories.taggedWith[Employee.MemorySize]
    val _timerFreq = (-1 seconds).taggedWith[TimerFreq]
    val behaviours = ProbabilityBag.complete[Employee.Behaviour](
      WorkBehaviour -> originalWorkingProb,
      LoiterBehaviour -> (1 - originalWorkingProb)
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
      "remember it" in new EmployeeTest {
        subjectRef ! Work
        rememberedActions should contain(Work)
      }
    }
  }

  "An impermeable Employee" when {
    implicit val permeability = 0D.taggedWith[Permeability]

    "influenced to loiter" should {
      "maintain its tendencies unchanged" in {
        implicit val workingProb = 0D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceTo(Loiter)

          workingProbability should be(0)
          loiteringProbability should be(1)
        }
      }
    }

    "influenced to work" should {
      "maintain its tendencies unchanged" in {
        implicit val workingProb = 1D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceTo(Work)

          workingProbability should be(1)
          loiteringProbability should be(0)
        }
      }
    }
  }

  "A positively-permeable Employee" when {
    "the majority is loitering" should {
      "reduce its tendency to work" in {
        implicit val permeability = 0.7.taggedWith[Permeability]
        new EmployeeTest {
          influenceTo(Loiter, times = 55)
          influenceTo(Work, times = 45)

          workingProbability should be < originalWorkingProb.asInstanceOf[Double]
          loiteringProbability should be > (1 - originalWorkingProb.asInstanceOf[Double])
        }
      }
    }

    "the majority is working" should {
      "increase its tendency to work" in {
        implicit val permeability = 0.7.taggedWith[Permeability]
        new EmployeeTest {
          influenceTo(Loiter, times = 45)
          influenceTo(Work, times = 55)

          workingProbability should be > originalWorkingProb.asInstanceOf[Double]
          loiteringProbability should be < (1 - originalWorkingProb.asInstanceOf[Double])
        }
      }
    }
  }

  "A fully positively-permeable Employee" when {
    implicit val permeability = 1D.taggedWith[Permeability]

    "influenced to loiter" should {
      "entirely drop its tendency to work" in {
        implicit val workingProb = 0D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceTo(Loiter)

          workingProbability should be(0)
          loiteringProbability should be(1)
        }
      }
    }

    "influenced to work" should {
      "entirely drop its tendency to loiter" in {
        implicit val workingProb = 1D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceTo(Work)

          workingProbability should be(1)
          loiteringProbability should be(0)
        }
      }
    }
  }

  "A negatively-permeable Employee" when {
    "the majority is loitering" should {
      "increase its tendency to work" in {
        implicit val permeability = -0.7.taggedWith[Permeability]
        new EmployeeTest {
          influenceTo(Loiter, times = 55)
          influenceTo(Work, times = 45)

          workingProbability should be > originalWorkingProb.asInstanceOf[Double]
          loiteringProbability should be < (1 - originalWorkingProb.asInstanceOf[Double])
        }
      }
    }

    "the majority is working" should {
      "reduce its tendency to work" in {
        implicit val permeability = -0.7.taggedWith[Permeability]
        new EmployeeTest {
          influenceTo(Loiter, times = 45)
          influenceTo(Work, times = 55)

          workingProbability should be < originalWorkingProb.asInstanceOf[Double]
          loiteringProbability should be > (1 - originalWorkingProb.asInstanceOf[Double])
        }
      }
    }
  }

  "A fully negatively-permeable Employee" when {
    implicit val permeability = -1D.taggedWith[Permeability]

    "influenced to loiter" should {
      "entirely drop its tendency to loiter" in {
        implicit val workingProb = 0D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceTo(Loiter)

          workingProbability should be(1)
          loiteringProbability should be(0)
        }
      }
    }

    "influenced to work" should {
      "entirely drop its tendency to work" in {
        implicit val workingProb = 1D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceTo(Work)

          workingProbability should be(0)
          loiteringProbability should be(1)
        }
      }
    }
  }
}
