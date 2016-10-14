package org.tpmag

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import breeze.linalg.support.CanTraverseValues
import breeze.linalg.support.CanTraverseValues.ValuesVisitor
import breeze.numerics.sqrt
import breeze.stats.MeanAndVariance
import breeze.stats.meanAndVariance
import breeze.stats.meanAndVariance.reduce_Double

object ProductionSupervisor {
  case class Produce(time: Time)
  case object FireLazies
  case object GetCurrentTime
  case class CurrentTime(time: Time)

  // Needed for statistics
  implicit object IterableAsTraversable extends CanTraverseValues[Iterable[Double], Double] {
    def traverse(from: Iterable[Double], fn: ValuesVisitor[Double]): Unit = from.map(fn.visit)
    def isTraversableAgain(from: Iterable[Double]): Boolean = true
  }
}

class ProductionSupervisor(
    val initialTime: Time,
    val periodLength: Int,
    val maxDeviationsAllowed: Double,
    val employeeCount: Int,
    val timerFreq: FiniteDuration) extends Actor with Scheduled {
  import Employee._
  import ProductionSupervisor._

  def timerMessage = FireLazies

  val producersPerTime = mutable.Map[Time, mutable.Set[ActorRef]]()

  def receive = {
    case Produce(time) => {
      val producersForTime = producersPerTime.getOrElseUpdate(time, mutable.Set())
      producersForTime += sender
    }
    case FireLazies if !producersPerTime.isEmpty => {
      val registeredTimes = producersPerTime.keys.toSeq
      val periodTimes = registeredTimes.sorted.take(periodLength)
      val (from, to) = (periodTimes.min, periodTimes.max)
      val laziesFound = lazies(from, to)
      println(s"\nFound ${laziesFound.size} lazies, will fire them")
      laziesFound.foreach(_ ! Fire)
      producersPerTime --= (from to to)
    }
    case GetCurrentTime => { sender ! CurrentTime(maxTime) }
  }

  private[this] def lazies(from: Time, to: Time) = {
    val relevantProducers = producersBetween(from, to)
    val producePerProducer = relevantProducers.groupBy(identity).mapValues(_.size.toDouble)
    val MeanAndVariance(meanProduce, produceVariance, _) = meanAndVariance(producePerProducer.values)
    val tolerance = maxDeviationsAllowed * sqrt(produceVariance)

    producePerProducer.collect {
      case (producer, produce) if Math.abs(produce - meanProduce) > tolerance => producer
    }
  }

  private[this] def producersBetween(from: Time, to: Time): Iterable[ActorRef] =
    producersPerTime.collect { case (t, producers) if t >= from && t < to => producers }.flatten

  private[this] def maxTime =
    producersPerTime.keys.foldLeft(initialTime)((t, maxT) => if (t > maxT) t else maxT)
}
