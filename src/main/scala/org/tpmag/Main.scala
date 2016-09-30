package org.tpmag
import scala.concurrent._
import ExecutionContext.Implicits.global

import akka.actor.ActorSystem
import akka.actor.Props
import scala.concurrent.duration._
import org.tpmag.Employee._
import org.tpmag.ProductionWatcher._

object Main extends App {
  val system = ActorSystem("tp-mag")
  val productionWatcher = system.actorOf(Props(classOf[ProductionWatcher], 10, 1, 1))

  val employees = (1 to 100).map { i =>
    system.actorOf(Props(classOf[Employee], productionWatcher, 0L, 0.5), s"employee$i")
  }

  employees.map { employee => system.scheduler.schedule(0.seconds, 1.second, employee, Act) }
  system.scheduler.schedule(0.seconds, 1.second, productionWatcher, FireLazies)
}
