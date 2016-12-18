package org.mag.tp.ui

import akka.actor.{ActorRef, Props}
import akka.testkit.TestActorRef
import com.softwaremill.macwire.wire
import com.softwaremill.tagging.Tagger
import org.mag.tp.domain.WorkArea.{Action, Loiter, Work}
import org.mag.tp.domain.employee.{Employee, Group}
import org.mag.tp.ui.FrontendActor.StatsLog
import org.mag.tp.ui.StatsLogger.{FlushLogSummary, GroupActionStats, MultiMap}
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

    def receiveLoitering(employee: ActorRef = testRef[Employee],
                         group: Group = testGroup("testGroup"),
                         times: Int = 1): Unit = {
      (0 until times).foreach { _ =>
        subjectRef ! Loiter(employee, group)
      }
    }

    def flushLogs(): Unit = {
      subjectRef ! FlushLogSummary
    }

    def prevActionsByGroup: MultiMap[Group, Action] = subject.prevState.actionsByGroup

    def prevActionsByEmployee: MultiMap[ActorRef, Action] = subject.prevState.actionsByEmployee

    def actionsByGroup: MultiMap[Group, Action] = subject.state.actionsByGroup

    def actionsByEmployee: MultiMap[ActorRef, Action] = subject.state.actionsByEmployee

    def sizeOf(mmap: MultiMap[_, _]): Int = mmap.values map (_.size) sum
  }

  "A StatsLogger" when {
    val aGroup = testGroup("A")
    val anEmployee = testRef[Employee]

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

      "register the actions it receives" in new StatsLoggerTest {
        resume()

        receiveWork(employee = anEmployee, group = aGroup)

        actionsByGroup.get(aGroup) should not be (None)
        actionsByEmployee.get(anEmployee) should not be (None)
      }
    }

    "flushed" should {
      "not forget its current state" in new StatsLoggerTest {
        resume()

        receiveWork(employee = anEmployee, group = aGroup)

        flushLogs()

        prevActionsByGroup.get(aGroup) should not be (None)
        prevActionsByEmployee.get(anEmployee) should not be (None)
        actionsByGroup.get(aGroup) should be(None)
        actionsByEmployee.get(anEmployee) should be(None)
      }
    }

    "presented with Actions by Employees it doesn't know" should {
      "mark them as 'changed' in its log" in new StatsLoggerTest {
        resume()

        receiveWork(group = aGroup)

        flushLogs()

        frontendActor.expectMsg[StatsLog](
          StatsLog(Map(
            "work" -> Map(aGroup.id -> GroupActionStats(currentCount = 1, changedCount = 0)),
            "loiter" -> Map(aGroup.id -> GroupActionStats(0, 0))
          ))
        )
      }
    }

    "presented with Actions by Employees it does know" should {
      "not mark them as 'changed' in its log if they maintain their previous tendency" in new StatsLoggerTest {
        resume()

        receiveWork(group = aGroup, employee = anEmployee)
        flushLogs()

        frontendActor.expectMsgType[StatsLog]

        receiveWork(group = aGroup, employee = anEmployee)
        flushLogs()


        frontendActor.expectMsg[StatsLog](
          StatsLog(Map(
            "work" -> Map(aGroup.id -> GroupActionStats(currentCount = 1, changedCount = 0)),
            "loiter" -> Map(aGroup.id -> GroupActionStats(0, 0))
          ))
        )
      }

      "mark them as 'changed' in its log if they change their previous tendency" in new StatsLoggerTest {
        resume()

        val actionsReceivedPerLog = 5

        receiveWork(group = aGroup, employee = anEmployee, times = actionsReceivedPerLog)
        flushLogs()

        frontendActor.expectMsgType[StatsLog]

        receiveLoitering(group = aGroup, employee = anEmployee, times = actionsReceivedPerLog)
        flushLogs()

        frontendActor.expectMsg[StatsLog](
          StatsLog(Map(
            "work" -> Map(aGroup.id -> GroupActionStats(0, 0)),
            "loiter" -> Map(aGroup.id -> GroupActionStats(currentCount = 1, changedCount = 1))
          ))
        )
      }
    }
  }

}
