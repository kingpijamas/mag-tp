package org.mag.tp.domain

import akka.actor.{Actor, ActorRef, actorRef2Scala}
import com.softwaremill.tagging.@@
import org.mag.tp.util.Scheduled

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

object Employer {
  case object PaySalaries

  trait TimerFreq
}

class Employer(val timerFreq: FiniteDuration @@ Employer.TimerFreq,
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
