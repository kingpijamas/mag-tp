package org.tpmag.domain

import scala.concurrent.duration.FiniteDuration

import org.tpmag.domain.behaviour.AccusationsReceiver

import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{ @@ => @@ }

import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala

object HumanResources {
  trait AccusationReceptionTime
  trait VeredictVotesReceptionTime

  def props(accusationReceptionTime: FiniteDuration @@ AccusationReceptionTime,
            veredictVotesReceptionTime: FiniteDuration @@ VeredictVotesReceptionTime,
            juryPool: ActorRef @@ EmployeePool): Props =
    Props(wire[HumanResources])
}

class HumanResources(
  val accusationReceptionTime: FiniteDuration @@ HumanResources.AccusationReceptionTime,
  val veredictVotesReceptionTime: FiniteDuration @@ HumanResources.VeredictVotesReceptionTime,
  val jury: ActorRef @@ EmployeePool)
    extends AccusationsReceiver {
  import Employee._

  def onGuiltyVeredict(accused: ActorRef): Unit = {
    // TODO
    println(s"You've been found guilty $accused. Goodbye")
    accused ! Fire
  }

  def onNotGuiltyVeredict(accused: ActorRef): Unit = {
    // TODO
    println(s"You've been found not guilty $accused. Congratulations")
  }
}
