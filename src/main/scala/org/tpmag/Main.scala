package org.tpmag

import scala.collection.immutable
import scala.concurrent.duration.DurationDouble
import scala.language.postfixOps

import org.tpmag.domain.Employee.LoiterBehaviour
import org.tpmag.domain.Employee.WorkBehaviour
import org.tpmag.domain.WorkArea
import org.tpmag.domain.WorkArea.Broadcastability
import org.tpmag.domain.WorkArea.EmployeeCount
import org.tpmag.domain.WorkArea.EmployeeTimerFreq
import org.tpmag.domain.WorkArea.EmployerTimerFreq

import com.softwaremill.tagging.Tagger

import akka.actor.ActorSystem

object Main extends App {
  val system = ActorSystem("tp-mag")

  val behaviours = immutable.Map(
    WorkBehaviour -> 0.75,
    LoiterBehaviour -> 0.25)

  val targetEmployeeCount = 5
  val broadcastability = 5
  val employeeTimerFreq = 0.5 seconds
  val employerTimerFreq = employeeTimerFreq * 5

  val workArea = system.actorOf(WorkArea.props(
    targetEmployeeCount = targetEmployeeCount.taggedWith[EmployeeCount],
    broadcastability = broadcastability.taggedWith[Broadcastability],
    envy = 0.25,
    behaviours = behaviours.toSeq,
    employeeTimerFreq = employeeTimerFreq.taggedWith[EmployeeTimerFreq],
    employerTimerFreq = employerTimerFreq.taggedWith[EmployerTimerFreq]))
}
