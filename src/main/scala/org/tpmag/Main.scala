package org.tpmag
import scala.concurrent._
import ExecutionContext.Implicits.global

import akka.actor.ActorSystem
import akka.actor.Props
import scala.concurrent.duration._

object Main extends App {
  val system = ActorSystem("tp-mag")
  val employee = system.actorOf(Props(classOf[Employee], 0.5), "employee")
  employee ! Act

  system.scheduler.schedule(0.seconds, 1.second, employee, Act)
}