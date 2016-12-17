package org.mag.tp.domain

import akka.actor.{ActorRef, ActorSystem, Props}
import com.softwaremill.macwire.wire
import com.softwaremill.tagging.{@@, Tagger}
import org.mag.tp.domain.employee.Employee

import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

trait DomainModule {
  def system: ActorSystem

  def employeeGroups: immutable.Seq[employee.Group]

  def employeeTimerFreq: FiniteDuration @@ Employee

  def visibility: Int

  def employeePropsFactory(workArea: ActorRef @@ WorkArea, group: employee.Group): Props @@ Employee =
    Props(wire[Employee]).taggedWith[Employee]

  def workAreaPropsFactory(mandatoryBroadcastables: Traversable[ActorRef]): Props @@ WorkArea = {
    val employeeProps = employeePropsFactory _
    Props(wire[WorkArea]).taggedWith[WorkArea]
  }
}
