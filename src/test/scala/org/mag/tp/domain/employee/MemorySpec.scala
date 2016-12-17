package org.mag.tp.domain.employee

import akka.actor.ActorRef
import akka.testkit.TestProbe
import com.softwaremill.macwire.wire
import com.softwaremill.tagging.Tagger
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

    def rememberedActionsCount(typ: ActionType): Int = subject.rememberedActionCountsByType(typ)
  }

  "A Memory" when {
    "witnessing an Action" should {
      "remember it" in new MemoryTest {
        rememberWork()
        rememberedActionTypes should contain(classOf[Work])
        rememberedActionsCount(classOf[Work]) should be(1)
        subject.rememberedActionCountsByEmployee.values should contain(1)
      }
    }
  }
}
