package org.tpmag.domain

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import com.softwaremill.macwire.wire
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import breeze.stats._
import breeze.linalg.support.CanTraverseValues
import breeze.linalg.support.CanTraverseValues.ValuesVisitor
import breeze.numerics.sqrt
import breeze.stats.MeanAndVariance
import org.tpmag.util.Scheduled
import com.softwaremill.tagging._
import com.softwaremill.macwire._
import org.tpmag.domain.behaviour.TimerActor

object ProductionSupervisor {
  case class Produce(time: Time)
  case object FireLazies

  // Needed for statistics
  implicit object IterableAsTraversable extends CanTraverseValues[Iterable[Double], Double] {
    def traverse(from: Iterable[Double], fn: ValuesVisitor[Double]): Unit = from.map(fn.visit)
    def isTraversableAgain(from: Iterable[Double]): Boolean = true
  }

  trait PeriodLength
  trait MaxDeviationsAllowed
  trait EmployeeCount

  def props(initialTime: Time,
            periodLength: Int @@ PeriodLength,
            maxDeviationsAllowed: Double @@ MaxDeviationsAllowed,
            employeeCount: Int @@ EmployeeCount,
            timerFreq: FiniteDuration): Props =
    Props(wire[ProductionSupervisor])
}

class ProductionSupervisor(
  initialTime: Time,
  periodLength: Int @@ ProductionSupervisor.PeriodLength,
  maxDeviationsAllowed: Double @@ ProductionSupervisor.MaxDeviationsAllowed,
  employeeCount: Int @@ ProductionSupervisor.EmployeeCount,
  val timerFreq: FiniteDuration)
    extends TimerActor with Scheduled {
  import Employee._
  import ProductionSupervisor._

  def timerMessage = FireLazies

  val producersPerTime = mutable.Map[Time, mutable.Set[ActorRef]]()

  registerReceive {
    case Produce(time) =>
      val producersForTime = producersPerTime.getOrElseUpdate(time, mutable.Set())
      producersForTime += sender

    case FireLazies if !producersPerTime.isEmpty =>
      val registeredTimes = producersPerTime.keys.toSeq
      val periodTimes = registeredTimes.sorted.take(periodLength)
      val (from, to) = (periodTimes.min, periodTimes.max)
      val laziesFound = lazies(from, to)
      println(s"\nFound ${laziesFound.size} lazies, will fire them")
      laziesFound.foreach(_ ! Fire)
      producersPerTime --= (from to to)
  }

  private[this] def lazies(from: Time, to: Time) = {
    def producersBetween(from: Time, to: Time): Iterable[ActorRef] =
      producersPerTime.collect { case (t, producers) if t >= from && t < to => producers }.flatten

    val relevantProducers = producersBetween(from, to)
    val producePerProducer = relevantProducers.groupBy(identity).mapValues(_.size.toDouble)
    val MeanAndVariance(meanProduce, produceVariance, _) = meanAndVariance(producePerProducer.values)
    val tolerance = maxDeviationsAllowed * sqrt(produceVariance)

    producePerProducer.collect {
      case (producer, produce) if Math.abs(produce - meanProduce) > tolerance => producer
    }
  }

  def time: Time = producersPerTime.keys.foldLeft(initialTime)((t, maxT) => if (t > maxT) t else maxT)
}
