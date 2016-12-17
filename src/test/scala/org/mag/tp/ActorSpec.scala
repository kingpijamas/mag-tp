package org.mag.tp

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestProbe
import org.scalatest.{BeforeAndAfterAll, Suite}

trait ActorSpec extends BeforeAndAfterAll {
  this: Suite =>

  implicit val system = ActorSystem("test-system")

  def testRef(): ActorRef = TestProbe().ref

  def testRefAndProbe(): (ActorRef, TestProbe) = {
    val testProbe = TestProbe()
    (testProbe.ref, testProbe)
  }

  override def afterAll(): Unit = {
    try {
      super.afterAll()
    } finally {
      system.terminate()
    }
  }
}
