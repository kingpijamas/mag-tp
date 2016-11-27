package org.mag.tp

import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, Suite}

trait ActorSpec extends BeforeAndAfterAll {
  this: Suite =>

  implicit val system = ActorSystem("test-system")

  override def afterAll() {
    try {
      super.afterAll()
    }
    finally {
      system.terminate()
    }
  }
}
