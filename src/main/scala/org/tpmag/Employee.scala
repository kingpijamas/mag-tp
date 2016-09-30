package org.tpmag

import scala.util.Random

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.actorRef2Scala

object Employee {
  case object Act
  case object Fire
}

class Employee(
    val productionWatcher: ActorRef,
    var time: Time,
    val workPropensity: Double) extends Actor {
  import Employee._
  import ProductionWatcher._

  def receive = {
    case Act => {
      if (Random.nextDouble <= workPropensity) {
        //        println("working")
        productionWatcher ! Produce(time)
      } else {
        //        println("loitering")
      }
      time += 1
    }
    case Fire => println("oh noes :(")
  }
}
