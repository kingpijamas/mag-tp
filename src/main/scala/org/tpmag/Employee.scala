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
    var time: Time, // FIXME: get time from elsewhere (or else crashed employees will start at 0L)
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
