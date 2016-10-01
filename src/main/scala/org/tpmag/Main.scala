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
  val periodLength = 5
  val maxDeviationsAllowed = 1D
  val employeeCount = 100
  val productionWatcher =
    system.actorOf(Props(classOf[ProductionWatcher], periodLength, maxDeviationsAllowed, employeeCount))

  val time = 0L
  val workPropensity = 0.7
  val employees = (0 until employeeCount).map { i =>
    system.actorOf(Props(classOf[Employee], productionWatcher, time, workPropensity), s"employee$i")
  }

  employees.map { employee => system.scheduler.schedule(0.seconds, 1.second, employee, Act) }
  system.scheduler.schedule(0.seconds, 10.seconds, productionWatcher, FireLazies)
}
