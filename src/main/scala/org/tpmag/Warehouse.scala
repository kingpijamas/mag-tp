package org.tpmag

import scala.collection.mutable
import scala.util.Random

import com.softwaremill.macwire.wire

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala

object Warehouse {
  case class StealGoods(time: Time, quantity: Int)
  case class Goods(quantity: Int)

  case class TheftLog(time: Time, quantity: Int, thiefCaught: Boolean)

  def props(catchingPropensity: Double): Props = Props(wire[Warehouse])
}

class Warehouse(catchingPropensity: Double) extends Actor {
  import Employee.Fire
  import Warehouse._

  val thefts = mutable.Seq[TheftLog]()

  def receive = {
    case StealGoods(time, quantity) =>
      val thiefCaught = Random.nextDouble < catchingPropensity
      thefts :+ TheftLog(time, quantity, thiefCaught)

      if (thiefCaught) {
        println("Aha! thief!")
        sender ! Fire
      } else {
        sender ! Goods(quantity)
      }
  }
}
