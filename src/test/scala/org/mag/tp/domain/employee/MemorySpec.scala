package org.mag.tp.domain.employee

import akka.actor.ActorRef
import com.softwaremill.macwire.wire
import org.mag.tp.domain.WorkArea.{ActionType, Loiter, Work}
import org.mag.tp.util.ProbabilityBag
import org.mag.tp.{ActorSpec, UnitSpec}

import scala.language.postfixOps

class MemorySpec extends UnitSpec with ActorSpec {
  trait Permeability
  trait WorkingProb

  class MemoryTest(implicit maxMemories: Option[Int] = None) {
    val subject: Memory = wire[Memory]

    def rememberWork(employee: ActorRef = testRef(),
                     groupId: String = "testGroup",
                     times: Int = 1): Unit = {
      (0 until times).foreach { _ =>
        subject.remember(Work(employee, group(groupId)))
      }
    }

    def rememberLoitering(employee: ActorRef = testRef(),
                          groupId: String = "testGroup",
                          times: Int = 1): Unit = {
      (0 until times).foreach { _ =>
        subject.remember(Loiter(employee, group(groupId)))
      }
    }

    def group(id: String): Group = Group(
      id = id,
      targetSize = 100,
      permeability = 0.5,
      maxMemories = maxMemories,
      baseBehaviours = ProbabilityBag.complete[Behaviour](WorkBehaviour -> 0.5, LoiterBehaviour -> 0.5)
    )

    def rememberedActionTypes: Traversable[ActionType] = subject.rememberedActions map (_.getClass)

    def rememberedActionsCount: (ActionType => Int) = subject.rememberedActionCountsByType

    def rememberedActionsCountByEmployee: (ActorRef => Int) = subject.rememberedActionCountsByEmployee

    def knownEmployees: Traversable[ActorRef] = subject.knownEmployees
  }

  "A Memory" when {
    "given an Action to remember" should {
      "remember it" in new MemoryTest {
        val anEmployee = testRef()
        rememberWork(employee = anEmployee)

        rememberedActionTypes should contain(classOf[Work])
        rememberedActionsCount(classOf[Work]) should be(1)
        rememberedActionsCountByEmployee(anEmployee) should be(1)
        knownEmployees should contain(anEmployee)
      }
    }
  }

  "A limited Memory" when {
    implicit val maxMemories = Some(3)

    "given an Action to remember and full" should {
      "forget the earliest Action it remembers" in new MemoryTest {
        val employeeToForget = testRef()
        rememberLoitering(employee = employeeToForget)
        rememberWork(times = maxMemories.get)

        rememberedActionsCount(classOf[Loiter]) should be(0)
        rememberedActionsCount(classOf[Work]) should be(maxMemories.get)
        knownEmployees should not contain(employeeToForget)
        rememberedActionsCountByEmployee(employeeToForget) should be(0)
      }
    }

    "given an Action to remember and not full" should {
      "not forget the earliest Action it remembers" in new MemoryTest {
        rememberLoitering()
        rememberWork(times = maxMemories.get - 1)

        rememberedActionsCount(classOf[Loiter]) should be(1)
        rememberedActionsCount(classOf[Work]) should be(maxMemories.get - 1)
      }
    }
  }

  "An unlimited Memory" when {
    implicit val maxMemories: Option[Int] = None

    "given an Action to remember" should {
      "not forget the earliest Action it remembers" in new MemoryTest {
        rememberLoitering()
        rememberWork(times = 3)

        rememberedActionsCount(classOf[Loiter]) should be(1)
        rememberedActionsCount(classOf[Work]) should be(3)
      }
    }
  }
}
