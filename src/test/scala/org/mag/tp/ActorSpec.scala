package org.mag.tp

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestProbe
import com.softwaremill.tagging.{Tagger, @@}
import org.scalatest.{BeforeAndAfterAll, Suite}

trait ActorSpec extends BeforeAndAfterAll {
  this: Suite =>

  implicit val system = ActorSystem("test-system")

  def testRef[T]: ActorRef @@ T = TestProbe().ref.taggedWith[T]

  def testRefAndProbe[T]: (ActorRef @@ T, TestProbe) = {
    val testProbe = TestProbe()
    (testProbe.ref.taggedWith[T], testProbe)
  }

  override def afterAll(): Unit = {
    try {
      super.afterAll()
    } finally {
      system.terminate()
    }
  }
}
