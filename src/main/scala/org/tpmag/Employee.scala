package org.tpmag

import scala.util.Random

import akka.actor.Actor

case object Act

class Employee(val workPropensity: Double) extends Actor {
  def receive = {
    case Act => if (Random.nextDouble <= workPropensity) {
      println("working")
    } else {
      println("loitering")
    }
  }
}
