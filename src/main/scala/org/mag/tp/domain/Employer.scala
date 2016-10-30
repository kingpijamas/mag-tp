package org.mag.tp.domain

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

import org.mag.tp.util.Scheduled

import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{ @@ => @@ }

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala

object Employer {
  case object PaySalaries

  def props(timerFreq: FiniteDuration, workArea: ActorRef @@ WorkArea): Props =
    Props(wire[Employer])
}

class Employer(
  val timerFreq: FiniteDuration,
  val workArea: ActorRef @@ WorkArea)
    extends Actor with Scheduled {
  import Employee._
  import Employer._
  import WorkArea._

  def timerMessage: Any = PaySalaries

  var employees = mutable.Set[ActorRef]()

  def receive: Receive = {
    case Work | Loiter =>
      employees += sender

    case PaySalaries =>
      employees.foreach { employee =>
        val paycheck = Paycheck(employee, 1)
        employee ! paycheck
        workArea ! paycheck
      }
  }
}
