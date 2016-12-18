package org.mag.tp.domain.employee

import akka.actor.{ActorRef, Props}
import akka.testkit.TestActorRef
import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{@@, Tagger}
import org.mag.tp.domain.WorkArea
import org.mag.tp.domain.WorkArea.{ActionType, Loiter, Work}
import org.mag.tp.domain.employee.Employee.Act
import org.mag.tp.util.ProbabilityBag
import org.mag.tp.{ActorSpec, DomainMocks, UnitSpec}

import scala.language.postfixOps

class EmployeeSpec extends UnitSpec with ActorSpec with DomainMocks {
  trait Permeability
  trait WorkingProb

  class EmployeeTest(implicit maxMemories: Option[Int] = None,
                     permeability: Double @@ Permeability = 0.5.taggedWith[Permeability],
                     val originalWorkingProb: Double @@ WorkingProb = 0.5.taggedWith[WorkingProb]) {
    val timerFreq = dummyTime.taggedWith[Employee]

    val behaviours = ProbabilityBag.complete[Behaviour](
      WorkBehaviour -> originalWorkingProb,
      LoiterBehaviour -> (1 - originalWorkingProb)
    )

    val group = Group(
      id = "testGroup",
      targetSize = 100,
      permeability = permeability,
      maxMemories = maxMemories,
      baseBehaviours = behaviours
    )

    val workAreaRef = testRef[WorkArea]

    val subjectRef: TestActorRef[Employee] = TestActorRef.create(system, Props(wire[Employee]))
    val subject: Employee = subjectRef.underlyingActor

    def rememberedActionTypes: Traversable[ActionType] = subject.memory.rememberedActions map (_.getClass)

    def rememberedActionsCount: (ActionType => Int) = subject.memory.rememberedActionCountsByType

    def workingProbability: Double = subject._behaviours(WorkBehaviour)

    def loiteringProbability: Double = subject._behaviours(LoiterBehaviour)

    def influenceSubjectToWork(employee: ActorRef = testRef[Employee],
                               groupId: String = "anotherTestGroup",
                               times: Int = 1): Unit = {
      (0 until times).foreach { _ =>
        subjectRef ! Work(employee, group(groupId))
      }
      subjectRef ! Act
    }

    def influenceSubjectToLoiter(employee: ActorRef = testRef[Employee],
                                 groupId: String = "anotherTestGroup",
                                 times: Int = 1): Unit = {
      (0 until times).foreach { _ =>
        subjectRef ! Loiter(employee, group(groupId))
      }
      subjectRef ! Act
    }

    private[this] def group(id: String): Group = group.copy(id = id)
  }

  "An Employee" when {
    "witnessing an Action" should {
      "remember it" in new EmployeeTest {
        influenceSubjectToWork()

        rememberedActionTypes should contain(classOf[Work])
        rememberedActionsCount(classOf[Work]) should be(1)
        subject.memory.rememberedActionCountsByEmployee.values should contain(1)
      }
    }
  }

  "An impermeable Employee" when {
    implicit val permeability = 0D.taggedWith[Permeability]

    "influenced to loiter" should {
      "maintain its tendencies unchanged" in {
        implicit val workingProb = 0D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceSubjectToLoiter()

          workingProbability should be(0)
          loiteringProbability should be(1)
        }
      }
    }

    "influenced to work" should {
      "maintain its tendencies unchanged" in {
        implicit val workingProb = 1D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceSubjectToWork()

          workingProbability should be(1)
          loiteringProbability should be(0)
        }
      }
    }
  }

  "A positively-permeable Employee" when {
    implicit val permeability = 0.7.taggedWith[Permeability]

    "the majority is loitering" should {
      "reduce its tendency to work" in new EmployeeTest {
        influenceSubjectToLoiter(times = 200)
        influenceSubjectToWork(times = 50)

        workingProbability should be < originalWorkingProb.asInstanceOf[Double]
        loiteringProbability should be > (1 - originalWorkingProb.asInstanceOf[Double])
      }
    }

    "the majority is working" should {
      "increase its tendency to work" in new EmployeeTest {
        influenceSubjectToLoiter(times = 50)
        influenceSubjectToWork(times = 200)

        workingProbability should be > originalWorkingProb.asInstanceOf[Double]
        loiteringProbability should be < (1 - originalWorkingProb.asInstanceOf[Double])
      }
    }
  }

  "A fully positively-permeable Employee" when {
    implicit val permeability = 1D.taggedWith[Permeability]

    "influenced to loiter" should {
      "entirely drop its tendency to work" in {
        implicit val workingProb = 0D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceSubjectToLoiter()

          workingProbability should be(0)
          loiteringProbability should be(1)
        }
      }
    }

    "influenced to work" should {
      "entirely drop its tendency to loiter" in {
        implicit val workingProb = 1D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceSubjectToWork()

          workingProbability should be(1)
          loiteringProbability should be(0)
        }
      }
    }
  }

  "A negatively-permeable Employee" when {
    implicit val permeability = -0.7.taggedWith[Permeability]

    "the majority is loitering" should {
      "increase its tendency to work" in new EmployeeTest {
        influenceSubjectToLoiter(times = 200)
        influenceSubjectToWork(times = 50)

        workingProbability should be > originalWorkingProb.asInstanceOf[Double]
        loiteringProbability should be < (1 - originalWorkingProb.asInstanceOf[Double])
      }
    }

    "the majority is working" should {
      "reduce its tendency to work" in new EmployeeTest {
        influenceSubjectToLoiter(times = 50)
        influenceSubjectToWork(times = 200)

        workingProbability should be < originalWorkingProb.asInstanceOf[Double]
        loiteringProbability should be > (1 - originalWorkingProb.asInstanceOf[Double])
      }
    }
  }

  "A fully negatively-permeable Employee" when {
    implicit val permeability = -1D.taggedWith[Permeability]

    "influenced to loiter" should {
      "entirely drop its tendency to loiter" in {
        implicit val workingProb = 0D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceSubjectToLoiter()

          workingProbability should be(1)
          loiteringProbability should be(0)
        }
      }
    }

    "influenced to work" should {
      "entirely drop its tendency to work" in {
        implicit val workingProb = 1D.taggedWith[WorkingProb]
        new EmployeeTest {
          influenceSubjectToWork()

          workingProbability should be(0)
          loiteringProbability should be(1)
        }
      }
    }
  }
}
