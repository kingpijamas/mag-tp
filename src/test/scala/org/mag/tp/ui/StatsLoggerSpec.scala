package org.mag.tp.ui

import akka.actor.{ActorRef, Props}
import akka.testkit.TestActorRef
import com.softwaremill.macwire.wire
import com.softwaremill.tagging.Tagger
import org.mag.tp.domain.WorkArea
import org.mag.tp.domain.WorkArea.{Loiter, Work}
import org.mag.tp.domain.employee.{Employee, Group}
import org.mag.tp.ui.StatsLogger.MultiMap
import org.mag.tp.util.actor.Pausing.Resume
import org.mag.tp.{ActorSpec, DomainMocks, UnitSpec}

import scala.language.postfixOps

class StatsLoggerSpec extends UnitSpec with ActorSpec with DomainMocks {
  class StatsLoggerTest {
    val timerFreq = dummyTime.taggedWith[StatsLogger]
    val (frontendActorRef, frontendActor) = testRefAndProbe[FrontendActor]

    val subjectRef: TestActorRef[StatsLogger] = TestActorRef.create(system, Props(wire[StatsLogger]))
    val subject: StatsLogger = subjectRef.underlyingActor

    def resume(): Unit = {
      subjectRef ! Resume
    }

    def receiveWork(employee: ActorRef = testRef[Employee],
                    group: Group = testGroup("testGroup"),
                    times: Int = 1): Unit = {
      (0 until times).foreach { _ =>
        subjectRef ! Work(employee, group)
      }
    }

    def rememberLoitering(employee: ActorRef = testRef[Employee],
                          group: Group = testGroup("testGroup"),
                          times: Int = 1): Unit = {
      (0 until times).foreach { _ =>
        subjectRef ! Loiter(employee, group)
      }
    }

    def prevActionsByGroup: MultiMap[Group, WorkArea.Action] = subject.prevActionsByGroup

    def actionsByGroup: MultiMap[Group, WorkArea.Action] = subject.actionsByGroup

    def prevActionsByEmployee: MultiMap[ActorRef, WorkArea.Action] = subject.prevActionsByEmployee

    def actionsByEmployee: MultiMap[ActorRef, WorkArea.Action] = subject.actionsByEmployee

    def sizeOf(mmap: MultiMap[_, _]): Int = mmap.values map (_.size) sum
  }

  "A StatsLogger" when {
    "created" should {
      "ignore all messages" in new StatsLoggerTest {
        receiveWork()
        actionsByGroup shouldBe empty
        actionsByEmployee shouldBe empty
      }
    }

    "resumed" should {
      "not ignore all messages" in new StatsLoggerTest {
        resume()
        receiveWork()
        sizeOf(actionsByGroup) should not be (0)
        sizeOf(actionsByEmployee) should not be (0)
      }
    }
  }

}
