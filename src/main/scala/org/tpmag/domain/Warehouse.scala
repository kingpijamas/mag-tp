package org.tpmag.domain

import scala.collection.mutable
import scala.util.Random

import com.softwaremill.macwire.wire

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import org.tpmag.domain.behaviour.TheftVictimActor

object Warehouse {
  case class TheftLog(time: Time, amount: Int, thiefCaught: Boolean)

  def props(catchingPropensity: Double): Props = Props(wire[Warehouse])
}

class Warehouse(val catchingPropensity: Double) extends TheftVictimActor {
  import Employee.Fire
  import Warehouse._

  val thefts = mutable.Buffer[TheftLog]()

  def onTheftSuccess(time: Time, amount: Int): Unit = {
    logTheft(time, amount, thiefCaught = true)
    sender ! Fire
  }

  def onTheftFailure(time: Time, amount: Int): Unit = {
    logTheft(time, amount, thiefCaught = false)
  }

  private[this] def logTheft(time: Time, amount: Int, thiefCaught: Boolean): Unit = {
    thefts += TheftLog(time, amount, thiefCaught)
  }
}
